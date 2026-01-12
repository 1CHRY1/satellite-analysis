import asyncio
from fastapi import APIRouter, Query, Response, Request
from fastapi.concurrency import run_in_threadpool
from rio_tiler.io import Reader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
from rio_tiler.mosaic import mosaic_reader
from rio_tiler.models import ImageData
import numpy as np
import os
import requests
import math
from config import minio_config, common_config, TRANSPARENT_CONTENT
import time
import threading
import logging
from contextvars import ContextVar
from shapely.geometry import Polygon
from shapely.ops import transform
import pyproj
from concurrent.futures import ThreadPoolExecutor

MINIO_ENDPOINT = "http://" + minio_config['endpoint']

# region 工具函数 (保持不变)
def normalize(arr, min_val=0, max_val=5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")

def convert_to_uint8(data, original_dtype):
    if original_dtype == np.uint8:
        return data.astype(np.uint8)
    elif original_dtype == np.uint16:
        return (data / 65535.0 * 255.0).astype(np.uint8)
    else:
        return np.uint8(np.floor(data.clip(0, 255)))

def calc_tile_bounds(x, y, z):
    Z2 = math.pow(2, z)
    ul_lon_deg = x / Z2 * 360.0 - 180.0
    ul_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y / Z2)))
    ul_lat_deg = math.degrees(ul_lat_rad)
    
    lr_lon_deg = (x + 1) / Z2 * 360.0 - 180.0
    lr_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / Z2)))
    lr_lat_deg = math.degrees(lr_lat_rad)
    
    return {
        "west": ul_lon_deg,
        "east": lr_lon_deg,
        "south": lr_lat_deg,
        "north": ul_lat_deg,
        "bbox": [ul_lon_deg, lr_lat_deg, lr_lon_deg, ul_lat_deg]
    }
# endregion

def scene_tiler(scene, x, y, z, sensorName=None, mapper=None, **kwargs):
    """
    [并发版] 针对单个 Scene 并发读取 RGB，串行处理 Cloud，并返回 ImageData
    """
    # 1. 排除 SAR 数据
    if 'SAR' in str(sensorName):
        return None, None

    bucket_path = scene.get('bucket')
    paths = scene.get('path', {})
    
    # 构建 bands 映射
    bands_map = {'Red': None, 'Green': None, 'Blue': None}
    bandList = ['Red', 'Green', 'Blue']

    if sensorName == 'ZY1_AHSI':
        default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
        for band in bandList:
            band_key = f"band_{default_mapping.get(band, '1')}"
            if band_key in paths:
                bands_map[band] = paths[band_key]
            else:
                available_bands = sorted([k for k in paths.keys() if k.startswith('band_')])
                if available_bands:
                    bands_map[band] = paths[available_bands[0]]
    else:
        for band in bandList:
            map_idx = mapper.get(band) if mapper else None
            band_key = f"band_{map_idx}"
            if map_idx and band_key in paths:
                bands_map[band] = paths[band_key]
            else:
                if band == 'Red' and 'band_1' in paths:
                    bands_map[band] = paths['band_1']
                elif band == 'Green' and 'band_2' in paths:
                    bands_map[band] = paths['band_2']
                elif band == 'Blue' and 'band_3' in paths:
                    bands_map[band] = paths['band_3']

    # 如果任何一个波段缺失，直接返回
    if not all(bands_map.values()):
        return None, None

    nodata_val = scene.get('noData', 0)
    try:
        nodata_int = int(float(nodata_val))
    except:
        nodata_int = 0

    # ---------------------------------------------------------
    # [并发改造] 定义单个波段读取任务
    # ---------------------------------------------------------
    def fetch_single_band(band_name, path_suffix):
        full_path = f"{MINIO_ENDPOINT}/{bucket_path}/{path_suffix}"
        try:
            with Reader(full_path, options={'nodata': nodata_int}) as reader:
                tile_obj = reader.tile(x, y, z, tilesize=256)
                original_data = tile_obj.data[0]
                converted_data = convert_to_uint8(original_data, original_data.dtype)
                return band_name, converted_data, tile_obj.mask
        except Exception as e:
            # print(f"Error reading {band_name}: {e}")
            return band_name, None, None

    # 存放结果的字典，确保顺序
    results = {}
    
    # 使用线程池并发读取 3 个波段
    with ThreadPoolExecutor(max_workers=3) as executor:
        futures = []
        for b_name in bandList:
            path_suffix = bands_map[b_name]
            futures.append(executor.submit(fetch_single_band, b_name, path_suffix))
        
        # 获取结果
        for f in futures:
            b_name, data, mask = f.result()
            results[b_name] = {'data': data, 'mask': mask}

    # ---------------------------------------------------------
    # [验证] 检查是否所有波段都成功读取
    # ---------------------------------------------------------
    if any(results[b]['data'] is None for b in bandList):
        return None, None

    # ---------------------------------------------------------
    # [合并] 堆叠数据和合并 Mask
    # ---------------------------------------------------------
    try:
        # 按 R, G, B 顺序堆叠
        img_data_list = [results['Red']['data'], results['Green']['data'], results['Blue']['data']]
        rgb_data = np.stack(img_data_list) # Shape: (3, 256, 256)

        # 合并 Mask (取交集)
        base_mask = results['Red']['mask'] & results['Green']['mask'] & results['Blue']['mask']
    except Exception:
        return None, None

    # ---------------------------------------------------------
    # [去云] 云处理逻辑 (保持不变，或者你也可以把它放入线程池，但收益不大)
    # ---------------------------------------------------------
    cloud_path_suffix = scene.get('cloudPath')
    if cloud_path_suffix:
        try:
            cloud_full_path = f"{MINIO_ENDPOINT}/{bucket_path}/{cloud_path_suffix}"
            with Reader(cloud_full_path, options={'nodata': nodata_int}) as reader:
                cloud_tile = reader.tile(x, y, z, tilesize=256)
                cloud_data = cloud_tile.data[0]
                
                is_cloud = np.zeros(cloud_data.shape, dtype=bool)
                if "Landsat" in str(sensorName) or "Landset" in str(sensorName):
                    is_cloud = (cloud_data & (1 << 3)) > 0
                elif "MODIS" in str(sensorName):
                    cloud_state = (cloud_data & 0b11)
                    is_cloud = (cloud_state == 0) | (cloud_state == 1)
                elif "GF" in str(sensorName):
                    is_cloud = (cloud_data == 2)
                
                base_mask[is_cloud] = 0
        except Exception:
            pass

    return ImageData(rgb_data, base_mask, assets=[bucket_path])

async def check_disconnect(request: Request, step_name: str = ""):
    await asyncio.sleep(0) 
    if await request.is_disconnected():
        print(f"🚫 [取消] 客户端已断开连接。阶段: {step_name}")
        raise asyncio.CancelledError()

router = APIRouter()

@router.get("/{z}/{x}/{y}.png")
async def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    try:
        await check_disconnect(request, "Start")
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']

        url = common_config['create_no_cloud_config_url']
        data_body = {
            "sensorName": sensorName,
            "points": points
        }
        headers = {}
        authorization = request.headers.get('Authorization')
        if authorization:
            headers['Authorization'] = authorization
        cookie = request.headers.get('Cookie')
        if cookie:
            headers['Cookie'] = cookie

        def fetch_config_sync():
            return requests.post(url, json=data_body, headers=headers).json()
        
        json_response = await run_in_threadpool(fetch_config_sync)
        await check_disconnect(request, "After Config Fetch")

        if not json_response or json_response.get('status') == -1:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        mapper = json_response.get('data', {}).get('bandMapper', {})
        json_data = json_response.get('data', {}).get('scenesConfig', [])

        if not json_data:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        full_coverage_scenes = [scene for scene in json_data if float(scene.get('coverage', 0)) >= 0.999]
        
        scenes_to_process = []
        if full_coverage_scenes:
            scenes_to_process = [full_coverage_scenes[0]]
        else:
            max_scenes_to_process = min(len(json_data), 50)
            scenes_to_process = json_data[:max_scenes_to_process]

        await check_disconnect(request, "Before Mosaic")
        
        # -------------- region [关键修正] Mosaic 调用 --------------
        def run_mosaic_sync():
            # [修改] 直接返回 mosaic_reader 的结果，不尝试在内部解包成 img, mask
            # 因为它返回的是 (ImageData, list_of_assets)
            return mosaic_reader(
                scenes_to_process,
                scene_tiler,
                x, y, z,
                # threads=4,
                allowed_exceptions=(Exception,),
                sensorName=sensorName,
                mapper=mapper
            )

        # [修改] 接收返回值：img_obj 是 ImageData 对象，assets 是列表
        img_obj, assets = await run_in_threadpool(run_mosaic_sync)
        
        await check_disconnect(request, "After Mosaic")

        # [修改] 判空检查：检查对象是否存在以及对象内的数据是否存在
        if img_obj is None or img_obj.data is None:
             return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        # endregion

        # -------------- region [关键修正] 渲染 --------------
        
        # [修改] 从 ImageData 对象中提取 numpy 数组传给 render
        # img_obj.data -> (3, 256, 256) uint8
        # img_obj.mask -> (256, 256) uint8/bool
        
        content = await run_in_threadpool(
            render, 
            img_obj.data,      # 修正：传入 .data
            mask=img_obj.mask, # 修正：传入 .mask
            img_format="png", 
            **img_profiles.get("png")
        )
        
        return Response(content=content, media_type="image/png")
        # endregion

    except asyncio.CancelledError:
        return Response(status_code=204)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")