import os, shutil, glob, time, json
import math
from functools import partial
from multiprocessing import Pool, cpu_count
import rasterio
import gc
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
# from pyproj import Transformer
from rasterio.shutil import delete as rio_delete
from rasterio.io import MemoryFile

MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
INFINITY = 999999

class calc_no_cloud(Task):

    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])
        self.bandList = self.args[0].get('bandList', ['Red', 'Green', 'Blue'])

    def run(self):
        print("NoCloudGraphTask run", flush=True)
        print('start time ', time.time())

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution # 2km / 50km
        scenes = self.scenes
        bandList = self.bandList

        ## Step 2 : 自适应格网划分 ##############################################
        # 根据分辨率自动细分格网，使子格网分辨率在 10-15km 之间（保证 ≤20km）
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

        scene_band_paths = {} # as cache
        for scene in scenes:
            mapper = scene['bandMapper']
            bands = {band: None for band in bandList}
            for img in scene['images']:
                for band in bandList:
                    if str(img['band']) == str(mapper[band]):  # 检查当前图像是否匹配目标波段
                        bands[band] = img['tifPath']  # 动态赋值
            scene_band_paths[scene['sceneId']] = bands

        scenes_ref = ray.put(scenes)
        bandPaths_ref = ray.put(scene_band_paths)
        print('start time ', time.time(), flush=True)

        # TODO 注意这里的CPU限制！
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
                minio_endpoint=MINIO_ENDPOINT,
                task_id=self.task_id
            )
            all_task_refs.append(ref)
            in_flight.append(ref)
            if len(in_flight) >=  MAX_NUM_WORKS:
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

        ## Step 4 : Generate MosaicJSON as result #######################
        print([CONFIG.MINIO_TEMP_FILES_BUCKET+item["tifPath"] for item in upload_results], flush=True)
        response = requests.post(CONFIG.MOSAIC_CREATE_URL, json={
            "files": [f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{item['bucket']}/{item['tifPath']}" for item in upload_results],
            "minzoom": 7,
            "maxzoom": 20,
            "max_threads": 20
        }, headers={ "Content-Type": "application/json" })

        print('=============No Cloud Task Has Finally Finished=================', flush=True)
        # cleanup_temp_files(scenes_json_file, scene_band_paths_json_file)
        # if os.path.exists(temp_dir_path):
        #     shutil.rmtree(temp_dir_path)
        #     print('=============No Cloud Origin Data Deleted=================', flush=True)
        print('end time ', time.time())
        return response.json()

@ray.remote(num_cpus=1, memory=CONFIG.RAY_MEMORY_PER_TASK, scheduling_strategy="SPREAD")
def process_grid(grid_bbox, grid_label, scenes, scene_band_paths, minio_endpoint, task_id):
    """
    处理单个格网的去云合成

    Args:
        grid_bbox: (minLng, minLat, maxLng, maxLat) 格网边界
        grid_label: 格网标签，用于文件命名，格式如 "x_y_i_j"
        scenes: 场景列表
        scene_band_paths: 场景波段路径缓存
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

        ############### Prepare #########################
        # 按分辨率排序，格网的分辨率是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
        sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)

        ############### Core ############################
        for scene in sorted_scene:
            nodata = scene.get('noData')
            scene_label = scene.get('sensorName') + '-' + scene.get('sceneId') + '-' + scene.get('resolution')
            # print('Process', scene_label, flush=True)
            ########### Check cover ######################
            if not is_grid_intersected(scene, grid_bbox=grid_bbox):
                continue
            # print(scene.get('resolution'), grid_x, grid_y, flush=True)
            ########### Check cloud ######################
            cloud_band_path = scene.get('cloudPath')
            if not cloud_band_path:
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + next(iter(paths.values()))
                with COGReader(full_path, options={'nodata': int(nodata)}) as reader:                    
                    if not first_shape_set:
                        # 这一步只为了拿 H 和 W，数据读完就丢
                        temp_shape_obj = reader.part(bbox=grid_bbox, dst_crs="EPSG:3857", indexes=[1]) 
                        target_H, target_W = temp_shape_obj.data[0].shape
                        img = np.zeros((3, target_H, target_W), dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)                        
                        first_shape_set = True                    
                    # -------------------------------------------------------
                    # 正式读取数据 (缩进对其到 if 外面，保证每景都读！)
                    # -------------------------------------------------------
                    # 必须传入 target_H, target_W 确保对齐
                    current_img_obj = reader.part(bbox=grid_bbox,dst_crs="EPSG:3857", indexes=[1], height=target_H, width=target_W)                    
                    # 拿到当前景的数据
                    current_data = current_img_obj.data[0]
                    nodata_mask = current_img_obj.mask
                # 默认全部无云，只考虑nodata；(current_data != 0)是因为不信任原有的 mask
                # valid_mask = (nodata_mask.astype(bool)) & (current_data != 0)
                valid_mask = nodata_mask.astype(np.bool_, copy=False)
                np.not_equal(current_data, 0, out=valid_mask, where=valid_mask)
            else:
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + next(iter(paths.values()))
                with COGReader(full_path, options={'nodata': int(nodata)}) as ctx:
                    if not first_shape_set:
                        temp_img_data = ctx.part(bbox=grid_bbox,dst_crs="EPSG:3857", indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        img = np.zeros((3, target_H, target_W), dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)                        
                        first_shape_set = True
                    # 这里指定宽度高度, 可能发生小的重采样，默认 Nearest
                    img_data = ctx.part(bbox=grid_bbox, dst_crs="EPSG:3857",indexes=[1], height=target_H, width=target_W)
                    image_data = img_data.data[0]
                    nodata_mask = img_data.mask # true --> valid，false --> nodata

                sensorName = scene.get('sensorName')
                if "Landsat" in sensorName or "Landset" in sensorName:
                    cloud_mask = (image_data & (1 << 3)) > 0
                elif "MODIS" in sensorName:
                    cloud_state = (image_data & 0b11)
                    cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                elif "GF" in sensorName:
                    cloud_mask = (image_data == 2)
                else:
                    print("UNKNOWN :" , sensorName, flush=True)
                    continue

                # !!! valid_mask <--> 无云 且 非nodata
                # valid_mask = (~cloud_mask) & (nodata_mask.astype(bool)) & (image_data != 0)
                valid_mask = nodata_mask.astype(np.bool_, copy=False)
                valid_mask &= ~cloud_mask
                np.not_equal(image_data, 0, out=valid_mask, where=valid_mask)

            # 需要填充的区域 & 该景有效区域 <--> 该景可以填充格网的区域
            # fill_mask = need_fill_mask & valid_mask
            fill_mask = valid_mask
            fill_mask &= need_fill_mask

            if np.any(fill_mask): # 只要有任意一个是1 ，那就可以填充
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                if not paths:
                    continue
                paths = list(paths.values())
                # band_1 = read_band(paths[0],scene['bucket'], grid_bbox, target_H, target_W)
                # band_2 = read_band(paths[1],scene['bucket'], grid_bbox, target_H, target_W)
                # band_3 = read_band(paths[2],scene['bucket'], grid_bbox, target_H, target_W)
                # img[0][fill_mask] = band_1[fill_mask]
                # img[1][fill_mask] = band_2[fill_mask]
                # img[2][fill_mask] = band_3[fill_mask]
                # need_fill_mask[fill_mask] = False # False if pixel filled
                # filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                # print(f"grid fill progress: {filled_ratio * 100:.2f}%", flush=True)

                band_temp = read_band(paths[0], scene['bucket'], grid_bbox, target_H, target_W)
                np.copyto(img[0], band_temp, where=fill_mask)
                del band_temp
                band_temp = read_band(paths[1], scene['bucket'], grid_bbox, target_H, target_W)
                np.copyto(img[1], band_temp, where=fill_mask)
                del band_temp
                band_temp = read_band(paths[2], scene['bucket'], grid_bbox, target_H, target_W)
                np.copyto(img[2], band_temp, where=fill_mask)
                del band_temp
                np.putmask(need_fill_mask, fill_mask, False)
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                # print("fill done", flush=True)
                break
        first_shape_set = False

        vars_to_delete = [
            'current_img_obj', 'current_data',
            'img_data', 'image_data',
            'nodata_mask', 'valid_mask', 'fill_mask',
            'band_1', 'band_2', 'band_3'
        ]
        locs = locals()
        for var in vars_to_delete:
            if var in locs:
                del locs[var]
        gc.collect()

        # 需要转成3857
        grid_bbox_3857 = transform_bounds("EPSG:4326", "EPSG:3857", *grid_bbox)
        transform = from_bounds(*grid_bbox_3857, img.shape[2], img.shape[1])
        # 【最佳实践】使用 MemoryFile 实现 生成 -> 上传 一条龙
        with MemoryFile() as memfile:
            # 1. 写入数据到内存
            with memfile.open(
                driver='COG',
                height=img.shape[1],
                width=img.shape[2],
                count=3,
                # dtype=img.dtype,
                # Very Important!!!!!!!!!!!!!!!!
                # dtype=np.float32,
                dtype=np.uint8,
                nodata=0,
                crs='EPSG:3857',
                transform=transform,
                BIGTIFF='YES',
                NUM_THREADS="1",            
                BLOCKSIZE=512,
                COMPRESS='ZSTD',  # 压缩算法
                OVERVIEWS='AUTO',  
                OVERVIEW_RESAMPLING='NEAREST'  # 金字塔重采样方法
            ) as dst:
                dst.write(img)
            # 2. 指针归零
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
        # 将 uint16 (0-65535) 线性映射到 uint8 (0-255)
        return (data / 65535.0 * 255.0).astype(np.uint8)
    elif original_dtype == np.float32 or original_dtype == float:
        data_min = np.min(data)
        data_max = np.max(data)
        if data_min >= 0 and data_max <= 255:
            # 数据在uint8范围内，直接转uint8
            return data.astype(np.uint8)
        elif data_min >= 0 and data_max <= 65535:
            # 数据在uint16范围内，先转uint16，再映射到uint8
            temp_uint16 = data.astype(np.uint16)
            return (temp_uint16 / 65535.0 * 255.0).astype(np.uint8)
        else:
            # 归一化映射到0-255
            if data_max > data_min:
                normalized = (data - data_min) / (data_max - data_min)
                return (normalized * 255.0).astype(np.uint8)
            else:
                return np.zeros_like(data, dtype=np.uint8)
    else:
        # 其他类型，先归一化到0-1，再映射to 0-255
        data_min = np.min(data)
        data_max = np.max(data)
        if data_max > data_min:
            normalized = (data - data_min) / (data_max - data_min)
            return (normalized * 255.0).astype(np.uint8)
        else:
            return np.zeros_like(data, dtype=np.uint8)


# def read_band(band_path,scene_bucket, grid_bbox, target_H, target_W):
#     full_path = MINIO_ENDPOINT + "/" + scene_bucket + "/" + band_path
#     # TODO noData
#     with COGReader(full_path, options={'nodata': int(0)}) as reader:
#         band_data = reader.part(bbox=grid_bbox, indexes=[1], height=target_H, width=target_W)
#         original_data = band_data.data[0]
#         original_dtype = original_data.dtype
#         # 【新增】自动转换为uint8
#         converted_data = convert_to_uint8(original_data, original_dtype)
#         return converted_data

def read_band(band_path, scene_bucket, grid_bbox, target_H, target_W):
    # full_path = CONFIG.MINIO_ENDPOINT + "/" + scene_bucket + "/" + band_path
    full_path = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{scene_bucket}/{band_path}"
    
    with COGReader(full_path, options={'nodata': 0}) as reader:
        # 1. 读取数据
        data_part = reader.part(bbox=grid_bbox, indexes=[1], height=target_H, width=target_W)
        arr = data_part.data[0] # 原始数据
        
        # 2. 获取元数据
        dtype = arr.dtype
        
        # === Case 1: 已经是 Uint8 ===
        if dtype == np.uint8:
            return arr # 零拷贝直接返回
            
        # === Case 2: Uint16 (标准缩放) ===
        if dtype == np.uint16:
            # 转换为 float32 节省内存 (相比 float64)
            f_arr = arr.astype(np.float32)
            # 原地乘法 (In-place) 避免临时数组
            # 255 / 65535 = 0.00389099
            np.multiply(f_arr, 0.00389099, out=f_arr)
            return f_arr.astype(np.uint8)
            
        # === Case 3: 浮点数 (Float32/64) ===
        if np.issubdtype(dtype, np.floating):
            # 先计算 min/max，不占内存
            d_min = np.min(arr)
            d_max = np.max(arr)
            
            # [逻辑优化]：虽然是 float，但如果范围已经在 0-255 之间，直接强转
            # 这保留了你第二种代码的智慧
            if d_min >= 0 and d_max <= 255:
                return arr.astype(np.uint8)
            
            # [逻辑优化]：如果是 0-65535 的 float，先模拟 uint16 的缩放
            if d_min >= 0 and d_max <= 65535:
                # 这里的 arr 已经是 float 了，直接原地乘
                np.multiply(arr, 0.00389099, out=arr)
                return arr.astype(np.uint8)
                
            # [标准归一化]：其他范围，拉伸到 0-255
            if d_max > d_min:
                # 使用 In-place 操作防止 OOM
                # arr = (arr - min) / (max - min) * 255
                scale = 255.0 / (d_max - d_min)
                np.subtract(arr, d_min, out=arr) # arr 变成 arr - min
                np.multiply(arr, scale, out=arr) # arr 变成 arr * scale
                return arr.astype(np.uint8)
            
            return np.zeros_like(arr, dtype=np.uint8)

        # 其他类型兜底
        return arr.astype(np.uint8)
    

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
        # 不需要细分
        return [(grid_bbox, "0_0")]

    # 计算细分倍数，使子格网分辨率 ≤ target_max_km
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

# def transform_bbox_3857(grid_bbox):
#     transformer = Transformer.from_crs("EPSG:4326", "EPSG:3857", always_xy=True)
#     minx, miny = transformer.transform(grid_bbox[0], grid_bbox[1])
#     maxx, maxy = transformer.transform(grid_bbox[2], grid_bbox[3])
#     return  (minx, miny, maxx, maxy)


