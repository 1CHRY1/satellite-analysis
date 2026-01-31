import os, shutil, glob, time, json
from functools import partial
from multiprocessing import Pool, cpu_count
import rasterio
from rasterio.merge import merge
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from concurrent.futures import ThreadPoolExecutor, as_completed
import ray
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.gridUtil import GridHelper
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG
import requests

MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
INFINITY = 999999

@ray.remote(num_cpus=CONFIG.RAY_NUM_CPUS, memory=CONFIG.RAY_MEMORY_PER_TASK)
def process_grid(grid, scenes_json_file, scene_band_paths_json_file, grid_helper, minio_endpoint, temp_dir_path):
    try:
        from rio_tiler.io import COGReader
        import numpy as np
        import rasterio
        from rasterio.transform import from_bounds
        from rasterio.enums import Resampling
        import json
        import os

        # 从JSON文件中加载数据
        with open(scenes_json_file, 'r', encoding='utf-8') as f:
            scenes = json.load(f)

        with open(scene_band_paths_json_file, 'r', encoding='utf-8') as f:
            scene_band_paths = json.load(f)

        grid_x, grid_y = grid
        grid_lable = f'grid_{grid_x}_{grid_y}'
        INFINITY = 999999
        print('-' * 50, flush=True)
        print(f" start { grid_lable }", flush=True)

        def grid_bbox():
            bbox = grid_helper._get_grid_polygon(grid_x, grid_y)
            return bbox.bounds

        def is_grid_covered(scene):
            from shapely.geometry import shape, box
            bbox_wgs84 = grid_bbox()
            scene_geom = scene.get('bbox').get('geometry')
            polygon = shape(scene_geom)
            bbox_polygon = box(*bbox_wgs84)
            return polygon.contains(bbox_polygon)

        def is_grid_intersected(scene):
            from shapely.geometry import shape, box
            bbox_wgs84 = grid_bbox()
            scene_geom = scene.get('bbox').get('geometry')
            polygon = shape(scene_geom)
            bbox_polygon = box(*bbox_wgs84)
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

        bbox = grid_bbox()
        target_H = None
        target_W = None

        img_1 = None
        img_2 = None
        img_3 = None
        need_fill_mask = None
        first_shape_set = False

        ############### Prepare #########################
        # 按分辨率排序，格网的分辨率是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
        sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)


        ############### Core ############################
        for scene in sorted_scene:

            nodata = scene.get('noData')
            scene_label = scene.get('sensorName') + '-' + scene.get('sceneId') + '-' + scene.get('resolution')

            print('Process', scene_label, flush=True)

            ########### Check cover ######################
            if not is_grid_intersected(scene):
                print(scene_label, ':: not intersect, jump', flush=True)
                continue

            print(scene.get('resolution'), grid_x, grid_y, flush=True)

            ########### Check cloud ######################
            cloud_band_path = scene.get('cloudPath')

            if not cloud_band_path:
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + next(iter(paths.values()))

                with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                    
                    if not first_shape_set:
                        # 这一步只为了拿 H 和 W，数据读完就丢
                        temp_shape_obj = reader.part(bbox=bbox, indexes=[1]) 
                        target_H, target_W = temp_shape_obj.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}", flush=True)

                        img_1 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_2 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_3 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
                        first_shape_set = True
                    
                    # -------------------------------------------------------
                    # 正式读取数据 (缩进对其到 if 外面，保证每景都读！)
                    # -------------------------------------------------------
                    # 必须传入 target_H, target_W 确保对齐
                    current_img_obj = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    
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
                        temp_img_data = ctx.part(bbox=bbox, indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}", flush=True)

                        img_1 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_2 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        img_3 = np.full((target_H, target_W), 0, dtype=np.uint8)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，全都待标记
                        first_shape_set = True

                    # 这里指定宽度高度, 可能发生小的重采样，默认 Nearest
                    img_data = ctx.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
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

                # print(f"这一瓦片有云的像素数：",np.count_nonzero(cloud_mask), flush=True)
                # print(f"这一瓦片的非Nodata像素数: ",np.count_nonzero(nodata_mask.astype(bool)), flush=True)

                # !!! valid_mask <--> 无云 且 非nodata
                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool)) & (image_data != 0)

            # 需要填充的区域 & 该景有效区域 <--> 该景可以填充格网的区域
            fill_mask = need_fill_mask & valid_mask

            # print(f"这一景这一瓦片可填充的像素数：",np.count_nonzero(fill_mask), flush=True)

            if np.any(fill_mask): # 只要有任意一个是1 ，那就可以填充

                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                if not paths:
                    continue

                ################# OLD START ####################
                # def read_band(band_path):
                #     full_path = minio_endpoint + "/" + scene['bucket'] + "/" + band_path
                #     with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                #         return reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W).data[0]
                ################# OLD END ######################

                ################# NEW START ####################
                def read_band(band_path):
                    full_path = minio_endpoint + "/" + scene['bucket'] + "/" + band_path
                    with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                        band_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                        original_data = band_data.data[0]
                        original_dtype = original_data.dtype

                        # 【新增】自动转换为uint8
                        converted_data = convert_to_uint8(original_data, original_dtype)
                        print(f"Band data converted from {original_dtype} to uint8", flush=True)

                        return converted_data
                ################# NEW END ######################
                paths = list(paths.values())
                band_1 = read_band(paths[0])
                band_2 = read_band(paths[1])
                band_3 = read_band(paths[2])

                img_1[fill_mask] = band_1[fill_mask]
                img_2[fill_mask] = band_2[fill_mask]
                img_3[fill_mask] = band_3[fill_mask]

                need_fill_mask[fill_mask] = False # False if pixel filled

                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)

                print(f"grid fill progress: {filled_ratio * 100:.2f}%", flush=True)

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                print("fill done", flush=True)
                break

            print('ENDING ----- ', flush=True)

        first_shape_set = False

        # 读取RGB并保存tif
        img = np.stack([img_1, img_2, img_3])


        tif_path = os.path.join(temp_dir_path, f"{grid_x}_{grid_y}.tif")
        transform = from_bounds(*bbox, img.shape[2], img.shape[1])
        # Important Reference: https://gdal.org/en/stable/drivers/raster/cog.html
        with rasterio.open(
            tif_path, 'w',
            driver='COG',
            height=img.shape[1],
            width=img.shape[2],
            count=3,
            # dtype=img.dtype,
            # Very Important!!!!!!!!!!!!!!!!
            # dtype=np.float32,
            dtype=np.uint8,
            nodata=0,
            crs='EPSG:4326',
            transform=transform,
            BIGTIFF='YES',
            NUM_THREADS="ALL_CPUS",
            # COG 专用选项：强制块大小为 256x256
            BLOCKSIZE=256,
            COMPRESS='LZW',  # 压缩算法
            OVERVIEWS='AUTO',  # 自动生成金字塔
            OVERVIEW_RESAMPLING='NEAREST'  # 金字塔重采样方法
        ) as dst:
            dst.write(img)

        return tif_path, grid_x, grid_y

    except Exception as e:
        print(f"ERROR: {e}", flush=True)
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

    mosaic, out_trans = merge(src_files, mem_limit = 20480, use_highest_res = True) # use_highes_resolution，最终tif统一为最精细分辨率

    out_meta = src_files[0].meta.copy()

    # TODO：直接在这里写入cog
    out_meta.update({
        "driver": "GTiff",
        "height": mosaic.shape[1],
        "width": mosaic.shape[2],
        "transform": out_trans,
        "count": out_meta.get("count", 1),
        "crs": src_files[0].crs
    })

    # 路径设置
    temp_merge_path = os.path.join(temp_dir_path, f"{task_id}_temp_merge.tif")
    final_merge_path = os.path.join(temp_dir_path, f"{task_id}_merge_cog.tif")

    with rasterio.open(temp_merge_path, "w", **out_meta) as dest:
        dest.write(mosaic)

    if cog_info(temp_merge_path)["COG"]:
        return temp_merge_path
    else:
        with rasterio.open(temp_merge_path) as src:
            profile = cog_profiles.get("deflate")
            # Important Reference: https://gdal.org/en/stable/user/configoptions.html
            cog_translate(src, final_merge_path, profile, in_memory=True, overview_resampling="nearest", resampling="nearest", allow_intermediate_compression=False, temporary_compression="LZW", config={"GDAL_NUM_THREADS": "ALL_CPUS"})

        return final_merge_path

# 序列化数据到临时文件
def serialize_data_to_temp_files(scenes, scene_band_paths):
    """
    将scenes和scene_band_paths序列化为JSON文件
    返回文件路径
    """
    import uuid
    import json

    # 生成唯一的文件名
    uuid = str(uuid.uuid4())

    # 生成文件路径
    scenes_json_file = os.path.join(CONFIG.TEMP_OUTPUT_DIR, f"scenes_{uuid}.json")
    scene_band_paths_json_file = os.path.join(CONFIG.TEMP_OUTPUT_DIR, f"scene_band_paths_{uuid}.json")

    # 序列化并写入文件
    with open(scenes_json_file, 'w', encoding='utf-8') as f:
        json.dump(scenes, f, ensure_ascii=False, indent=2)

    with open(scene_band_paths_json_file, 'w', encoding='utf-8') as f:
        json.dump(scene_band_paths, f, ensure_ascii=False, indent=2)

    return scenes_json_file, scene_band_paths_json_file

def cleanup_temp_files(scenes_json_file, scene_band_paths_json_file):
    """
    清理临时JSON文件
    """
    try:
        if os.path.exists(scenes_json_file):
            os.remove(scenes_json_file)
        if os.path.exists(scene_band_paths_json_file):
            os.remove(scene_band_paths_json_file)
    except Exception as e:
        print(f"cleaup error: {e}", flush=True)


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
        # cloud = data.get('cloud') 没啥用

        ## Step 2 : Multithread Processing 4 Grids #############################
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

        temp_dir_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, self.task_id)
        os.makedirs(temp_dir_path, exist_ok=True)

        print('start time ', time.time(), flush=True)

        # 序列化数据到临时文件
        scenes_json_file, scene_band_paths_json_file = serialize_data_to_temp_files(scenes, scene_band_paths)
        # 使用ray
        ray_tasks = [
            process_grid.remote(grid=g,
                                scenes_json_file=scenes_json_file,
                                scene_band_paths_json_file=scene_band_paths_json_file,
                                grid_helper=grid_helper,
                                minio_endpoint=MINIO_ENDPOINT,
                                temp_dir_path=temp_dir_path)
            for g in grids
        ]
        from dataProcessing.model.scheduler import init_scheduler
        scheduler = init_scheduler()
        scheduler.set_task_refs(self.task_id, ray_tasks)
        results = ray.get(ray_tasks)

        # 不使用ray
        # results = []
        # for g in grids:
        #     result = process_grid(g, scenes_json_file, scene_band_paths_json_file, grid_helper, MINIO_ENDPOINT, temp_dir_path)
        #     results.append(result)


        ## Step 3 : Results Uploading and Statistic #######################

        upload_results = []
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = [
                executor.submit(upload_one, tif_path, grid_x, grid_y, self.task_id)
                for result in results if result is not None
                for tif_path, grid_x, grid_y in [result]
            ]
            for future in as_completed(futures):
                upload_results.append(future.result())

        upload_results.sort(key=lambda x: (x["grid"][0], x["grid"][1]))
        print('end upload ', time.time(), flush=True)

        ## Step 4 : Deprecated(Merge TIF) #######################
        # print('start merge ',time.time(), flush=True)
        # result_path = merge_tifs(temp_dir_path, task_id=self.task_id)
        # print('end merge ',time.time(), flush=True)
        # minio_path = f"{self.task_id}/noCloud_merge.tif"
        # uploadLocalFile(result_path, config.MINIO_TEMP_FILES_BUCKET, minio_path)

        # return {
        #     "grids": upload_results,
        #     "statistic": stats
        # }

        ## Step 4 : Generate MosaicJSON as result #######################
        print([CONFIG.MINIO_TEMP_FILES_BUCKET+item["tifPath"] for item in upload_results], flush=True)
        response = requests.post(CONFIG.MOSAIC_CREATE_URL, json={
            "files": [f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{item['bucket']}/{item['tifPath']}" for item in upload_results],
            "minzoom": 7,
            "maxzoom": 20,
            "max_threads": 20
        }, headers={ "Content-Type": "application/json" })

        print('=============No Cloud Task Has Finally Finished=================', flush=True)
        cleanup_temp_files(scenes_json_file, scene_band_paths_json_file)
        if os.path.exists(temp_dir_path):
            shutil.rmtree(temp_dir_path)
            print('=============No Cloud Origin Data Deleted=================', flush=True)
        return response.json()