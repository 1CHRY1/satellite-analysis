from fastapi import APIRouter, Query, Response, HTTPException, Body
from typing import Tuple, List, Optional, Union, Dict, Any
from minio import Minio
import numpy as np
from io import BytesIO
import os, json, uuid
import requests
from config import minio_config, TRANSPARENT_CONTENT

import mercantile
from rio_tiler.profiles import img_profiles
from rio_tiler.io import COGReader
from cogeo_mosaic.mosaic import MosaicJSON
from rio_tiler_mosaic.mosaic import mosaic_tiler
from rio_tiler_mosaic.methods import defaults
from rio_tiler.colormap import cmap
from rio_tiler.utils import render
import time
from functools import lru_cache
import asyncio

#### Helper functions ##################################################################

MINIO_ENDPOINT = minio_config['endpoint']
MINIO_ACCESS_KEY = minio_config['access_key']
MINIO_SECRET_KEY = minio_config['secret_key']
MINIO_BUCKET = minio_config['bucket']
MINIO_DIR = minio_config['dir']

def tiler(src_path: str, *args, nodata: Optional[Union[float, int]] = None, **kwargs) -> Tuple[np.ndarray, np.ndarray]:
    with COGReader(src_path, options={"nodata": nodata}) as cog:
        return cog.tile(*args, **kwargs)

def normalize(arr, min_val=0, max_val=5000):
    arr = np.nan_to_num(arr)
    if arr.ndim == 3:
        # 如果 min_val/max_val 不是 list/array，则转成 array
        if isinstance(min_val, (int, float)):
            min_val = np.full(arr.shape[0], min_val)
        if isinstance(max_val, (int, float)):
            max_val = np.full(arr.shape[0], max_val)
        for i in range(arr.shape[0]):
            band_min = min_val[i]
            band_max = max_val[i]
            arr[i] = np.clip((arr[i] - band_min) / (band_max - band_min), 0, 1)
        arr = (arr * 255).astype("uint8")
    else:
        arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
        arr = (arr * 255).astype("uint8")
    return arr

def unique_filename() -> str:
    return str(uuid.uuid4())

def upload_to_minio(json_obj, object_name):
    client = Minio(
        MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=False
    )
    json_bytes = json.dumps(json_obj).encode("utf-8")
    client.put_object(
        MINIO_BUCKET,
        object_name,
        data=BytesIO(json_bytes),
        length=len(json_bytes),
        content_type="application/json"
    )

def fetch_mosaic_definition(mosaic_url):
    resp = requests.get(mosaic_url)
    resp.raise_for_status()
    return resp.json()


#### Router #############################################################################
router = APIRouter()

@router.post("/create_rgb_mosaics", summary="为RGB三个波段分别创建mosaicJSON")
def create_rgb_mosaics(
    red_files: List[str] = Body(..., description="红波段COG文件列表"),
    green_files: List[str] = Body(..., description="绿波段COG文件列表"), 
    blue_files: List[str] = Body(..., description="蓝波段COG文件列表"),
    minzoom: int = Body(4, description="最小缩放级别"),
    maxzoom: int = Body(20, description="最大缩放级别"),
    max_threads: int = Body(20, description="最大线程数"),
):
    """为RGB三个波段分别创建mosaicJSON"""
    
    # 创建三个独立的mosaicJSON
    red_mosaic = MosaicJSON.from_urls(red_files, minzoom=minzoom, maxzoom=maxzoom, max_threads=max_threads)
    green_mosaic = MosaicJSON.from_urls(green_files, minzoom=minzoom, maxzoom=maxzoom, max_threads=max_threads)
    blue_mosaic = MosaicJSON.from_urls(blue_files, minzoom=minzoom, maxzoom=maxzoom, max_threads=max_threads)
    
    # 上传到MinIO
    red_path = MINIO_DIR + "/" + unique_filename() + "_red.json"
    green_path = MINIO_DIR + "/" + unique_filename() + "_green.json"
    blue_path = MINIO_DIR + "/" + unique_filename() + "_blue.json"
    
    upload_to_minio(red_mosaic.model_dump(), red_path)
    upload_to_minio(green_mosaic.model_dump(), green_path)
    upload_to_minio(blue_mosaic.model_dump(), blue_path)
    
    return {
        "red_mosaic": {"bucket": MINIO_BUCKET, "object_path": red_path},
        "green_mosaic": {"bucket": MINIO_BUCKET, "object_path": green_path},
        "blue_mosaic": {"bucket": MINIO_BUCKET, "object_path": blue_path}
    }

@lru_cache(maxsize=128)
def fetch_mosaic_definition_cached(mosaic_url: str) -> str:  # 返回序列化后的字符串
    """缓存序列化后的结果"""
    result = fetch_mosaic_definition(mosaic_url)
    return json.dumps(result, sort_keys=True)  # 确保字典可哈希

def get_mosaic_definition(mosaic_url: str) -> Dict[str, Any]:
    """获取反序列化的结果"""
    cached = fetch_mosaic_definition_cached(mosaic_url)
    return json.loads(cached)

def get_rgb_tiles(
    red_url: str, 
    green_url: str, 
    blue_url: str, 
    z: int, 
    x: int, 
    y: int, 
    pixel_selection: str
) -> Tuple[Optional[Tuple[np.ndarray, np.ndarray]], ...]:
    """并行获取三个波段的瓦片数据"""
    
    # 并行获取三个mosaic定义
    # 使用安全的获取方式
    
    red_def, green_def, blue_def = asyncio.gather(
        asyncio.to_thread(get_mosaic_definition, red_url),
        asyncio.to_thread(get_mosaic_definition, green_url),
        asyncio.to_thread(get_mosaic_definition, blue_url)
    )
    
    def_list = [red_def, green_def, blue_def]
    data_list = []
    for item in def_list:
        quadkey_zoom = item["minzoom"]
        mercator_tile = mercantile.Tile(x=x, y=y, z=z)

        if mercator_tile.z > quadkey_zoom:
            depth = mercator_tile.z - quadkey_zoom
            for _ in range(depth):
                mercator_tile = mercantile.parent(mercator_tile)

        quadkey = mercantile.quadkey(*mercator_tile)
        assets = item["tiles"].get(quadkey)
        if not assets:
            return None

        method = pixel_selection.lower()
        if method == "highest":
            sel = defaults.HighestMethod()
        elif method == "lowest":
            sel = defaults.LowestMethod()
        else:
            sel = defaults.FirstMethod()

        start_time = time.time()
        img, mask = mosaic_tiler(assets, x, y, z, tiler, pixel_selection=sel, nodata=0)
        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"获取tiler操作耗时：{elapsed_time:.6f} 秒")
        if img is None:
            continue
        data_list.append((img, mask))

    # # 并行处理三个波段
    # print(red_assets)
    # print(green_assets)
    # print(blue_assets)
    # red_task = mosaic_tiler(red_assets, x, y, z, tiler, pixel_selection=sel, nodata=0)
    # green_task = mosaic_tiler(green_assets, x, y, z, tiler, pixel_selection=sel, nodata=0)
    # blue_task = mosaic_tiler(blue_assets, x, y, z, tiler, pixel_selection=sel, nodata=0)
    
    # red_data, green_data, blue_data = asyncio.gather(red_task, green_task, blue_task)
    
    return data_list[0]

@router.get("/rgb_mosaictile/{z}/{x}/{y}.png")
def rgb_mosaictile(
    z: int, x: int, y: int,
    red_mosaic_url: str = Query(..., description="红波段Mosaic JSON URL"),
    green_mosaic_url: str = Query(..., description="绿波段Mosaic JSON URL"),
    blue_mosaic_url: str = Query(..., description="蓝波段Mosaic JSON URL"),
    min_red: float = Query(0, description="红波段归一化最小值"),
    max_red: float = Query(255, description="红波段归一化最大值"),
    min_green: float = Query(0, description="绿波段归一化最小值"),
    max_green: float = Query(255, description="绿波段归一化最大值"),
    min_blue: float = Query(0, description="蓝波段归一化最小值"),
    max_blue: float = Query(255, description="蓝波段归一化最大值"),
    pixel_selection: str = Query("first", description="像素选择方法")
):
    """优化后的RGB瓦片生成端点"""
    try:
        # 并行获取三个波段数据
        start_time = time.time()
        red_data = get_rgb_tiles(
            red_mosaic_url,
            green_mosaic_url,
            blue_mosaic_url,
            z, x, y,
            pixel_selection
        )
        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"请求操作耗时：{elapsed_time:.6f} 秒")
        
        
        red_img, red_mask = red_data
        
        
        red_normalized = normalize(red_img, min_red, max_red)
        
        # 合成RGB图像
        
        # 合成RGB图像
        # rgb_img = np.stack([red_norm[0], green_norm[0], blue_norm[0]])
        # combined_mask = red_mask & green_mask & blue_mask
        
        # 调试日志（生产环境可移除）
        print(f"Processed tile z={z} x={x} y={y}")
        
        content = render(red_normalized[0], mask=red_mask)
        return Response(content=content, media_type="image/png")
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# @router.get("/rgb_mosaictile/{z}/{x}/{y}.png")
# def rgb_mosaictile(
#     z: int, x: int, y: int,
#     red_mosaic_url: str = Query(..., description="红波段Mosaic JSON URL"),
#     green_mosaic_url: str = Query(..., description="绿波段Mosaic JSON URL"),
#     blue_mosaic_url: str = Query(..., description="蓝波段Mosaic JSON URL"),
#     min_red: float = Query(0, description="红波段归一化最小值"),
#     max_red: float = Query(255, description="红波段归一化最大值"),
#     min_green: float = Query(0, description="绿波段归一化最小值"),
#     max_green: float = Query(255, description="绿波段归一化最大值"),
#     min_blue: float = Query(0, description="蓝波段归一化最小值"),
#     max_blue: float = Query(255, description="蓝波段归一化最大值"),
#     pixel_selection: str = Query("first", description="像素选择方法")
# ):
#     """从三个独立的mosaicJSON获取RGB瓦片"""
#     try:
#         start_time = time.time()
#         # 获取三个波段的数据
#         red_data = get_single_band_tile(red_mosaic_url, z, x, y, pixel_selection)
#         green_data = get_single_band_tile(green_mosaic_url, z, x, y, pixel_selection)
#         blue_data = get_single_band_tile(blue_mosaic_url, z, x, y, pixel_selection)
#         end_time = time.time()
#         elapsed_time = end_time - start_time
#         print(f"get band tile操作耗时：{elapsed_time:.6f} 秒", flush=True)
#         if red_data is None or green_data is None or blue_data is None:
#             return Response(status_code=204)
        
#         red_img, red_mask = red_data
#         green_img, green_mask = green_data
#         blue_img, blue_mask = blue_data
        
#         # 归一化各个波段
#         start_time = time.time()
#         red_normalized = normalize(red_img, min_red, max_red)
#         green_normalized = normalize(green_img, min_green, max_green)
#         blue_normalized = normalize(blue_img, min_blue, max_blue)
        
#         # 合成RGB图像
#         rgb_img = np.stack([red_normalized[0], green_normalized[0], blue_normalized[0]])
#         combined_mask = red_mask & green_mask & blue_mask
#         end_time = time.time()
#         elapsed_time = end_time - start_time
#         print(f"归一化合成操作耗时：{elapsed_time:.6f} 秒", flush=True)

#         print("Red mask dtype:", red_mask.dtype, "unique values:", np.unique(red_mask), flush=True)
#         print("Green mask dtype:", green_mask.dtype, "unique values:", np.unique(green_mask), flush=True)
#         print("Blue mask dtype:", blue_mask.dtype, "unique values:", np.unique(blue_mask), flush=True)
#         print("Combined mask dtype:", combined_mask.dtype, "unique values:", np.unique(combined_mask), flush=True)

        
#         content = render(rgb_img, mask=combined_mask)
#         return Response(content=content, media_type="image/png")
        
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))

# def get_single_band_tile(mosaic_url: str, z: int, x: int, y: int, pixel_selection: str):
#     """从单个mosaicJSON获取瓦片数据"""
#     mosaic_def = fetch_mosaic_definition(mosaic_url)
    
#     quadkey_zoom = mosaic_def["minzoom"]
#     mercator_tile = mercantile.Tile(x=x, y=y, z=z)

#     if mercator_tile.z > quadkey_zoom:
#         depth = mercator_tile.z - quadkey_zoom
#         for _ in range(depth):
#             mercator_tile = mercantile.parent(mercator_tile)

#     quadkey = mercantile.quadkey(*mercator_tile)
#     assets = mosaic_def["tiles"].get(quadkey)
#     if not assets:
#         return None

#     method = pixel_selection.lower()
#     if method == "highest":
#         sel = defaults.HighestMethod()
#     elif method == "lowest":
#         sel = defaults.LowestMethod()
#     else:
#         sel = defaults.FirstMethod()

#     img, mask = mosaic_tiler(assets, x, y, z, tiler, pixel_selection=sel, nodata=0)
#     if img is None:
#         return None
    
#     return img, mask
