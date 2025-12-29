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
from datetime import datetime
from typing import List, Dict, Any, Optional
import numpy as np
from datetime import datetime
from typing import List, Dict, Any, Optional
from rasterio.enums import Resampling

###################### 与 计算复杂无云一版图不一样的地方 ##########################
# 0. 核心思想就是外层加了个周期，里面还是景列表的处理
# 1. process_grid，参数变更，scenes_by_period_json_file代表按周期划分过后的景，新增cur_key所以能够追踪到当前周期下的景列表；scene_band_paths不变；新增重采样参数，分为上采样和下采样（sorted_scene）
# 2. 新增SceneAggregator来按周期划分景
# 3. 具体使用ray和不使用ray、上传tif、生成mosaicjson，做了改变，但都是外面加了一层周期套一下。
##############################################################################
MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
INFINITY = 999999

class SceneAggregator:
    def __init__(self, scenes: List[Dict[str, str]]):
        """
        初始化场景数据
        """
        self.scenes = scenes
        # 定义固定的时间解析格式
        self.TIME_FORMAT = '%Y-%m-%d %H:%M:%S'

    def _get_period_key(self, dt: datetime, period: str) -> str:
        """
        根据周期类型生成格式化的时间键
        """
        if period == 'year':
            return dt.strftime('%Y')
        elif period == 'month':
            return dt.strftime('%Y-%m')
        elif period == 'season':
            month = dt.month
            # 定义季度开始月：1, 4, 7, 10
            if month in (1, 2, 3):
                start_month = 1
            elif month in (4, 5, 6):
                start_month = 4
            elif month in (7, 8, 9):
                start_month = 7
            else: # 10, 11, 12
                start_month = 10
                
            return f"{dt.year}-{start_month:02d}"
        else:
            raise ValueError(f"不支持的周期类型: {period}")

    def _generate_continuous_keys(self, start_dt: datetime, end_dt: datetime, period: str) -> List[str]:
        """
        生成从开始时间到结束时间，按指定周期连续的键列表
        （此方法保持不变，因为它只依赖于 datetime 对象）
        """
        keys = set()
        current_dt = start_dt
        
        while current_dt <= end_dt:
            key = self._get_period_key(current_dt, period)
            keys.add(key)
            
            # 递增到下一个周期
            if period == 'year':
                current_dt = current_dt.replace(year=current_dt.year + 1)
            elif period == 'month':
                year = current_dt.year
                month = current_dt.month + 1
                if month > 12:
                    month = 1
                    year += 1
                # 统一设置为下个月 1 号，避免日期错误
                current_dt = current_dt.replace(year=year, month=month, day=1) 
            elif period == 'season':
                # 季度增加 3 个月
                month = current_dt.month + 3
                year = current_dt.year
                if month > 12:
                    month -= 12
                    year += 1
                current_dt = current_dt.replace(year=year, month=month, day=1)
            else:
                break 

        # ... (排序和格式化逻辑保持不变)
        dt_keys = []
        for key in keys:
            if period == 'year':
                dt_keys.append(datetime.strptime(key, '%Y'))
            else:
                dt_keys.append(datetime.strptime(key, '%Y-%m'))
        
        dt_keys.sort()
        
        if period == 'year':
            return [dt.strftime('%Y') for dt in dt_keys]
        else:
            return [dt.strftime('%Y-%m') for dt in dt_keys]


    def aggregate_scenes(self, period: str) -> Dict[str, List[Dict[str, str]]]:
        """
        按指定周期聚合场景数据，并补全连续时间序列。
        """
        if not self.scenes:
            return {}

        # 1. 解析所有时间，并找到范围
        parsed_times = []
        for scene in self.scenes:
            try:
                # ====== 关键修改点：使用新的 TIME_FORMAT ======
                dt = datetime.strptime(scene['sceneTime'], self.TIME_FORMAT)
                parsed_times.append(dt)
            except ValueError:
                print(f"警告: 无法使用 '{self.TIME_FORMAT}' 解析时间字符串 {scene['sceneTime']}")
                continue
            
        if not parsed_times:
            return {}

        min_dt = min(parsed_times)
        max_dt = max(parsed_times)

        # 2. 生成连续且完整的周期键
        continuous_keys = self._generate_continuous_keys(min_dt, max_dt, period)

        # 3. 初始化结果字典，并填充空列表
        result_dict: Dict[str, List[Dict[str, str]]] = {key: [] for key in continuous_keys}

        # 4. 聚合数据
        for scene in self.scenes:
            try:
                # ====== 关键修改点：使用新的 TIME_FORMAT ======
                dt = datetime.strptime(scene['sceneTime'], self.TIME_FORMAT)
            except ValueError:
                continue

            key = self._get_period_key(dt, period)
            
            if key in result_dict:
                result_dict[key].append(scene)

        return result_dict


def merge_time_series_cubes(results_by_period, grids, time_keys, temp_dir, band_num, resample='upscale'):
    """
    输入：
        ...
        resample: 'upscale' (对齐到最大分辨率/最大尺寸) 或 'downscale' (对齐到最小分辨率/最小尺寸)
    """
    print(f"Start merging cubes with strategy: {resample}...", flush=True)
    
    # 1. 数据重组
    grid_map = {}
    for g in grids:
        grid_map[tuple(g)] = {k: None for k in time_keys} 

    for t_key, res_list in results_by_period.items():
        if not res_list: continue
        for item in res_list:
            if item is None: continue
            tif_path, gx, gy = item
            if (gx, gy) in grid_map:
                grid_map[(gx, gy)][t_key] = tif_path

    merged_results = []
    cube_dir = os.path.join(temp_dir, "cubes")
    os.makedirs(cube_dir, exist_ok=True)

    # 2. 遍历每个格网
    for (gx, gy), time_files in grid_map.items():
        
        # ====================【修改点1：第一遍扫描，确定目标尺寸】====================
        valid_metas = [] # 存储 (width, height, transform, path)
        
        for t_key, t_path in time_files.items():
            if t_path and os.path.exists(t_path):
                with rasterio.open(t_path) as src:
                    valid_metas.append({
                        "width": src.width,
                        "height": src.height,
                        "transform": src.transform,
                        "path": t_path
                    })
        
        if not valid_metas:
            print(f"Grid {gx}_{gy} has no data, skipping.", flush=True)
            continue

        # 根据 upscale/downscale 决定基准尺寸
        # upscale: 选像素数最多的 (分辨率最高的)
        # downscale: 选像素数最少的 (分辨率最低的)
        if resample == 'downscale':
            # 按 (width * height) 升序排，取最小的
            target_meta = sorted(valid_metas, key=lambda x: x["width"] * x["height"])[0]
        else:
            # 默认 upscale，按降序排，取最大的
            target_meta = sorted(valid_metas, key=lambda x: x["width"] * x["height"], reverse=True)[0]
            
        target_w = target_meta["width"]
        target_h = target_meta["height"]
        target_transform = target_meta["transform"]
        
        # ===========================================================================

        total_bands = band_num * len(time_keys)
        stacked_data = []
        
        # ====================【修改点2：第二遍读取，强制重采样对齐】====================
        for t_key in time_keys:
            current_tif = time_files[t_key]
            
            if current_tif and os.path.exists(current_tif):
                with rasterio.open(current_tif) as src:
                    # 判断当前文件尺寸是否与目标尺寸一致
                    if src.width != target_w or src.height != target_h:
                        # 尺寸不一致，进行重采样读取
                        # 使用 nearest 保证值不会出现类似 1.5 这种小数，尤其是分类/掩膜数据
                        # 如果是连续影像(RGB)，也可以考虑 Resampling.bilinear
                        data = src.read(
                            out_shape=(src.count, target_h, target_w),
                            resampling=Resampling.nearest 
                        )
                    else:
                        data = src.read()

                    # 波段数对齐检查
                    if data.shape[0] < band_num:
                        pad = np.zeros((band_num - data.shape[0], target_h, target_w), dtype=np.uint8)
                        data = np.concatenate((data, pad), axis=0)
                    
                    stacked_data.append(data[:band_num])
            else:
                # 无数据，直接生成目标尺寸的空数组
                empty_chunk = np.zeros((band_num, target_h, target_w), dtype=np.uint8)
                stacked_data.append(empty_chunk)
        # ===========================================================================

        final_array = np.concatenate(stacked_data, axis=0)
        out_path = os.path.join(cube_dir, f"cube_{gx}_{gy}.tif")
        
        try:
            with rasterio.open(
                out_path, 'w',
                driver='COG',
                height=target_h,
                width=target_w,
                count=total_bands,
                dtype=np.uint8,
                crs='EPSG:4326',
                transform=target_transform, # 使用选定基准的 transform
                BIGTIFF='YES',
                NUM_THREADS="ALL_CPUS",
                BLOCKSIZE=256,
                COMPRESS='LZW',
                OVERVIEWS='AUTO',
                OVERVIEW_RESAMPLING='NEAREST'
            ) as dst:
                dst.write(final_array)
            merged_results.append((out_path, gx, gy))
            
        except Exception as e:
            print(f"Error writing cube {gx}_{gy}: {e}", flush=True)

    print(f"Merged {len(merged_results)} grids.", flush=True)
    return merged_results

@ray.remote(num_cpus=CONFIG.RAY_NUM_CPUS, memory=CONFIG.RAY_MEMORY_PER_TASK)
def process_grid(grid, scenes_by_period_json_file, scene_band_paths_json_file, grid_helper, minio_endpoint, temp_dir_path, band_num, cur_key, resample):
    try:
        from rio_tiler.io import COGReader
        import numpy as np
        import rasterio
        from rasterio.transform import from_bounds
        from rasterio.enums import Resampling
        import json
        import os

        # 从JSON文件中加载数据
        with open(scenes_by_period_json_file, 'r', encoding='utf-8') as f:
            scenesByPeriod = json.load(f)
            scenes = scenesByPeriod[cur_key]

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

        img_list = [None] * band_num
        need_fill_mask = None
        first_shape_set = False

        ############### Prepare #########################
        # 按分辨率排序，格网的分辨率是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
        # 根据 resample 参数决定排序顺序和重采样算法
        # reverse=False (默认): 小数字在前 (0.5m, 10m) -> 优先使用高分辨率定型 -> Upscale
        # reverse=True: 大数字在前 (10m, 0.5m) -> 优先使用低分辨率定型 -> Downscale
        is_reverse_sort = True if resample == 'downscale' else False
        sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=is_reverse_sort)

        ############### Core ############################
        for scene in sorted_scene:

            nodata = scene.get('noData')
            scene_label = scene.get('sensorName') + '-' + scene.get('sceneId') + '-' + scene.get('resolution')

            print('Process', scene_label, flush=True)

            ########### Check cover ######################
            if not is_grid_intersected(scene):
                print(scene_label, ':: not intersect, jump', flush=True)
                continue

            print(scene.get('resolution'), grid_x, grid_y)

            ########### Check cloud ######################
            cloud_band_path = scene.get('cloudPath')

            if not cloud_band_path:
                # reading Red --> 确定格网 target_H, target_W
                if not first_shape_set:

                    scene_id = scene['sceneId']
                    paths = scene_band_paths.get(scene_id)
                    full_path = minio_endpoint + "/" + scene['bucket'] + "/" + next(iter(paths.values())) # paths字典中第一个键值
                    with COGReader(full_path, options={'nodata': int(nodata)}) as reader:

                        temp_img_data = reader.part(bbox=bbox, indexes=[1]) # 不设置width/height的话不会重采样，分辨率与原始tif一致
                        nodata_mask = temp_img_data.mask

                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}", flush=True)

                        img_list = [np.full((target_H, target_W), 0, dtype=np.uint8) for _ in range(band_num)]
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，待填充
                        first_shape_set = True


                # 默认全部无云，只考虑nodata
                valid_mask = (nodata_mask.astype(bool))

            else:
                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + next(iter(paths.values()))
                with COGReader(full_path, options={'nodata': int(nodata)}) as ctx:

                    if not first_shape_set:
                        temp_img_data = ctx.part(bbox=bbox, indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}", flush=True)

                        img_list = [np.full((target_H, target_W), 0, dtype=np.uint8) for _ in range(band_num)]

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
                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool))

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
                def calculate_ndvi(nir_band, red_band):
                    """计算 NDVI 指数"""
                    # 避免除以零（将分母为零的位置设为 NaN 或 0）
                    denominator = nir_band.astype(float) + red_band.astype(float)
                    ndvi = np.divide(
                        nir_band.astype(float) - red_band.astype(float),
                        denominator,
                        where=(denominator != 0)  # 仅在分母非零时计算
                    )
                    # 将 NaN 或无效值替换为 0（或其他默认值）
                    ndvi = np.nan_to_num(ndvi, nan=0, posinf=0, neginf=0)
                    # 转换为 uint8（范围 0-255）或保持 float32（范围 -1 到 1）
                    # 如果需要存储为图像，可以缩放到 0-255：
                    # ndvi_uint8 = ((ndvi + 1) * 127.5).astype(np.uint8)
                    return ndvi

                def calculate_evi(nir_band, red_band, blue_band=None, C1=6.0, C2=7.5, L=1.0, G=2.5):
                    """计算 EVI（增强型植被指数）"""
                    # 确保输入是浮点型，避免整数运算溢出
                    nir = nir_band.astype(float) * 0.0000275 - 0.2  # 假设是辐射值
                    red = red_band.astype(float) * 0.0000275 - 0.2
                    # 如果没有蓝波段，可以省略 C2 * Blue 项（但精度会下降）
                    if blue_band is not None:
                        blue = blue_band.astype(float) * 0.0000275 - 0.2
                        denominator = nir + C1 * red - C2 * blue + L
                    else:
                        print("Warning: Blue band not provided, EVI calculation may be less accurate.", flush=True)
                        denominator = nir + C1 * red + L  # 简化公式（无蓝波段）

                    # 避免除以零
                    evi = np.divide(
                        G * (nir - red),
                        denominator,
                        where=(denominator != 0)  # 仅在分母非零时计算
                    )

                    # 处理无效值（NaN 或无穷大）
                    evi = np.nan_to_num(evi, nan=0, posinf=0, neginf=0)
                    return evi
                band_list = list()
                for band, band_path in paths.items():
                    if band == "NDVI":
                        NIR = read_band(band_path['NIR'])
                        Red = read_band(band_path['Red'])
                        NDVI = calculate_ndvi(NIR, Red)
                        band_list.append(NDVI)
                    elif band == "EVI":
                        NIR = read_band(band_path['NIR'])
                        Red = read_band(band_path['Red'])
                        # 检查是否有蓝波段（可选）
                        Blue = read_band(band_path['Blue']) if 'Blue' in band_path else None
                        # 计算 EVI
                        EVI = calculate_evi(NIR, Red, Blue)
                        band_list.append(EVI)
                    else:
                        band_list.append(read_band(band_path))

                for i in range(band_num):
                    img_list[i][fill_mask] = band_list[i][fill_mask]

                need_fill_mask[fill_mask] = False # False if pixel filled

                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)

                print(f"grid fill progress: {filled_ratio * 100:.2f}%", flush=True)

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                print("fill done", flush=True)
                break

            print('ENDING ----- ', flush=True)

        first_shape_set = False

        # 读取RGB并保存tif
        img = np.stack(img_list)

        tif_path = os.path.join(temp_dir_path, f"{cur_key}_{grid_x}_{grid_y}.tif")
        transform = from_bounds(*bbox, img.shape[2], img.shape[1])
        with rasterio.open(
            tif_path, 'w',
            driver='COG',
            height=img.shape[1],
            width=img.shape[2],
            count=band_num,
            # dtype=img.dtype,
            # Very Important!!!!!!!!!!!!!!!!
            # dtype=np.float32,
            dtype=np.uint8,
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

def upload_one(tif_path, cur_key, grid_x, grid_y, task_id):
    minio_key = f"{task_id}/{cur_key}_{grid_x}_{grid_y}.tif"
    uploadLocalFile(tif_path, CONFIG.MINIO_TEMP_FILES_BUCKET, minio_key)
    return {
        "grid": [grid_x, grid_y],
        "bucket": CONFIG.MINIO_TEMP_FILES_BUCKET,
        "tifPath": minio_key
    }

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
        print(f"cleanup error: {e}", flush=True)


class calc_eo_cube(Task):

    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])
        self.bandList = self.args[0].get('bandList', ['Red', 'Green', 'Blue'])
        self.period = self.args[0].get('period', 'month')
        self.resample = self.args[0].get('resample', 'upscale')


    def run(self):
        print("EOCubeTask run", flush=True)

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution
        scenes = self.scenes
        bandList = self.bandList
        band_num = len(bandList)
        # cloud = data.get('cloud') 没啥用

        ## Step 1.2 : Get Scenes By Period #################################################
        aggregator = SceneAggregator(scenes=scenes)
        scenesByPeriod = aggregator.aggregate_scenes(self.period)
        time_keys = sorted(list(scenesByPeriod.keys()))

        ## Step 2 : Multithread Processing 4 Grids #############################
        grid_helper = GridHelper(gridResolution)

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
                    bands[index] = config

            # 处理普通波段
            for band in bandList:
                if band != 'NDVI' and band != 'EVI':
                    target_band = mapper.get(band)
                    if target_band is None:
                        raise ValueError(f"Band '{band}' not found in mapper")
                    bands[band] = find_band_path(scene['images'], target_band)

            if 'NDVI' in bands:
                bands['NDVI']['NIR'] = find_band_path(scene['images'], mapper.get('NIR'))
                bands['NDVI']['Red'] = find_band_path(scene['images'], mapper.get('Red'))

            # 处理 EVI（更灵活的版本）
            if 'EVI' in bands:
                bands['EVI']['Blue'] = find_band_path(scene['images'], mapper.get('Blue'))
                bands['EVI']['Red'] = find_band_path(scene['images'], mapper.get('Red'))
                bands['EVI']['NIR'] = find_band_path(scene['images'], mapper.get('NIR'))

            # 检查完整性（同上）
            scene_band_paths[scene['sceneId']] = bands

        temp_dir_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, self.task_id)
        os.makedirs(temp_dir_path, exist_ok=True)

        print('start time ', time.time(), flush=True)

        # 序列化数据到临时文件
        scenes_by_period_json_file, scene_band_paths_json_file = serialize_data_to_temp_files(scenesByPeriod, scene_band_paths)
        
        ## Step 3 : Process #############################
        # 使用ray
        from dataProcessing.model.scheduler import init_scheduler
        all_ray_tasks = [] # 用于 scheduler 的扁平列表
        # 用于后续还原结果结构的字典，存的是 ObjectRef
        futures_by_period = {key: [] for key in scenesByPeriod.keys()} 
        for cur_key, cur_item in scenesByPeriod.items():
            if len(cur_item) == 0:
                continue
            for g in grids:
                task_ref = process_grid.remote(
                    grid=g,
                    scenes_by_period_json_file=scenes_by_period_json_file,
                    scene_band_paths_json_file=scene_band_paths_json_file,
                    grid_helper=grid_helper,
                    minio_endpoint=MINIO_ENDPOINT,
                    temp_dir_path=temp_dir_path,
                    band_num=band_num,
                    cur_key=cur_key,
                    resample=self.resample
                )
                # A. 存入总列表 (给调度器用)
                all_ray_tasks.append(task_ref)
                # B. 存入分类字典 (给后续结果还原用)
                futures_by_period[cur_key].append(task_ref)
        scheduler = init_scheduler()
        scheduler.set_task_refs(self.task_id, all_ray_tasks)
        resultsByPeriod = {key: [] for key in scenesByPeriod.keys()}
        for key, futures in futures_by_period.items():
            if futures:
                resultsByPeriod[key] = ray.get(futures)
            else:
                resultsByPeriod[key] = []

        # 不使用ray
        # results按周期组织。一个周期，对应一个mosaicJSON，通常只有一个格网
        # resultsByPeriod = { key: [] for key in scenesByPeriod.keys() }
        # for cur_key, cur_item in scenesByPeriod.items():
        #     if len(cur_item) == 0:
        #         continue
        #     for g in grids:
        #         result = process_grid(g, scenes_by_period_json_file, scene_band_paths_json_file, grid_helper, MINIO_ENDPOINT, temp_dir_path, band_num, cur_key = cur_key, resample=self.resample)
        #         resultsByPeriod[cur_key].append(result)

        ## Step 4 : Merge Different Period to one cube #############################
        merged_results = merge_time_series_cubes(
            resultsByPeriod, self.tiles, time_keys, temp_dir_path, band_num, resample=self.resample
        )

        upload_results = []
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = [executor.submit(upload_one, r[0], "", r[1], r[2], self.task_id) for r in merged_results]
            for f in as_completed(futures):
                upload_results.append(f.result())
        
        upload_results.sort(key=lambda x: (x["grid"][0], x["grid"][1]))

        # 生成唯一的 MosaicJSON
        final_mosaic_data = {}
        if upload_results:
            urls = [f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{i['bucket']}/{i['tifPath']}" for i in upload_results]
            response = requests.post(CONFIG.MOSAIC_CREATE_URL, json={
                "files": urls, "minzoom": 7, "maxzoom": 20, "max_threads": 20
            }, headers={"Content-Type": "application/json"})
            final_mosaic_data = response.json()

        # 清理并返回
        cleanup_temp_files(scenes_by_period_json_file, scene_band_paths_json_file)
        if os.path.exists(temp_dir_path):
            shutil.rmtree(temp_dir_path)

        # 【修改】直接合并 Json 属性返回
        return {
            'dimensions': time_keys,
            **final_mosaic_data  # 直接展开 MosaicJSON 内容
        }

        # ## Step 3 : Results Uploading and Statistic #######################
        # upload_results_by_period = {key: [] for key in scenesByPeriod.keys()}
        # with ThreadPoolExecutor(max_workers=8) as executor:
        #     future_to_period = {} 
        #     for period_key, res_list in resultsByPeriod.items():
        #         for result in res_list:
        #             if result is None:
        #                 continue
        #             tif_path, grid_x, grid_y = result 
        #             future = executor.submit(upload_one, tif_path, period_key, grid_x, grid_y, self.task_id)
        #             future_to_period[future] = period_key 
        #     for future in as_completed(future_to_period):
        #         period = future_to_period[future]
        #         try:
        #             res = future.result()
        #             upload_results_by_period[period].append(res)
        #         except Exception as exc:
        #             print(f'Upload generated an exception: {exc}')
        # for period_key in upload_results_by_period:
        #     upload_results_by_period[period_key].sort(key=lambda x: (x["grid"][0], x["grid"][1]))
        # print('end upload ', time.time(), flush=True)

        # ## Step 4 : Generate MosaicJSON as result #######################
        # final_responses = {}
        # for period_key, upload_results in upload_results_by_period.items():
        #     if not upload_results:
        #         continue

        #     print(f"Generating MosaicJSON for period: {period_key}", flush=True)
        #     print([CONFIG.MINIO_TEMP_FILES_BUCKET + '/' + item["tifPath"] for item in upload_results], flush=True)
            
        #     # 针对当前周期(period_key)的数据发起请求
        #     response = requests.post(CONFIG.MOSAIC_CREATE_URL, json={
        #         "files": [f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{item['bucket']}/{item['tifPath']}" for item in upload_results],
        #         "minzoom": 7,
        #         "maxzoom": 20,
        #         "max_threads": 20
        #     }, headers={ "Content-Type": "application/json" })
            
        #     # 打印请求的 URL、状态码和响应内容
        #     print(f"[{period_key}] Request URL:", CONFIG.MOSAIC_CREATE_URL, flush=True)
        #     print(f"[{period_key}] Status Code:", response.status_code, flush=True)
        #     final_responses[period_key] = response.json()


        # print('=============EOCube Task Has Finally Finished=================', flush=True)
        # cleanup_temp_files(scenes_by_period_json_file, scene_band_paths_json_file)
        # if os.path.exists(temp_dir_path):
        #     shutil.rmtree(temp_dir_path)
        #     print('=============EOCube Origin Data Deleted=================', flush=True)
        # return {
        #     'dimensions': list(final_responses.keys()), 
        #     'data': final_responses
        # }