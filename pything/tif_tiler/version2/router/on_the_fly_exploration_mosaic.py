from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import requests
import math
from config import minio_config, TRANSPARENT_CONTENT
import time
import json
from typing import Dict, List, Tuple, Optional
import hashlib

####### Helper ########################################################################################
MINIO_ENDPOINT = "http://" + minio_config['endpoint']

def normalize(arr, min_val = 0 , max_val = 5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")

def convert_to_uint8(data, original_dtype):
    """
    自动将不同数据类型转换为uint8
    """
    if original_dtype == np.uint8:
        return data.astype(np.uint8)
    elif original_dtype == np.uint16:
        return (data / 65535.0 * 255.0).astype(np.uint8)
    else:
        return np.uint8(np.floor(data.clip(0, 255)))

def calc_tile_bounds(x, y, z):
    """ Calculate the bounds of a tile in degrees """
    Z2 = math.pow(2, z)

    ul_lon_deg = x / Z2 * 360.0 - 180.0
    ul_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y / Z2)))
    ul_lat_deg = math.degrees(ul_lat_rad)

    lr_lon_deg = (x + 1) / Z2 * 360.0 - 180.0
    lr_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / Z2)))
    lr_lat_deg = math.degrees(lr_lat_rad)

    result = {
        "west": ul_lon_deg,
        "east": lr_lon_deg,
        "south": lr_lat_deg,
        "north": ul_lat_deg,
        "bbox": [ul_lon_deg, lr_lat_deg, lr_lon_deg, ul_lat_deg]
    }
    
    return result

####### 简化的MosaicJSON处理 ########################################################################################

def create_simple_mosaic(scenes_config: List[Dict]) -> Dict[str, List[str]]:
    """
    创建简化的MosaicJSON结构
    由于后端已经返回了与当前瓦片相关的场景，我们只需要排序即可
    """
    # 排序场景：先按是否有云排序（无云优先），再按覆盖率排序
    sorted_scenes = sorted(
        scenes_config,
        key=lambda x: (
            1 if x.get('cloudPath') else 0,  # 无云为0，有云为1
            -x.get('coverage', 0)  # 覆盖率高优先
        )
    )
    
    # 返回排序后的场景ID列表
    return {
        'scene_ids': [scene['sceneId'] for scene in sorted_scenes],
        'scene_map': {scene['sceneId']: scene for scene in scenes_config}
    }

# 缓存
cache = {}

def get_cache_key(sensor_name: str, start_time: str, end_time: str, x: int, y: int, z: int) -> str:
    """生成缓存键"""
    key_str = f"{sensor_name}_{start_time}_{end_time}_{z}_{x}_{y}"
    return hashlib.md5(key_str.encode()).hexdigest()

####### Router ########################################################################################
router = APIRouter()

@router.get("/{z}/{x}/{y}")
async def get_tile(
    z: int, x: int, y: int,
    sensorName: str = Query(...),
    startTime: str = Query(...),
    endTime: str = Query(...),
):
    start_time = time.time()

    try:
        t1 = time.time()

        # 计算tile的边界
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']
        print(f"[⏱] 计算 tile 边界耗时: {time.time() - t1:.3f} 秒")

        # 生成缓存键
        cache_key = get_cache_key(sensorName, startTime, endTime, x, y, z)
        
        # 检查缓存
        if cache_key in cache:
            print(f"[✓] 使用缓存数据")
            mosaic_data = cache[cache_key]['mosaic']
            mapper = cache[cache_key]['mapper']
            scene_ids = mosaic_data['scene_ids']
            scene_map = mosaic_data['scene_map']
        else:
            # spring boot 接口
            t2 = time.time()
            url = "http://192.168.1.127:8999/api/v1/modeling/example/noCloud/createNoCloudConfig"
            data = {
                "sensorName": sensorName,
                "startTime": startTime,
                "endTime": endTime,
                "points": points
            }
            json_response = requests.post(url, json=data).json()
            print(f"[⏱] SpringBoot 请求耗时: {time.time() - t2:.3f} 秒")

            t3 = time.time()
            # 获取数据
            mapper = json_response.get('data', {}).get('bandMapper', {})
            json_data = json_response.get('data', {}).get('scenesConfig', [])
            
            # 创建简化的mosaic数据
            mosaic_data = create_simple_mosaic(json_data)
            
            # 缓存结果
            cache[cache_key] = {
                'mosaic': mosaic_data,
                'mapper': mapper,
                'timestamp': time.time()
            }
            print(f"[⏱] 处理场景数据耗时: {time.time() - t3:.3f} 秒")
            
            scene_ids = mosaic_data['scene_ids']
            scene_map = mosaic_data['scene_map']

        if not scene_ids:
            print(f"[!] 瓦片 {z}/{x}/{y} 没有场景数据")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        print(f"[✓] 瓦片 {z}/{x}/{y} 有 {len(scene_ids)} 个候选场景")
        
        # 获取相关场景
        relevant_scenes = [scene_map[sid] for sid in scene_ids]
        
        t4 = time.time()
        # 处理bandMapper
        scene_band_paths = {}
        bandList = ['Red', 'Green', 'Blue']

        for scene in relevant_scenes:
            bands = {band: None for band in bandList}
            paths = scene.get('path', {})  
            
            if sensorName == 'ZY1_AHSI':
                # ZY1_AHSI 特殊处理
                default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
                for band in bandList:
                    band_key = f"band_{default_mapping.get(band, '1')}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        available_bands = sorted([k for k in paths.keys() if k.startswith('band_')])
                        if available_bands:
                            bands[band] = paths[available_bands[0]]
            else:
                # 正常处理
                for band in bandList:
                    band_key = f"band_{mapper[band]}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        # 备选波段处理
                        if band == 'Red' and 'band_1' in paths:
                            bands[band] = paths['band_1']
                        elif band == 'Green' and 'band_2' in paths:
                            bands[band] = paths['band_2']
                        elif band == 'Blue' and 'band_3' in paths:
                            bands[band] = paths['band_3']
            
            scene_band_paths[scene['sceneId']] = bands

        print(f"[⏱] 处理 bandMapper 耗时: {time.time() - t4:.3f} 秒")

        t5 = time.time()
        # 格网初始化
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
        total_cloud_mask = np.zeros((target_H, target_W), dtype=bool)
        
        print(f"[⏱] 初始化图像数组耗时: {time.time() - t5:.3f} 秒")

        processed_scenes = 0
        filled_ratio = 0.0

        t6 = time.time()
        # 只处理相关场景
        for scene in relevant_scenes:
            scene_start = time.time()
            if 'SAR' in scene.get('sensorName'):
                continue

            nodata = scene.get('noData')
            try:
                if nodata is not None:
                    nodata_int = int(float(nodata))
                else:
                    nodata_int = 0
            except (ValueError, TypeError):
                nodata_int = 0
                print(f"警告: 无法转换nodata值 '{nodata}'，使用默认值0")

            scene_label = f"{scene.get('sensorName')}-{scene.get('sceneId')}-{scene.get('resolution')}"
            processed_scenes += 1

            cloud_band_path = scene.get('cloudPath')
            
            # 读取red波段获取nodata_mask
            red_band_path = scene_band_paths[scene['sceneId']]['Red']
            if not red_band_path:
                continue
            full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
            try:
                with COGReader(full_red_path, options={'nodata': int(nodata_int)}) as reader:
                    temp_img_data = reader.tile(x, y, z, tilesize=256)
                    nodata_mask = temp_img_data.mask.astype(bool)
            except Exception as e:
                print(f"无法读取文件 {full_red_path}: {str(e)}")
                continue

            valid_mask = nodata_mask
            
            # 处理云掩膜
            if cloud_band_path:
                cloud_full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                try:
                    with COGReader(cloud_full_path, options={'nodata': int(nodata_int)}) as reader:
                        cloud_img_data = reader.tile(x, y, z, tilesize=256)
                        img_data = cloud_img_data.data[0]

                        sensorName_type = scene.get('sensorName')
                        if "Landsat" in sensorName_type or "Landset" in sensorName_type:
                            cloud_mask = (img_data & (1 << 3)) > 0
                        elif "MODIS" in sensorName_type:
                            cloud_state = (img_data & 0b11)
                            cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                        elif "GF" in sensorName_type:    
                            cloud_mask = (img_data == 2)
                        else:
                            cloud_mask = np.zeros((target_H, target_W), dtype=bool)
                        
                        total_cloud_mask[need_fill_mask & valid_mask] |= cloud_mask[need_fill_mask & valid_mask]

                except Exception as e:
                    print(f"无法读取云文件 {cloud_full_path}: {str(e)}")
            
            fill_mask = need_fill_mask & valid_mask
            
            print(f"[⏱] 处理场景 {scene.get('sceneId')} 掩膜耗时: {time.time() - scene_start:.3f} 秒")
            
            if np.any(fill_mask):
                band_start = time.time()
                def read_band(band_path):
                    full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + band_path
                    try:
                        with COGReader(full_path, options={'nodata': int(nodata_int)}) as reader:
                            band_data = reader.tile(x, y, z, tilesize=256)
                            original_data = band_data.data[0]
                            original_dtype = original_data.dtype
                            converted_data = convert_to_uint8(original_data, original_dtype)
                            return converted_data
                    except Exception as e:
                        print(f"无法读取文件 {full_path}: {str(e)}")
                        return None

                bands = scene_band_paths[scene['sceneId']]
                band_1 = read_band(bands['Red'])
                band_2 = read_band(bands['Green'])
                band_3 = read_band(bands['Blue'])
                
                if band_1 is None or band_2 is None or band_3 is None:
                    print(f"警告: {scene_label} 读取波段失败")
                    continue

                img_r[fill_mask] = band_1[fill_mask]
                img_g[fill_mask] = band_2[fill_mask]
                img_b[fill_mask] = band_3[fill_mask]

                need_fill_mask[fill_mask] = False
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                
                print(f"[⏱] 读取和填充波段耗时: {time.time() - band_start:.3f} 秒")
                
            if not np.any(need_fill_mask):
                print(f"瓦片已填满，共处理 {processed_scenes} 个场景")
                break
                
        print(f"[⏱] 场景处理总耗时: {time.time() - t6:.3f} 秒")
        
        t7 = time.time()
        img = np.stack([img_r, img_g, img_b])

        # 透明度掩膜
        transparent_mask = need_fill_mask | total_cloud_mask
        alpha_mask = (~transparent_mask).astype(np.uint8) * 255

        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
        print(f"[⏱] 图像渲染耗时: {time.time() - t7:.3f} 秒")

        print(f"[⏱] 总耗时: {time.time() - start_time:.3f} 秒")
        print(f"[📊] 处理 {processed_scenes}/{len(scene_ids)} 个场景")
        print(f"[📊] 填充率: {filled_ratio:.2%}, 云覆盖率: {np.count_nonzero(total_cloud_mask) / total_cloud_mask.size:.2%}")
        
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(f"错误类型: {type(e).__name__}")
        print(f"错误信息: {str(e)}")
        import traceback
        traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

# 清理缓存的辅助函数
def clean_cache(max_age_seconds: int = 3600):
    """清理过期的缓存"""
    current_time = time.time()
    keys_to_delete = []
    for key, value in cache.items():
        if current_time - value['timestamp'] > max_age_seconds:
            keys_to_delete.append(key)
    
    for key in keys_to_delete:
        del cache[key]
    
    if keys_to_delete:
        print(f"[🧹] 清理了 {len(keys_to_delete)} 个过期缓存")