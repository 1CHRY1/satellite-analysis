import asyncio
from fastapi import APIRouter, Query, Response, Request
from fastapi.concurrency import run_in_threadpool
from rio_tiler.io import Reader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
from rio_tiler.mosaic import mosaic_reader
from rio_tiler.mosaic.methods import HighestMethod
from rio_tiler.models import ImageData
import numpy as np
import os
import requests
import math
from config import minio_config, common_config, TRANSPARENT_CONTENT
import time
import traceback
import sys
import logging

# ----------------- 日志配置 -----------------
logging.basicConfig(stream=sys.stdout, level=logging.INFO)
logger = logging.getLogger("Tiler")

MINIO_ENDPOINT = "http://" + minio_config['endpoint']

# ----------------- 环境配置 -----------------
if "GDAL_DISABLE_READDIR_ON_OPEN" in os.environ: del os.environ["GDAL_DISABLE_READDIR_ON_OPEN"]
if "CPL_VSIL_CURL_ALLOWED_EXTENSIONS" in os.environ: del os.environ["CPL_VSIL_CURL_ALLOWED_EXTENSIONS"]
os.environ["GDAL_HTTP_TIMEOUT"] = "10"
os.environ["GDAL_HTTP_MAX_RETRY"] = "3"

# ----------------- 工具函数 -----------------
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
        "west": ul_lon_deg, "east": lr_lon_deg, "south": lr_lat_deg, "north": ul_lat_deg,
        "bbox": [ul_lon_deg, lr_lat_deg, lr_lon_deg, ul_lat_deg]
    }

# ----------------- 断连检查辅助函数 -----------------
async def check_disconnect(request: Request, step_name: str = ""):
    # 释放控制权，让 event loop 有机会处理断连信号
    await asyncio.sleep(0) 
    if await request.is_disconnected():
        print(f"🚫 [取消] 客户端断开连接 - 阶段: {step_name}")
        raise asyncio.CancelledError()

# ----------------- 单波段读取 -----------------
def fetch_band_serial(full_path, nodata_int, x, y, z):
    for attempt in range(2):
        try:
            with Reader(full_path, options={'nodata': nodata_int}) as reader:
                tile_obj = reader.tile(x, y, z, tilesize=256)
                data = tile_obj.data[0]
                mask = tile_obj.mask
                converted_data = convert_to_uint8(data, data.dtype)
                if mask.dtype == bool: mask = mask.astype("uint8") * 255
                if mask.max() == 1: mask = mask * 255
                return converted_data, mask
        except Exception:
            if attempt == 1: return None, None
            time.sleep(0.05)
    return None, None

# ----------------- Tiler -----------------
def custom_scene_tiler(scene, x, y, z, sensorName=None, mapper=None, **kwargs):
    try:
        if 'SAR' in str(sensorName): return None, None
        bucket_path = scene.get('bucket')
        paths = scene.get('path', {})
        
        bands_map = {'Red': None, 'Green': None, 'Blue': None}
        bandList = ['Red', 'Green', 'Blue']
        if sensorName == 'ZY1_AHSI':
            default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
            for band in bandList:
                k = f"band_{default_mapping.get(band)}"
                if k in paths: bands_map[band] = paths[k]
        else:
            for band in bandList:
                map_idx = mapper.get(band) if mapper else None
                k = f"band_{map_idx}"
                if map_idx and k in paths: bands_map[band] = paths[k]
                else:
                    if band == 'Red' and 'band_1' in paths: bands_map[band] = paths['band_1']
                    elif band == 'Green' and 'band_2' in paths: bands_map[band] = paths['band_2']
                    elif band == 'Blue' and 'band_3' in paths: bands_map[band] = paths['band_3']

        if not all(bands_map.values()): return None, None

        nodata_val = scene.get('noData', 0)
        try: nodata_int = int(float(nodata_val))
        except: nodata_int = 0

        # 串行读取
        red_path = f"{MINIO_ENDPOINT}/{bucket_path}/{bands_map['Red']}"
        red_data, red_mask = fetch_band_serial(red_path, nodata_int, x, y, z)
        if red_data is None: return None, None

        green_path = f"{MINIO_ENDPOINT}/{bucket_path}/{bands_map['Green']}"
        green_data, _ = fetch_band_serial(green_path, nodata_int, x, y, z)

        blue_path = f"{MINIO_ENDPOINT}/{bucket_path}/{bands_map['Blue']}"
        blue_data, _ = fetch_band_serial(blue_path, nodata_int, x, y, z)

        rgb_data = np.zeros((3, 256, 256), dtype="uint8")
        rgb_data[0] = red_data
        if green_data is not None: rgb_data[1] = green_data
        if blue_data is not None: rgb_data[2] = blue_data

        final_mask = red_mask.copy()
        # [核心清洗] 确保黑值为透明，配合 HighestMethod 使用
        is_black = (rgb_data[0] == 0) & (rgb_data[1] == 0) & (rgb_data[2] == 0)
        final_mask[is_black] = 0
        
        # 云处理
        cloud_path_suffix = scene.get('cloudPath')
        if cloud_path_suffix:
            try:
                cloud_full_path = f"{MINIO_ENDPOINT}/{bucket_path}/{cloud_path_suffix}"
                with Reader(cloud_full_path, options={'nodata': nodata_int}) as reader:
                    cloud_tile = reader.tile(x, y, z, tilesize=256)
                    cloud_data = cloud_tile.data[0]
                    is_cloud = (cloud_data > 0)
                    final_mask[is_cloud] = 0
            except:
                pass

        return ImageData(rgb_data, final_mask, assets=[bucket_path])
    except Exception:
        return None, None

# ----------------- 路由 -----------------
router = APIRouter()

@router.get("/{z}/{x}/{y}.png")
async def get_tile(
    request: Request, z: int, x: int, y: int, sensorName: str = Query(...),
):
    try:
        # [Check 1] 刚进来就检查
        await check_disconnect(request, "Start")
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']

        # 配置参数
        url = common_config['create_no_cloud_config_url']
        data_body = {"sensorName": sensorName, "points": points}
        headers = {}
        # 你加回来的 Header 逻辑
        authorization = request.headers.get('Authorization')
        if authorization: headers['Authorization'] = authorization
        cookie = request.headers.get('Cookie')
        if cookie: headers['Cookie'] = cookie
        
        def fetch_config_sync():
            return requests.post(url, json=data_body, headers=headers).json()
        
        # IO 阻塞操作 1：请求配置
        json_response = await run_in_threadpool(fetch_config_sync)
        
        # [Check 2] 拿到配置后立刻检查
        await check_disconnect(request, "After Config Fetch")

        if not json_response or json_response.get('status') == -1:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        mapper = json_response.get('data', {}).get('bandMapper', {})
        json_data = json_response.get('data', {}).get('scenesConfig', [])
        
        # TODO 后端可能也是根据bbox得出的覆盖率，如果bbox不准，假的bbox正好把这章瓦片都盖住，那可能就会出现锯齿？
        # full_coverage_scenes = [scene for scene in json_data if float(scene.get('coverage', 0)) >= 0.999]
        # if full_coverage_scenes:
        #     scenes_to_process = [full_coverage_scenes[0]]
        # else:
        #     max_scenes_to_process = min(len(json_data), 50)
        #     scenes_to_process = json_data[:max_scenes_to_process]
        max_scenes_to_process = min(len(json_data), 50)
        scenes_to_process = json_data[:max_scenes_to_process]

        if not scenes_to_process:
             return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        # [Check 3] 在最重的 Mosaic 计算之前检查
        await check_disconnect(request, "Before Mosaic")

        # IO/CPU 阻塞操作 2：Mosaic (核心耗时步骤)
        def run_mosaic_sync():
            return mosaic_reader(
                scenes_to_process,
                custom_scene_tiler, 
                x, y, z,
                allowed_exceptions=(Exception,), 
                sensorName=sensorName,
                mapper=mapper,
                pixel_selection=HighestMethod() # 核心修复
            )

        img_obj, _ = await run_in_threadpool(run_mosaic_sync)

        # [Check 4] Mosaic 算完了，渲染之前再检查一次
        await check_disconnect(request, "After Mosaic")

        if img_obj is None or img_obj.data is None:
             return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        # 终极清洗 (Render 前)
        final_rgb = img_obj.data
        final_mask = img_obj.mask
        is_pure_black = (final_rgb[0] == 0) & (final_rgb[1] == 0) & (final_rgb[2] == 0)
        final_mask[is_pure_black] = 0
        
        # CPU 阻塞操作 3：渲染图片
        content = await run_in_threadpool(
            render, 
            img_obj.data, 
            mask=final_mask,
            img_format="png", 
            **img_profiles.get("png")
        )
        return Response(content=content, media_type="image/png")

    except asyncio.CancelledError:
        return Response(status_code=204)
    except Exception as e:
        print(f"[ERROR] Tile {z}/{x}/{y}: {e}")
        # traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")