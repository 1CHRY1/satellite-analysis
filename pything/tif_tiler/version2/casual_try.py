from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
from cogeo_mosaic.mosaic import MosaicJSON
from rio_tiler_mosaic.mosaic import mosaic_tiler
from rio_tiler_mosaic.methods import defaults
import numpy as np
import os
import requests
import math
import mercantile
from config import minio_config, TRANSPARENT_CONTENT
import time
from typing import Tuple, Dict, List
from concurrent.futures import ThreadPoolExecutor

####### Helper ########################################################################################
MINIO_ENDPOINT = "http://" + minio_config['endpoint']

def normalize(arr, min_val = 0 , max_val = 5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")

def convert_to_uint8(data, original_dtype):
    """自动将不同数据类型转换为uint8"""
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

router = APIRouter()

@router.get("/{z}/{x}/{y}")
def get_tile(
    z: int, x: int, y: int,
    sensorName: str = Query(...),
    startTime: str = Query(...),
    endTime: str = Query(...),
):
    try:
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']

        url = "http://192.168.1.127:8999/api/v1/modeling/example/noCloud/createNoCloudConfig"
        data = {
            "sensorName": sensorName,
            "startTime": startTime,
            "endTime": endTime,
            "points": points
        }
        json_response = requests.post(url, json=data).json()

        mapper = json_response.get('data', {}).get('bandMapper', {})

        json_data = json_response.get('data', {}).get('scenesConfig', [])

        def sort_key(scene):
            has_cloud = 1 if scene.get('cloudPath') else 0
            resolution = float(scene["resolution"].replace("m", ""))
            return (has_cloud, resolution)
        
        sorted_scene = sorted(json_data, key=sort_key)

        band_urls = {'Red': [], 'Green': [], 'Blue': []}
        scene_info_map = {}

        for scene in sorted_scene:
            if 'SAR' in scene.get('sensorName', ''):
                continue

            paths = scene.get('path', {})
            bucket = scene['bucket']

            if sensorName == 'ZY1_AHSI':
                default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
                for band, band_num in default_mapping.items():
                    band_key = f"band_{band_num}"
                    if band_key in paths:
                        full_url = f"{MINIO_ENDPOINT}/{bucket}/{paths[band_key]}"
                        band_urls[band].append(full_url)
                        scene_info_map[full_url] = scene
            else:
                for band, band_num in mapper.items():
                    band_key = f"band_{band_num}"
                    if band_key in paths:
                        full_url = f"{MINIO_ENDPOINT}/{bucket}/{paths[band_key]}"
                        band_urls[band].append(full_url)
                        scene_info_map[full_url] = scene
                    else:
                        # 备选波段处理
                        backup_mapping = {'Red': 'band_1', 'Green': 'band_2', 'Blue': 'band_3'}
                        if backup_mapping.get(band) in paths:
                            full_url = f"{MINIO_ENDPOINT}/{bucket}/{paths[backup_mapping[band]]}"
                            band_urls[band].append(full_url)
                            scene_info_map[full_url] = scene
        
        mosaics = {}
        for band_name, urls in band_urls.items():
            


    except Exception as e:
        print(f"错误类型: {type(e).__name__}")
        print(f"错误信息: {str(e)}")
        import traceback
        traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")