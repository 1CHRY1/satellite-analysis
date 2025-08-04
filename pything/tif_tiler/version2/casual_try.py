from fastapi import APIRouter, Query, Response, Request
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

MINIO_ENDPOINT = "http://" + minio_config['endpoint']

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
        logger.error(f"无法读取文件 {full_path}: {str(e)}")
        return None


router = APIRouter()
@router.get("/{z}/{x}/{y}.png")
def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    try:
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']

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

        json_response = requests.post(url, json=data, headers=headers).json()

        if not json_response:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        if json_response.get('status') == -1:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        mapper = json_response.get('data', {}).get('bandMapper', {})

        json_data = json_response.get('data', {}).get('scenesConfig', [])

        if not json_data:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        full_coverage_scenes = [scene for scene in json_data if float(scene.get('coverage', 0)) >= 0.999]

        if full_coverage_scenes:
            sorted_full_coverage = sorted(full_coverage_scenes, key=lambda x: float(x.get('cloud', 0)))
            scenes_to_process = [sorted_full_coverage[0]]
            use_single_scene = True
        else:
            if z > 12:
                scenes_to_process = json_data[:10]
            else:
                scenes_to_process = json_data[:]
            use_single_scene = False
        
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
        
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool)

        total_cloud_mask = np.zeros((target_H, target_W), dtype=bool)

        filled_ratio = 0.0

        for scene in scenes_to_process:
            if 'SAR' in sensorName:
                continue
            
            nodata = scene.get('noData')

            try:
                if nodata is not None:
                    nodata_int = int(float(nodata))
                else:
                    nodata_int = 0
            except (ValueError, TypeError):
                nodata_int = 0
            
            cloud_band_path = scene.get('cloudPath')

            red_band_path = scene_band_paths[scene['sceneId']]['Red']
            if not red_band_path:
                continue
            
            full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
            try:
                with Reader(full_red_path, options={'nodata': int(nodata_int)}) as reader:
                    temp_img_data = reader.tile(x, y, z, tilesize=256)
                    nodata_mask = temp_img_data.mask.astype(bool)
            except Exception as e:
                continue

            valid_mask = nodata_mask

            if cloud_band_path:
                cloud_full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                try:
                    with Reader(cloud_full_path, options={'nodata': int(nodata_int)}) as reader:
                        cloud_img_data = reader.tile(x, y, z, tilesize=256)
                        img_data = cloud_img_data.data[0]

                        if "Landsat" in sensorName or "Landset" in sensorName:
                            cloud_mask = (img_data & (1 << 3)) > 0
                        elif "MODIS" in sensorName:
                            cloud_state = (img_data & 0b11)
                            cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                        elif "GF" in sensorName:
                            cloud_mask = (img_data == 2)
                        else:
                            cloud_mask = np.zeros((target_H, target_W), dtype=bool)
                        
                        if use_single_scene:
                            total_cloud_mask = cloud_mask
                        else:
                            total_cloud_mask[need_fill_mask & valid_mask] |= cloud_mask[need_fill_mask & valid_mask]
                except Exception as e:
                    print("Cannot read cloud file")
            
            fill_mask = need_fill_mask & valid_mask

            if np.any(fill_mask) or use_single_scene:
                bands = scene_band_paths[scene['sceneId']]
                bucket_path = scene['bucket']

                band_1 = read_band(x, y, z, bucket_path, bands['Red'], nodata_int)
                band_2 = read_band(x, y, z, bucket_path, bands['Green'], nodata_int)
                band_3 = read_band(x, y, z, bucket_path, bands['Blue'], nodata_int)

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
            
            if use_single_scene:
                break
            elif filled_ratio >= 0.95:
                break
        
        img = np.stack([img_r, img_g, img_b])

        transparent_mask = need_fill_mask | total_cloud_mask
        alpha_mask = (~transparent_mask).astype(np.uint8) * 255

        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
        return Response(content=content, media_type="image/png")
    
    except Exception as e:
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")