import os, shutil, glob, time, json
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
        # cloud = data.get('cloud') 没啥用

        ## Step 2 : Multithread Processing 4 Grids #############################
        # !!!!!!!!修改，根据行政区划的面积，划分格网分辨率大致处于10-15km的格网
        # 不一定要按照全球格网划分的规则（也就是不一定要走gridHelper）， 可以自己直接拆分成一个一个grid_bbox
        grid_helper = GridHelper(gridResolution)

        scene_band_paths = {} # as cache
        for scene in scenes:
            mapper = scene['bandMapper']
            bands = {band: None for band in bandList}
            for img in scene['images']:
                for band in bandList:
                    if str(img['band']) == str(mapper[band]):  # 检查当前图像是否匹配目标波段
                        bands[band] = img['tifPath']  # 动态赋值
            scene_band_paths[scene['sceneId']] = bands

        # temp_dir_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, self.task_id)
        # os.makedirs(temp_dir_path, exist_ok=True)
        scenes_ref = ray.put(scenes)
        bandPaths_ref = ray.put(scene_band_paths)
        print('start time ', time.time(), flush=True)

        # TODO 注意这里的CPU限制！
        MAX_NUM_WORKS = 20
        results = []
        in_flight = []
        all_task_refs = []
        for g in grids:
            ref = process_grid.remote(
                grid=g,
                scenes=scenes_ref,
                scene_band_paths=bandPaths_ref,
                grid_helper=grid_helper,
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

        upload_results = [r for r in results if r is not None]# 1. 过滤掉 None (执行失败的任务)
        upload_results.sort(key=lambda x: (x["grid"][0], x["grid"][1]))

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

@ray.remote(num_cpus=1, memory=CONFIG.RAY_MEMORY_PER_TASK)
def process_grid(grid, scenes, scene_band_paths, grid_helper, minio_endpoint, task_id):
    try:
        grid_x, grid_y = grid
        grid_lable = f'grid_{grid_x}_{grid_y}'
        INFINITY = 999999
        # print('-' * 50, flush=True)
        # print(f" start { grid_lable }", flush=True)
        grid_bbox = grid_helper._get_grid_polygon(grid_x, grid_y).bounds
        target_H = None
        target_W = None
        img = None
        need_fill_mask = None
        first_shape_set = False

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
                valid_mask = (nodata_mask.astype(bool)) & (current_data != 0)
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
                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool)) & (image_data != 0)

            # 需要填充的区域 & 该景有效区域 <--> 该景可以填充格网的区域
            fill_mask = need_fill_mask & valid_mask
            if np.any(fill_mask): # 只要有任意一个是1 ，那就可以填充
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                if not paths:
                    continue
                paths = list(paths.values())
                band_1 = read_band(paths[0],scene['bucket'], grid_bbox, target_H, target_W)
                band_2 = read_band(paths[1],scene['bucket'], grid_bbox, target_H, target_W)
                band_3 = read_band(paths[2],scene['bucket'], grid_bbox, target_H, target_W)
                img[0][fill_mask] = band_1[fill_mask]
                img[1][fill_mask] = band_2[fill_mask]
                img[2][fill_mask] = band_3[fill_mask]
                need_fill_mask[fill_mask] = False # False if pixel filled
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                # print(f"grid fill progress: {filled_ratio * 100:.2f}%", flush=True)

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                # print("fill done", flush=True)
                break
        first_shape_set = False

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
            minio_key = f"{task_id}/{grid_x}_{grid_y}.tif"
            #!!!!!!!!!!确保 uploadLocalFile 内部调用的是 boto3 的 upload_fileobj
            # TODO 上传内存文件！

            uploadMemFile(memfile, CONFIG.MINIO_TEMP_FILES_BUCKET, minio_key)
            return {
                "grid": [grid_x, grid_y],
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


def read_band(band_path,scene_bucket, grid_bbox, target_H, target_W):
    full_path = MINIO_ENDPOINT + "/" + scene_bucket + "/" + band_path
    # TODO noData
    with COGReader(full_path, options={'nodata': int(0)}) as reader:
        band_data = reader.part(bbox=grid_bbox, indexes=[1], height=target_H, width=target_W)
        original_data = band_data.data[0]
        original_dtype = original_data.dtype
        # 【新增】自动转换为uint8
        converted_data = convert_to_uint8(original_data, original_dtype)
        return converted_data

# def transform_bbox_3857(grid_bbox):
#     transformer = Transformer.from_crs("EPSG:4326", "EPSG:3857", always_xy=True)
#     minx, miny = transformer.transform(grid_bbox[0], grid_bbox[1])
#     maxx, maxy = transformer.transform(grid_bbox[2], grid_bbox[3])
#     return  (minx, miny, maxx, maxy)


