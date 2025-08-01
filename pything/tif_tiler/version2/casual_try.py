from fastapi import APIRouter, Query, Response, Request
from rio_tiler.io import COGReader
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

def read_band(x,y,z,bucket_path, band_path, nodata_int):
    full_path = MINIO_ENDPOINT + "/" + bucket_path + "/" + band_path

    try:
        with COGReader(full_path, options={'nodata': int(nodata_int)}) as reader:
            band_data = reader.tile(x,y,z.tilesize=256)
            original_data = band_data.data[0]
            original_dtype = original_data.dtype
            converted_data = convert_to_uint8(original, original_dtype)
            return converted_data
    except Exception as e:
        return None

router = APIRouter()
@router.get("/{z}/{x}/{y}.png")
def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    start_time = time.time()

    try:
        tile_bound = calc_tile_bounds(x,y,z)
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
        
        json_response = requests
