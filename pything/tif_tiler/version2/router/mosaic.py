from fastapi import APIRouter, Query, Response, HTTPException, Body
from typing import Tuple, List, Optional, Union
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

@router.post("/create", summary="Create a mosaicJSON object based on the list of COG files")
def create_mosaic(
    files: List[str] = Body(..., description="COG paths list"),
    minzoom: int = Body(4, description="Minimum zoom level"),
    maxzoom: int = Body(20, description="Maximum zoom level"),
    max_threads: int = Body(20, description="Maximum number of threads"),
):
    '''Create a mosaicJSON object based on the list of COG files'''
    # Step 1: Create mosaicJSON object
    mosaic = MosaicJSON.from_urls(
        urls=files,
        minzoom=minzoom,
        maxzoom=maxzoom,
        max_threads=max_threads,
    )

    # Step 2: Upload mosaicJSON to MinIO
    object_path = MINIO_DIR + "/" + unique_filename() + ".json"
    upload_to_minio(mosaic.model_dump(), object_path)
    
    # Step 3: Response mosaicJSON URL
    res = json.dumps({
        "bucket": MINIO_BUCKET,
        "object_path": object_path,
    })

    return Response(content=res, media_type="application/json")


@router.put("/update", summary="Update the mosaicJSON object based on the list of additional COG files")
def update_mosaicjson(
    files: List[str] = Body(..., description="COG paths list"),
    mosaic_url: str = Body(..., description="mosaicjson full path"),
    max_threads: int = Body(20, description="Maximum number of threads")
):
    '''Update the mosaicJSON object based on the list of additional COG files'''
    try:
        # Step 1: Download the existing mosaicJSON
        mosaic_def = fetch_mosaic_definition(mosaic_url)
        
        # Step 2: Merge COG files
        all_files = files + mosaic_def.get("files", [])

        # Step 3: Re-generate MosaicJSON
        mosaic = MosaicJSON.from_urls(
            urls=all_files,
            minzoom=mosaic_def.get("minzoom"),
            maxzoom=mosaic_def.get("maxzoom"),
            max_threads=max_threads,
        )

        # Step 4: Upload back to MinIO, overwrite the existing object
        from urllib.parse import urlparse, unquote
        parsed = urlparse(mosaic_url)
        path = unquote(parsed.path.lstrip("/"))
        bucket = path.split("/", 1)[0]
        object_name = path.split("/", 1)[1]
        upload_to_minio(mosaic.model_dump(), object_name)

        res = json.dumps({
            "bucket": bucket,
            "object_path": object_name,
        })
        return Response(content=res, media_type="application/json")        
        
    except Exception as e:
        return Response(content=str(e), media_type="text/plain", status_code=500)


from functools import lru_cache
import concurrent.futures

# 【优化建议 1】: 加上缓存装饰器 (如果 fetch_mosaic_definition 是你自己写的)
# 避免每次请求 tile 都去重新下载几十 MB 的 MosaicJSON
# @lru_cache(maxsize=128) 
# def fetch_mosaic_definition(url): ...

@router.get("/mosaictile/{z}/{x}/{y}.png")
def mosaictile(
    z: int, x: int, y: int,
    mosaic_url: str = Query(..., description="Mosaic JSON URL"),
    min: float = Query(0, description="Normalization minimum value"),
    max: float = Query(255, description="Normalization maximum value"),
    pixel_selection: str = Query("first", description="Pixel selection method : first/highest/lowest"),
    bands_index: str = Query("0,1,2", description="Comma-separated list of band indices for RGB visualization")
):
    try:
        # Step 1: Fetch mosaic definition 
        # 【建议】确保这个函数内部有缓存机制，否则这是第一大卡顿点
        mosaic_def = fetch_mosaic_definition(mosaic_url)
        quadkey_zoom = mosaic_def["minzoom"]
        mercator_tile = mercantile.Tile(x=x, y=y, z=z)

        # Step 2: Configure pixel selection method
        method = pixel_selection.lower()
        if method == "highest":
            sel = defaults.HighestMethod()
        elif method == "lowest":
            sel = defaults.LowestMethod()
        else:
            sel = defaults.FirstMethod()

        # Step 3: Get assets list
        assets_list = [] # 初始化移到外面

        if z < quadkey_zoom:
            # 低级别瓦片 -> 计算对应8级瓦片范围
            factor = 2 ** (quadkey_zoom - z)
            x_start = x * factor
            y_start = y * factor

            # 【优化 2】: 使用 Set 进行去重，避免重复下载同一个 TIF
            # 许多 quadkey 可能指向同一个 asset，特别是大图覆盖多个网格时
            unique_assets = set()

            for xx in range(x_start, x_start + factor):
                for yy in range(y_start, y_start + factor):
                    qk = mercantile.quadkey(xx, yy, quadkey_zoom)
                    assets = mosaic_def["tiles"].get(qk)
                    if assets:
                        # 仅添加未见过的 asset
                        for asset in assets:
                            if asset not in unique_assets:
                                unique_assets.add(asset)
                                assets_list.append(asset)
        else:
            # z >= quadkey_zoom
            if mercator_tile.z > quadkey_zoom:
                depth = mercator_tile.z - quadkey_zoom
                for _ in range(depth):
                    mercator_tile = mercantile.parent(mercator_tile)

            quadkey = mercantile.quadkey(*mercator_tile)
            assets = mosaic_def["tiles"].get(quadkey)
            if assets:
                assets_list = assets # 直接赋值

        # 统一检查空列表
        if not assets_list:
            return Response(status_code=204)
            
        # 【优化 3 (关键)】: 开启多线程并发 (threads)
        # rio-tiler 的 mosaic_reader/tiler 支持 threads 参数
        # 这会让几十个文件的请求同时发出，而不是一个接一个等待
        # 注意：这里我们没有改动 tiler 函数本身，只是改了调用方式
        img, mask = mosaic_tiler(
            assets_list, 
            x, y, z, 
            tiler, 
            pixel_selection=sel, 
            nodata=0,
            threads=8,  # <--- 新增：8个线程并发读取 Minio
            # allowed_exceptions=(FileNotFoundError,) # 可选：防止某个文件丢失导致整个崩溃
        )

        if img is None:
            return Response(status_code=204)
        
        # Step 5: Normalize, render and response
        bands, height, width = img.shape

        # Parse the bands_index parameter
        selected_bands = list(map(int, bands_index.split(',')))

        # Validate
        if any(b < 0 or b >= bands for b in selected_bands):
            raise ValueError(f"Invalid band indices. Valid indices are between 0 and {bands - 1}.")
        
        img = img[selected_bands]

        # 【优化 4】: 简化 Numpy 操作 (微小性能提升，主要是代码整洁)
        if len(selected_bands) == 1:
            img = np.repeat(img, 3, axis=0)
        elif len(selected_bands) == 2:
            # 使用 efficient 的方式补齐
            img = np.concatenate([img, img[:1]], axis=0)
        elif len(selected_bands) >= 3:
            img = img[:3]
        
        # 渲染
        content = render(img, mask)
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(f"Error in mosaictile: {e}") # 建议打印更详细的日志
        raise HTTPException(status_code=500, detail=str(e))
    

# 针对无云一版图计算结果mosaic进行波段计算分析
@router.get("/analysis/{z}/{x}/{y}.png")
def analysis_tile(
    z: int, x: int, y: int,
    mosaic_url: str = Query(..., description="Mosaic JSON URL"),
    expression: str = Query(..., description="Expression to calculate (e.g. 'b1/b2+b3')"),
    pixel_method: str = Query("first", description="Pixel selection method : first/highest/lowest"),
    color: str = Query("rdylgn", description="Color map"),
    # colormap Reference: https://cogeotiff.github.io/rio-tiler/colormap/#default-rio-tilers-colormaps
):
    try:
        # Step 1: Fetch mosaic definition 
        mosaic_def = fetch_mosaic_definition(mosaic_url)

        # Step 2: Get assets list using mercantile and mosaicJSON minzoom
        quadkey_zoom = mosaic_def["minzoom"]
        mercator_tile = mercantile.Tile(x=x, y=y, z=z)
    
        if mercator_tile.z > quadkey_zoom:
            depth = mercator_tile.z - quadkey_zoom
            for _ in range(depth):
                mercator_tile = mercantile.parent(mercator_tile)
    
        quadkey = mercantile.quadkey(*mercator_tile)
        assets = mosaic_def["tiles"].get(quadkey)
        if not assets:
            return Response(status_code=204)
       
        # Step 3: Configure pixel selection method
        method = pixel_method.lower()
        if method == "highest":
            sel = defaults.HighestMethod()
        elif method == "lowest":
            sel = defaults.LowestMethod()
        else:
            sel = defaults.FirstMethod()
        
        # Step 4: Get tile from mosaic  , expression=expression
        img, mask = mosaic_tiler(assets, x, y, z, tiler, pixel_selection=sel, expression=expression, nodata=0)
        if img is None:
            return Response(status_code=204)
 
        # Step 5: Render
        # img_uint8 = normalize(img[0], np.min(img[0]), np.max(img[0]))
        img_uint8 = normalize(img[0], -1, 1)
        content = render(img_uint8, mask, img_format="png", colormap=cmap.get(color), **img_profiles.get("png"))
        # 不要拉伸
        # content = render(img[0], mask, img_format="png", colormap=cmap.get(color), **img_profiles.get("png"))
        return Response(content=content, media_type="image/png")
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail=str(e))


# localhost:8000/mosaic/analysis/13/6834/3215.png?mosaic_url=http://192.168.1.105:30900/temp-files/mosaicjson/c6a36030-ae52-4513-8d2f-739895c60c65.json&expression=b1/b2+b3
# http://localhost:8000/mosaic/analysis/13/6834/3215.png?mosaic_url=http://192.168.1.105:30900/temp-files/mosaicjson/c6a36030-ae52-4513-8d2f-739895c60c65.json&expression=b1-b2