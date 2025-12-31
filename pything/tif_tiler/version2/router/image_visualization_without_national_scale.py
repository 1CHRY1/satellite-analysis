import asyncio
from fastapi import APIRouter, Query, Response, Request
from fastapi.concurrency import run_in_threadpool
from rio_tiler.io import Reader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
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

MINIO_ENDPOINT = "http://" + minio_config['endpoint']

# region 工具函数
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

def read_band(x, y, z, bucket_path, band_path, nodata_int):
    full_path = MINIO_ENDPOINT + "/" + bucket_path + "/" + band_path
    
    try:
        with Reader(full_path, options={'nodata': int(nodata_int)}) as reader:
            band_data = reader.tile(x, y, z, tilesize=256)
            original_data = band_data.data[0]
            original_dtype = original_data.dtype
            converted_data = convert_to_uint8(original_data, original_dtype)
            return converted_data
    except Exception as e:
        print(f"无法读取文件 {full_path}: {str(e)}", flush=True)
        return None


# 定义一个辅助函数来检查断开连接
async def check_disconnect(request: Request, step_name: str = ""):
    await asyncio.sleep(0) 
    if await request.is_disconnected():
        print(f"🚫 [取消] 客户端已断开连接。阶段: {step_name}")
        # 抛出 CancelledError 是中断执行的标准做法，或者直接 return Response
        raise asyncio.CancelledError()

router = APIRouter()

# 阻塞改造
@router.get("/{z}/{x}/{y}.png")
async def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    try:
        # -------------- region 获取tile的边界 --------------
        await check_disconnect(request, "Start")
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']
        # endregion

        # -------------- region 参数配置 --------------
        url = common_config['create_no_cloud_config_url']
        data = {
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
        # endregion

        # -------------- region [阻塞改造] 获取scenes和bandMapper --------------
        def fetch_config_sync():
            return requests.post(url, json=data, headers=headers).json()
        
        # 将 requests 放入线程池
        json_response = await run_in_threadpool(fetch_config_sync)

        # 这里的检查会非常灵敏，因为上一步 await 让出了控制权
        await check_disconnect(request, "After Config Fetch")

        if not json_response:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        if json_response.get('status') == -1:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        mapper = json_response.get('data', {}).get('bandMapper', {})

        json_data = json_response.get('data', {}).get('scenesConfig', [])

        if not json_data:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        # endregion
        
        # -------------- region 完全覆盖tile的景的列表 --------------
        full_coverage_scenes = [scene for scene in json_data if float(scene.get('coverage', 0)) >= 0.999]
        # endregion

        # -------------- region 选择要处理的景 --------------
        if full_coverage_scenes:
            # sorted_full_coverage = sorted(full_coverage_scenes, key=lambda x: float(x.get('cloud', 0)))
            scenes_to_process = [full_coverage_scenes[0]]
            use_single_scene = True
        else:
            max_scenes_to_process = min(len(json_data), 10)
            scenes_to_process = json_data[:max_scenes_to_process]
            use_single_scene = False
            
        # endregion

        # -------------- region 获取RGB三波段的路径 --------------
        scene_band_paths = {}
        bandList = ['Red', 'Green', 'Blue']

        for scene in scenes_to_process:
            bands = {band: None for band in bandList}
            paths = scene.get('path', {})

            if sensorName == 'ZY1_AHSI':
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
                for band in bandList:
                    band_key = f"band_{mapper[band]}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        if band == 'Red' and 'band_1' in paths:
                            bands[band] = paths['band_1']
                        elif band == 'Green' and 'band_2' in paths:
                            bands[band] = paths['band_2']
                        elif band == 'Blue' and 'band_3' in paths:
                            bands[band] = paths['band_3']

            scene_band_paths[scene['sceneId']] = bands
        # endregion

        # -------------- region 初始化 --------------
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool)

        total_cloud_mask = np.zeros((target_H, target_W), dtype=bool)

        filled_ratio = 0.0
        # endregion

        # -------------- region 处理每个景 --------------
        for scene in scenes_to_process:
            await check_disconnect(request, f"Processing Scene Start")
            # -------------- region SAR景不处理 --------------
            if 'SAR' in sensorName:
                continue
            # endregion
            nodata = scene.get('noData')

            # -------------- region 获取nodata --------------
            try:
                if nodata is not None:
                    nodata_int = int(float(nodata))
                else:
                    nodata_int = 0
            except (ValueError, TypeError):
                nodata_int = 0
            # endregion

            # 获取cloudPath
            cloud_band_path = scene.get('cloudPath')

            # -------------- region 获取有效像素的掩膜 --------------
            red_band_path = scene_band_paths[scene['sceneId']]['Red']
            if not red_band_path:
                continue

            full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
            # -------------- region [阻塞改造] 获取有效像素的掩膜 --------------
            def get_valid_mask_sync():
                with Reader(full_red_path, options={'nodata': int(nodata_int)}) as reader:
                    temp_img_data = reader.tile(x, y, z, tilesize=256)
                    return temp_img_data.mask.astype(bool)
            try:
                nodata_mask = await run_in_threadpool(get_valid_mask_sync)
            except Exception as e:
                continue

            valid_mask = nodata_mask
            # endregion

            # -------------- region [阻塞改造] 获取云掩膜 --------------
            if cloud_band_path:
                cloud_full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                def get_cloud_mask_sync():
                    with Reader(cloud_full_path, options={'nodata': int(nodata_int)}) as reader:
                        cloud_img_data = reader.tile(x, y, z, tilesize=256)
                        img_data = cloud_img_data.data[0]

                        if "Landsat" in sensorName or "Landset" in sensorName:
                            return (img_data & (1 << 3)) > 0
                        elif "MODIS" in sensorName:
                            cloud_state = (img_data & 0b11)
                            return (cloud_state == 0) | (cloud_state == 1)
                        elif "GF" in sensorName:
                            return (img_data == 2)
                        else:
                            return np.zeros((target_H, target_W), dtype=bool)
                try:
                    cloud_mask = await run_in_threadpool(get_cloud_mask_sync)
                    if use_single_scene:
                        total_cloud_mask = cloud_mask
                    else:
                        total_cloud_mask[need_fill_mask & valid_mask] |= cloud_mask[need_fill_mask & valid_mask]
                        
                except Exception as e:
                    print("Cannot read cloud file")
            # endregion
            
            # 获取可以填充的像素位置掩膜
            fill_mask = need_fill_mask & valid_mask
            
            await check_disconnect(request, "Before Pixel Fill")
            # -------------- region [阻塞改造] 填充像素 --------------
            if np.any(fill_mask) or use_single_scene:
                bands = scene_band_paths[scene['sceneId']]
                bucket_path = scene['bucket']

                # 定义同步读取函数，read_band 应该本身是同步的
                # 这里分别 wrap 或者一起 wrap 都行，分别 wrap 可以让每次读完都有机会 check disconnect
                band_1 = await run_in_threadpool(read_band, x, y, z, bucket_path, bands['Red'], nodata_int)
                band_2 = await run_in_threadpool(read_band, x, y, z, bucket_path, bands['Green'], nodata_int)
                band_3 = await run_in_threadpool(read_band, x, y, z, bucket_path, bands['Blue'], nodata_int)

                if band_1 is None or band_2 is None or band_3 is None:
                    continue

                if use_single_scene:
                    img_r[valid_mask] = band_1[valid_mask]
                    img_g[valid_mask] = band_2[valid_mask]
                    img_b[valid_mask] = band_3[valid_mask]
                    need_fill_mask[valid_mask] = False
                else:
                    img_r[fill_mask] = band_1[fill_mask]
                    img_g[fill_mask] = band_2[fill_mask]
                    img_b[fill_mask] = band_3[fill_mask]
                    need_fill_mask[fill_mask] = False

                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
            # endregion

            if use_single_scene:
                break
            elif filled_ratio >= 1:
                break
        

        await check_disconnect(request, "Before Render")
        # -------------- region [阻塞改造] 渲染 --------------
        img = np.stack([img_r, img_g, img_b])

        transparent_mask = need_fill_mask | total_cloud_mask
        alpha_mask = (~transparent_mask).astype(np.uint8) * 255
        # 渲染通常涉及大量压缩算法，是 CPU 密集型，必须 wrap
        content = await run_in_threadpool(
            render, 
            img, 
            mask=alpha_mask, 
            img_format="png", 
            **img_profiles.get("png")
        )
        return Response(content=content, media_type="image/png")
        # endregion
    # 专门捕获取消异常，返回 204 No Content
    except asyncio.CancelledError:
        return Response(status_code=204)
    except Exception as e:
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
