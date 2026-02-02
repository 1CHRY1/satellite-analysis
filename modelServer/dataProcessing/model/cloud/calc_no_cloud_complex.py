import os, shutil, glob, time, json
import math
from functools import partial
from multiprocessing import Pool, cpu_count
import rasterio
from rio_tiler.io import COGReader
import numpy as np
from rasterio.transform import from_bounds
from rasterio.warp import transform_bounds
from rasterio.enums import Resampling
from shapely.geometry import shape, box
from rasterio.merge import merge
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from concurrent.futures import ThreadPoolExecutor, as_completed
import ray
from dataProcessing.Utils.osUtils import uploadLocalFile, uploadMemFile
from dataProcessing.Utils.gridUtil import GridHelper
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG
import requests
from rasterio.shutil import delete as rio_delete
from rasterio.io import MemoryFile

MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
INFINITY = 999999


class calc_no_cloud_complex(Task):

    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])
        self.bandList = self.args[0].get('bandList', ['Red', 'Green', 'Blue'])

    def run(self):
        print("NoCloudGraphTask Complex run", flush=True)
        print('start time ', time.time())

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution  # 2km / 50km
        scenes = self.scenes
        bandList = self.bandList
        band_num = len(bandList)

        ## Step 2 : 自适应格网划分 ##############################################
        grid_helper = GridHelper(gridResolution)

        # 对每个原始格网进行细分（如果需要的话）
        all_grid_bboxes = []  # [(grid_bbox, grid_label), ...]
        for g in grids:
            grid_x, grid_y = g
            original_bbox = grid_helper._get_grid_polygon(grid_x, grid_y).bounds
            sub_grids = subdivide_grid_if_needed(original_bbox, gridResolution)
            for sub_bbox, sub_label in sub_grids:
                # grid_label 格式: "原始格网x_原始格网y_子格网i_子格网j"
                grid_label = f"{grid_x}_{grid_y}_{sub_label}"
                all_grid_bboxes.append((sub_bbox, grid_label))

        print(f"原始格网数: {len(grids)}, 细分后格网数: {len(all_grid_bboxes)}", flush=True)
        if gridResolution > 20:
            subdivide_factor = math.ceil(gridResolution / 15)
            actual_res = gridResolution / subdivide_factor
            print(f"格网细分: {subdivide_factor}x{subdivide_factor}, 子格网分辨率: {actual_res:.1f}km", flush=True)

        ## Step 3 : 构建波段路径缓存 ###########################################
        scene_band_paths = {}  # 作为缓存，存储每个 scene 的波段路径

        def find_band_path(images, target_band):
            """辅助函数：在影像列表中查找目标波段的路径"""
            for img in images:
                if str(img['band']) == str(target_band):
                    return img['tifPath']
            return None

        for scene in scenes:
            mapper = scene['bandMapper']
            bands = {band: None for band in bandList}
            # 定义一个字典，键是索引名称，值是对应的波段配置
            band_configs = {
                'NDVI': {'NIR': None, 'Red': None},
                'EVI': {'Blue': None, 'Red': None, 'NIR': None}
            }
            # 遍历字典，只有当索引在bandList中时才添加配置
            for index, config in band_configs.items():
                if index in bandList:
                    bands[index] = config.copy()

            # 处理普通波段
            for band in bandList:
                if band != 'NDVI' and band != 'EVI':
                    target_band = mapper.get(band)
                    if target_band is None:
                        raise ValueError(f"Band '{band}' not found in mapper")
                    bands[band] = find_band_path(scene['images'], target_band)

            if 'NDVI' in bands and bands['NDVI'] is not None:
                bands['NDVI']['NIR'] = find_band_path(scene['images'], mapper.get('NIR'))
                bands['NDVI']['Red'] = find_band_path(scene['images'], mapper.get('Red'))

            # 处理 EVI（更灵活的版本）
            if 'EVI' in bands and bands['EVI'] is not None:
                bands['EVI']['Blue'] = find_band_path(scene['images'], mapper.get('Blue'))
                bands['EVI']['Red'] = find_band_path(scene['images'], mapper.get('Red'))
                bands['EVI']['NIR'] = find_band_path(scene['images'], mapper.get('NIR'))

            scene_band_paths[scene['sceneId']] = bands

        # 使用 ray.put() 共享数据，避免序列化到文件
        scenes_ref = ray.put(scenes)
        bandPaths_ref = ray.put(scene_band_paths)
        bandList_ref = ray.put(bandList)

        print('start processing time ', time.time(), flush=True)

        ## Step 4 : 滑动窗口并发控制 ###########################################
        MAX_NUM_WORKS = 20
        results = []
        in_flight = []
        all_task_refs = []

        for grid_bbox, grid_label in all_grid_bboxes:
            ref = process_grid.remote(
                grid_bbox=grid_bbox,
                grid_label=grid_label,
                scenes=scenes_ref,
                scene_band_paths=bandPaths_ref,
                band_list=bandList_ref,
                band_num=band_num,
                minio_endpoint=MINIO_ENDPOINT,
                task_id=self.task_id
            )
            all_task_refs.append(ref)
            in_flight.append(ref)
            if len(in_flight) >= MAX_NUM_WORKS:
                done, in_flight = ray.wait(in_flight, num_returns=1)
                results.extend(ray.get(done))

        # 收尾
        if in_flight:
            results.extend(ray.get(in_flight))

        # 设置ref，用于取消任务
        from dataProcessing.model.scheduler import init_scheduler
        scheduler = init_scheduler()
        scheduler.set_task_refs(self.task_id, all_task_refs)

        upload_results = [r for r in results if r is not None]  # 过滤掉 None (执行失败的任务)
        upload_results.sort(key=lambda x: x["gridLabel"])  # 按 grid_label 排序

        ## Step 5 : Generate MosaicJSON as result #######################
        print([CONFIG.MINIO_TEMP_FILES_BUCKET + '/' + item["tifPath"] for item in upload_results], flush=True)
        response = requests.post(CONFIG.MOSAIC_CREATE_URL, json={
            "files": [f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{item['bucket']}/{item['tifPath']}" for item in upload_results],
            "minzoom": 7,
            "maxzoom": 20,
            "max_threads": 20
        }, headers={"Content-Type": "application/json"})

        # 打印请求的 URL、状态码和响应内容
        print("Request URL:", CONFIG.MOSAIC_CREATE_URL, flush=True)
        print("Status Code:", response.status_code, flush=True)
        print("Response Headers:", response.headers, flush=True)
        print("Response Text:", response.text, flush=True)
        print('=============No Cloud Complex Task Has Finally Finished=================', flush=True)
        print('end time ', time.time())
        return response.json()


@ray.remote(num_cpus=1, memory=CONFIG.RAY_MEMORY_PER_TASK)
def process_grid(grid_bbox, grid_label, scenes, scene_band_paths, band_list, band_num, minio_endpoint, task_id):
    """
    处理单个格网的去云合成（支持复杂波段如 NDVI、EVI）

    Args:
        grid_bbox: (minLng, minLat, maxLng, maxLat) 格网边界
        grid_label: 格网标签，用于文件命名，格式如 "x_y_i_j"
        scenes: 场景列表
        scene_band_paths: 场景波段路径缓存
        band_list: 波段列表
        band_num: 波段数量
        minio_endpoint: MinIO 地址
        task_id: 任务 ID
    """
    try:
        target_H = None
        target_W = None
        img = None
        need_fill_mask = None
        first_shape_set = False
        filled_ratio = 0.0

        print('-' * 50, flush=True)
        print(f" start {grid_label}", flush=True)

        ############### Prepare #########################
        # 按分辨率排序，格网的分辨率是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
        sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)

        ############### Core ############################
        for scene in sorted_scene:
            nodata = scene.get('noData')
            scene_label = scene.get('sensorName') + '-' + scene.get('sceneId') + '-' + scene.get('resolution')

            ########### Check cover ######################
            if not is_grid_intersected(scene, grid_bbox=grid_bbox):
                print(scene_label, ':: not intersect, jump', flush=True)
                continue

            print('Process', scene_label, flush=True)

            ########### Check cloud ######################
            cloud_band_path = scene.get('cloudPath')

            if not cloud_band_path:
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                # 获取第一个非 None 且非 dict 的路径
                first_path = get_first_valid_path(paths)
                if not first_path:
                    continue
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + first_path

                with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                    if not first_shape_set:
                        temp_shape_obj = reader.part(bbox=grid_bbox, dst_crs="EPSG:3857", indexes=[1])
                        target_H, target_W = temp_shape_obj.data[0].shape
                        img = np.zeros((band_num, target_H, target_W), dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
                        first_shape_set = True

                    current_img_obj = reader.part(bbox=grid_bbox, dst_crs="EPSG:3857", indexes=[1], height=target_H, width=target_W)
                    current_data = current_img_obj.data[0]
                    nodata_mask = current_img_obj.mask

                # 默认全部无云，只考虑nodata
                valid_mask = (nodata_mask.astype(bool)) & (current_data != 0)

            else:
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                first_path = get_first_valid_path(paths)
                if not first_path:
                    continue
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + first_path

                with COGReader(full_path, options={'nodata': int(nodata)}) as ctx:
                    if not first_shape_set:
                        temp_img_data = ctx.part(bbox=grid_bbox, dst_crs="EPSG:3857", indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        img = np.zeros((band_num, target_H, target_W), dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
                        first_shape_set = True

                    img_data = ctx.part(bbox=grid_bbox, dst_crs="EPSG:3857", indexes=[1], height=target_H, width=target_W)
                    image_data = img_data.data[0]
                    nodata_mask = img_data.mask

                sensorName = scene.get('sensorName')
                if "Landsat" in sensorName or "Landset" in sensorName:
                    cloud_mask = (image_data & (1 << 3)) > 0
                elif "MODIS" in sensorName:
                    cloud_state = (image_data & 0b11)
                    cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                elif "GF" in sensorName:
                    cloud_mask = (image_data == 2)
                else:
                    print("UNKNOWN :", sensorName, flush=True)
                    continue

                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool)) & (image_data != 0)

            # 需要填充的区域 & 该景有效区域
            fill_mask = need_fill_mask & valid_mask

            if np.any(fill_mask):
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                if not paths:
                    continue

                # 读取各波段数据
                band_data_list = []
                for band in band_list:
                    band_path = paths.get(band)
                    if band == "NDVI" and isinstance(band_path, dict):
                        nir_path = band_path.get('NIR')
                        red_path = band_path.get('Red')
                        if nir_path and red_path:
                            nir = read_band(nir_path, scene['bucket'], grid_bbox, target_H, target_W)
                            red = read_band(red_path, scene['bucket'], grid_bbox, target_H, target_W)
                            ndvi = calculate_ndvi(nir, red)
                            band_data_list.append(ndvi)
                        else:
                            print(f"WARNING: Missing NIR or Red path for NDVI", flush=True)
                            band_data_list.append(np.zeros((target_H, target_W), dtype=np.uint8))
                    elif band == "EVI" and isinstance(band_path, dict):
                        nir_path = band_path.get('NIR')
                        red_path = band_path.get('Red')
                        blue_path = band_path.get('Blue')
                        if nir_path and red_path:
                            nir = read_band(nir_path, scene['bucket'], grid_bbox, target_H, target_W)
                            red = read_band(red_path, scene['bucket'], grid_bbox, target_H, target_W)
                            blue = read_band(blue_path, scene['bucket'], grid_bbox, target_H, target_W) if blue_path else None
                            evi = calculate_evi(nir, red, blue)
                            band_data_list.append(evi)
                        else:
                            print(f"WARNING: Missing NIR or Red path for EVI", flush=True)
                            band_data_list.append(np.zeros((target_H, target_W), dtype=np.uint8))
                    elif band_path is not None and isinstance(band_path, str):
                        # 普通波段：确保 band_path 是字符串
                        band_data = read_band(band_path, scene['bucket'], grid_bbox, target_H, target_W)
                        band_data_list.append(band_data)
                    else:
                        print(f"WARNING: Invalid band_path for {band}: {type(band_path)}", flush=True)
                        # 填充零数据
                        band_data_list.append(np.zeros((target_H, target_W), dtype=np.uint8))

                # 填充数据
                for i in range(band_num):
                    img[i][fill_mask] = band_data_list[i][fill_mask]

                need_fill_mask[fill_mask] = False
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                break

        first_shape_set = False

        # 转换坐标系到 3857
        grid_bbox_3857 = transform_bounds("EPSG:4326", "EPSG:3857", *grid_bbox)
        transform = from_bounds(*grid_bbox_3857, img.shape[2], img.shape[1])

        # 使用 MemoryFile 实现 生成 -> 上传 一条龙
        with MemoryFile() as memfile:
            with memfile.open(
                driver='COG',
                height=img.shape[1],
                width=img.shape[2],
                count=band_num,
                dtype=np.uint8,
                nodata=0,
                crs='EPSG:3857',
                transform=transform,
                BIGTIFF='YES',
                NUM_THREADS="1",
                BLOCKSIZE=512,
                COMPRESS='ZSTD',
                OVERVIEWS='AUTO',
                OVERVIEW_RESAMPLING='NEAREST'
            ) as dst:
                dst.write(img)

            memfile.seek(0)
            minio_key = f"{task_id}/{grid_label}.tif"
            uploadMemFile(memfile, CONFIG.MINIO_TEMP_FILES_BUCKET, minio_key)

            return {
                "gridLabel": grid_label,
                "bucket": CONFIG.MINIO_TEMP_FILES_BUCKET,
                "tifPath": minio_key
            }

    except Exception as e:
        print(f"ERROR: {e}", flush=True)
        return None


def get_first_valid_path(paths):
    """获取第一个有效的路径（非 None 且非 dict）"""
    if not paths:
        return None
    for key, value in paths.items():
        if value is not None and not isinstance(value, dict):
            return value
    # 如果所有都是 dict（如 NDVI、EVI），则尝试获取嵌套路径
    for key, value in paths.items():
        if isinstance(value, dict):
            for sub_key, sub_value in value.items():
                if sub_value is not None:
                    return sub_value
    return None


def is_grid_intersected(scene, grid_bbox):
    scene_geom = scene.get('bbox').get('geometry')
    polygon = shape(scene_geom)
    bbox_polygon = box(*grid_bbox)
    return polygon.intersects(bbox_polygon)


def convert_to_uint8(data, original_dtype):
    """
    自动将不同数据类型转换为uint8
    - Byte (uint8): 直接返回
    - UInt16: 映射到0-255范围
    - Float32:
        * 如果数据范围在0-255内，直接转uint8
        * 如果数据范围在0-65535内，直接转uint16再映射到uint8
        * 否则归一化映射到0-255
    """
    if original_dtype == np.uint8:
        return data.astype(np.uint8)
    elif original_dtype == np.uint16:
        return (data / 65535.0 * 255.0).astype(np.uint8)
    elif original_dtype == np.float32 or original_dtype == float:
        data_min = np.min(data)
        data_max = np.max(data)
        if data_min >= 0 and data_max <= 255:
            return data.astype(np.uint8)
        elif data_min >= 0 and data_max <= 65535:
            temp_uint16 = data.astype(np.uint16)
            return (temp_uint16 / 65535.0 * 255.0).astype(np.uint8)
        else:
            if data_max > data_min:
                normalized = (data - data_min) / (data_max - data_min)
                return (normalized * 255.0).astype(np.uint8)
            else:
                return np.zeros_like(data, dtype=np.uint8)
    else:
        data_min = np.min(data)
        data_max = np.max(data)
        if data_max > data_min:
            normalized = (data - data_min) / (data_max - data_min)
            return (normalized * 255.0).astype(np.uint8)
        else:
            return np.zeros_like(data, dtype=np.uint8)


def read_band(band_path, scene_bucket, grid_bbox, target_H, target_W):
    """读取单个波段数据"""
    if band_path is None:
        print(f"WARNING: band_path is None, returning zeros", flush=True)
        return np.zeros((target_H, target_W), dtype=np.uint8)

    full_path = MINIO_ENDPOINT + "/" + scene_bucket + "/" + band_path
    with COGReader(full_path, options={'nodata': int(0)}) as reader:
        band_data = reader.part(bbox=grid_bbox, dst_crs="EPSG:3857", indexes=[1], height=target_H, width=target_W)
        original_data = band_data.data[0]
        original_dtype = original_data.dtype
        converted_data = convert_to_uint8(original_data, original_dtype)
        return converted_data


def calculate_ndvi(nir_band, red_band):
    """计算 NDVI 指数，返回 uint8 格式 (0-255)"""
    denominator = nir_band.astype(float) + red_band.astype(float)
    ndvi = np.divide(
        nir_band.astype(float) - red_band.astype(float),
        denominator,
        where=(denominator != 0)
    )
    ndvi = np.nan_to_num(ndvi, nan=0, posinf=0, neginf=0)
    # 将 -1~1 映射到 0~255
    ndvi_uint8 = ((ndvi + 1) * 127.5).astype(np.uint8)
    return ndvi_uint8


def calculate_evi(nir_band, red_band, blue_band=None, C1=6.0, C2=7.5, L=1.0, G=2.5):
    """计算 EVI（增强型植被指数），返回 uint8 格式"""
    nir = nir_band.astype(float) * 0.0000275 - 0.2
    red = red_band.astype(float) * 0.0000275 - 0.2

    if blue_band is not None:
        blue = blue_band.astype(float) * 0.0000275 - 0.2
        denominator = nir + C1 * red - C2 * blue + L
    else:
        denominator = nir + C1 * red + L

    evi = np.divide(
        G * (nir - red),
        denominator,
        where=(denominator != 0)
    )
    evi = np.nan_to_num(evi, nan=0, posinf=0, neginf=0)
    # EVI 范围通常在 -1~1，映射到 0~255
    evi_uint8 = np.clip((evi + 1) * 127.5, 0, 255).astype(np.uint8)
    return evi_uint8


def subdivide_grid_if_needed(grid_bbox, resolution_km, target_max_km=15):
    """
    如果格网分辨率超过 20km，则进行细分，使子格网分辨率在 10-15km 之间

    Args:
        grid_bbox: (minLng, minLat, maxLng, maxLat)
        resolution_km: 当前格网分辨率 (km)
        target_max_km: 目标最大分辨率 (km)，默认 15km

    Returns:
        子格网列表: [(bbox, label), ...]
        - bbox: (minLng, minLat, maxLng, maxLat)
        - label: 用于文件命名的标签
    """
    min_lng, min_lat, max_lng, max_lat = grid_bbox

    if resolution_km <= 20:
        return [(grid_bbox, "0_0")]

    subdivide_factor = math.ceil(resolution_km / target_max_km)

    grid_width = (max_lng - min_lng) / subdivide_factor
    grid_height = (max_lat - min_lat) / subdivide_factor

    sub_grids = []
    for i in range(subdivide_factor):
        for j in range(subdivide_factor):
            sub_bbox = (
                min_lng + i * grid_width,
                min_lat + j * grid_height,
                min_lng + (i + 1) * grid_width,
                min_lat + (j + 1) * grid_height
            )
            sub_label = f"{i}_{j}"
            sub_grids.append((sub_bbox, sub_label))

    return sub_grids
