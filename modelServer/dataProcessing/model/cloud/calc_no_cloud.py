import os
import shutil
import glob
import time
import json
import requests
import numpy as np
from functools import partial
from concurrent.futures import ThreadPoolExecutor, as_completed
import ray

import rasterio
from rasterio.merge import merge
from rasterio.transform import from_bounds
from rasterio.enums import Resampling
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from shapely.geometry import shape, box

from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.gridUtil import GridHelper
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG

# 如果 rio_tiler 在所有节点环境都已安装，最好放在这。
# 如果只在 Worker 节点有，则保持在函数内。为了保险起见，这里放在顶层，
# 如果报错，移回 process_grid 内部。
try:
    from rio_tiler.io import COGReader
except ImportError:
    pass  # 允许 Driver 节点没有安装 rio_tiler，只要 Worker 有即可

MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
INFINITY = 999999

@ray.remote(num_cpus=CONFIG.RAY_CPUS_PER_TASK, memory=CONFIG.RAY_MEMORY_PER_TASK)
def process_grid(grid, scenes, scene_band_paths, grid_helper, minio_endpoint, temp_dir_path, num_threads=1):
    """
    处理单个格网的任务函数
    优化：scenes 和 scene_band_paths 直接从内存读取，无需 IO
    """
    try:
        # 再次确保导入，防止某些 Worker 环境未预加载
        from rio_tiler.io import COGReader
        
        grid_x, grid_y = grid
        grid_lable = f'grid_{grid_x}_{grid_y}'
        print('-' * 50, flush=True)
        print(f" start { grid_lable }", flush=True)

        # ---------------- 内部 Helper 函数 ----------------
        def grid_bbox():
            bbox_poly = grid_helper._get_grid_polygon(grid_x, grid_y)
            return bbox_poly.bounds

        def is_grid_intersected(scene):
            bbox_wgs84 = grid_bbox()
            scene_geom = scene.get('bbox').get('geometry')
            polygon = shape(scene_geom)
            bbox_polygon = box(*bbox_wgs84)
            return polygon.intersects(bbox_polygon)

        def convert_to_uint8(data, original_dtype):
            """
            自动将不同数据类型转换为uint8
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
        # ------------------------------------------------

        bbox = grid_bbox()
        target_H = None
        target_W = None

        img_1 = None
        img_2 = None
        img_3 = None
        need_fill_mask = None
        first_shape_set = False

        ############### Prepare #########################
        # 按分辨率排序
        sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)

        ############### Core ############################
        for scene in sorted_scene:

            nodata = scene.get('noData')
            scene_label = scene.get('sensorName') + '-' + scene.get('sceneId') + '-' + scene.get('resolution')

            # print('Process', scene_label, flush=True)

            ########### Check cover ######################
            if not is_grid_intersected(scene):
                # print(scene_label, ':: not intersect, jump', flush=True)
                continue

            print(f"Processing {scene_label} for {grid_lable}", flush=True)

            ########### Check cloud ######################
            cloud_band_path = scene.get('cloudPath')

            # --- 公共逻辑：准备读取路径 ---
            scene_id = scene['sceneId']
            paths = scene_band_paths.get(scene_id)
            if not paths:
                continue
            
            # 使用第一个波段来获取 metadata 或 mask
            first_band_path = next(iter(paths.values()))
            full_path_ref = f"{minio_endpoint}/{scene['bucket']}/{first_band_path}"

            if not cloud_band_path:
                # ---------------- 无云掩膜文件的情况 ----------------
                with COGReader(full_path_ref, options={'nodata': int(nodata)}) as reader:
                    
                    if not first_shape_set:
                        temp_shape_obj = reader.part(bbox=bbox, indexes=[1]) 
                        target_H, target_W = temp_shape_obj.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}", flush=True)

                        img_1 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_2 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_3 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
                        first_shape_set = True
                    
                    # 确保对齐
                    current_img_obj = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    current_data = current_img_obj.data[0]
                    nodata_mask = current_img_obj.mask

                valid_mask = (nodata_mask.astype(bool)) & (current_data != 0)

            else:
                # ---------------- 有云掩膜文件的情况 ----------------
                with COGReader(full_path_ref, options={'nodata': int(nodata)}) as ctx:

                    if not first_shape_set:
                        temp_img_data = ctx.part(bbox=bbox, indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}", flush=True)

                        img_1 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_2 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_3 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool) 
                        first_shape_set = True

                    img_data = ctx.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    image_data = img_data.data[0]
                    nodata_mask = img_data.mask 

                sensorName = scene.get('sensorName')

                # 解析云掩膜
                if "Landsat" in sensorName or "Landset" in sensorName:
                    cloud_mask = (image_data & (1 << 3)) > 0
                elif "MODIS" in sensorName:
                    cloud_state = (image_data & 0b11)
                    cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                elif "GF" in sensorName:
                    cloud_mask = (image_data == 2)
                else:
                    print("UNKNOWN SENSOR :" , sensorName, flush=True)
                    continue

                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool)) & (image_data != 0)

            # 需要填充的区域 & 该景有效区域 <--> 该景可以填充格网的区域
            fill_mask = need_fill_mask & valid_mask

            if np.any(fill_mask): 
                
                # 定义读取函数
                def read_band(band_path):
                    full_path = f"{minio_endpoint}/{scene['bucket']}/{band_path}"
                    # 注意：COGReader 在此处实例化是线程安全的
                    with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                        band_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                        original_data = band_data.data[0]
                        original_dtype = original_data.dtype
                        # 转换类型
                        return convert_to_uint8(original_data, original_dtype)

                paths_list = list(paths.values())
                
                # 使用多线程并行读取 R, G, B
                # max_workers=3 对应 RGB 三个波段，网络 IO 会并行执行
                with ThreadPoolExecutor(max_workers=3) as executor:
                    # executor.map 保证返回结果的顺序与输入 paths_list[:3] 的顺序一致
                    results = list(executor.map(read_band, paths_list[:3]))
                
                band_1, band_2, band_3 = results
                

                img_1[fill_mask] = band_1[fill_mask]
                img_2[fill_mask] = band_2[fill_mask]
                img_3[fill_mask] = band_3[fill_mask]

                need_fill_mask[fill_mask] = False # 标记为已填充

                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                # print(f"grid fill progress: {filled_ratio * 100:.2f}%", flush=True)

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                print("fill done (or > 99.5%)", flush=True)
                break

        # 如果 target_H 从未被设置（说明没有景覆盖该格网），直接返回 None
        if target_H is None:
            return None

        # 如果全图都是NoData，直接返回None，不生成文件
        if np.all(need_fill_mask):
            return None

        # 读取RGB并保存tif
        img = np.stack([img_1, img_2, img_3])

        tif_path = os.path.join(temp_dir_path, f"{grid_x}_{grid_y}.tif")
        transform = from_bounds(*bbox, img.shape[2], img.shape[1])
        
        with rasterio.open(
            tif_path, 'w',
            driver='COG',
            height=img.shape[1],
            width=img.shape[2],
            count=3,
            dtype=np.uint8,
            nodata=0,
            crs='EPSG:4326',
            transform=transform,
            BIGTIFF='YES',
            NUM_THREADS=num_threads,
            BLOCKSIZE=256,
            COMPRESS='LZW',
            OVERVIEWS='AUTO',
            OVERVIEW_RESAMPLING='NEAREST'
        ) as dst:
            dst.write(img)

        return tif_path, grid_x, grid_y

    except Exception as e:
        import traceback
        print(f"ERROR in {grid_lable}: {e}", flush=True)
        traceback.print_exc()
        return None

def upload_one(tif_path, grid_x, grid_y, task_id):
    minio_key = f"{task_id}/{grid_x}_{grid_y}.tif"
    uploadLocalFile(tif_path, CONFIG.MINIO_TEMP_FILES_BUCKET, minio_key)
    return {
        "grid": [grid_x, grid_y],
        "bucket": CONFIG.MINIO_TEMP_FILES_BUCKET,
        "tifPath": minio_key
    }

# -------------------- DeprecationWarning --------------------
def merge_tifs(temp_dir_path: str, task_id: str) -> str:
    tif_files = glob.glob(os.path.join(temp_dir_path, "*.tif"))
    if not tif_files:
        raise ValueError("No .tif files found in the directory")

    src_files = [rasterio.open(fp) for fp in tif_files]
    mosaic, out_trans = merge(src_files, mem_limit = 20480, use_highest_res = True)
    
    out_meta = src_files[0].meta.copy()
    out_meta.update({
        "driver": "GTiff",
        "height": mosaic.shape[1],
        "width": mosaic.shape[2],
        "transform": out_trans,
        "count": out_meta.get("count", 1),
        "crs": src_files[0].crs
    })

    temp_merge_path = os.path.join(temp_dir_path, f"{task_id}_temp_merge.tif")
    final_merge_path = os.path.join(temp_dir_path, f"{task_id}_merge_cog.tif")

    with rasterio.open(temp_merge_path, "w", **out_meta) as dest:
        dest.write(mosaic)

    if cog_info(temp_merge_path)["COG"]:
        return temp_merge_path
    else:
        with rasterio.open(temp_merge_path) as src:
            profile = cog_profiles.get("deflate")
            cog_translate(
                src, final_merge_path, profile, 
                in_memory=True, 
                overview_resampling="nearest", 
                resampling="nearest", 
                allow_intermediate_compression=False, 
                temporary_compression="LZW", 
                config={"GDAL_NUM_THREADS": "ALL_CPUS"}
            )
        return final_merge_path


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

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution
        scenes = self.scenes
        bandList = self.bandList

        ## Step 2 : Multithread Processing 4 Grids #############################
        grid_helper = GridHelper(gridResolution)

        scene_band_paths = {} # as cache
        for scene in scenes:
            mapper = scene['bandMapper']
            bands = {band: None for band in bandList}
            for img in scene['images']:
                for band in bandList:
                    if str(img['band']) == str(mapper[band]):
                        bands[band] = img['tifPath']
            scene_band_paths[scene['sceneId']] = bands

        temp_dir_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, self.task_id)
        os.makedirs(temp_dir_path, exist_ok=True)

        print('start time ', time.time(), flush=True)

        # 优化：使用 Ray Object Store 存储大对象，避免写入临时文件和重复IO
        scenes_ref = ray.put(scenes)
        scene_band_paths_ref = ray.put(scene_band_paths)

        # 启动 Ray 任务
        # num_threads 与 Ray 分配的 CPU 数保持一致，避免线程过度订阅
        ray_tasks = [
            process_grid.remote(
                grid=g,
                scenes=scenes_ref,               # 传入引用
                scene_band_paths=scene_band_paths_ref, # 传入引用
                grid_helper=grid_helper,
                minio_endpoint=MINIO_ENDPOINT,
                temp_dir_path=temp_dir_path,
                num_threads=CONFIG.RAY_CPUS_PER_TASK
            )
            for g in grids
        ]
        
        from dataProcessing.model.scheduler import init_scheduler
        scheduler = init_scheduler()
        scheduler.set_task_refs(self.task_id, ray_tasks)
        
        # 获取结果，过滤 None (处理失败或无数据的瓦片)
        results = [r for r in ray.get(ray_tasks) if r is not None]

        ## Step 3 : Results Uploading and Statistic #######################
        upload_results = []
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = [
                executor.submit(upload_one, tif_path, grid_x, grid_y, self.task_id)
                for tif_path, grid_x, grid_y in results
            ]
            for future in as_completed(futures):
                try:
                    upload_results.append(future.result())
                except Exception as e:
                    print(f"Upload failed: {e}", flush=True)

        upload_results.sort(key=lambda x: (x["grid"][0], x["grid"][1]))
        print('end upload ', time.time(), flush=True)

        ## Step 4 : Generate MosaicJSON as result #######################
        # Debug info
        # print([CONFIG.MINIO_TEMP_FILES_BUCKET + "/" + item["tifPath"] for item in upload_results], flush=True)
        
        mosaic_payload = {
            "files": [f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{item['bucket']}/{item['tifPath']}" for item in upload_results],
            "minzoom": 7,
            "maxzoom": 20,
            "max_threads": 20
        }
        
        try:
            response = requests.post(
                CONFIG.MOSAIC_CREATE_URL, 
                json=mosaic_payload, 
                headers={"Content-Type": "application/json"}
            )
            response_json = response.json()
        except Exception as e:
            print(f"Mosaic creation failed: {e}", flush=True)
            response_json = {"error": str(e), "data": upload_results}

        print('=============No Cloud Task Has Finally Finished=================', flush=True)
        
        # 清理临时目录
        if os.path.exists(temp_dir_path):
            try:
                shutil.rmtree(temp_dir_path)
                print('=============No Cloud Origin Data Deleted=================', flush=True)
            except Exception as e:
                print(f"Failed to delete temp dir: {e}", flush=True)

        return response_json