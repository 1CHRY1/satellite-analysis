from fastapi import APIRouter, Query, Response, HTTPException, Body
from typing import Tuple, List
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

def tiler(src_path: str, *args, **kwargs) -> Tuple[np.ndarray, np.ndarray]:
    with COGReader(src_path) as cog:
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
async def create_mosaic(
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
async def update_mosaicjson(
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


@router.get("/mosaictile/{z}/{x}/{y}.png")
async def mosaictile(
    z: int, x: int, y: int,
    mosaic_url: str = Query(..., description="Mosaic JSON URL"),
    min: float = Query(0, description="Normalization minimum value"),
    max: float = Query(255, description="Normalization maximum value"),
    pixel_selection: str = Query("first", description="Pixel selection method : first/highest/lowest")
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
        method = pixel_selection.lower()
        if method == "highest":
            sel = defaults.HighestMethod()
        elif method == "lowest":
            sel = defaults.LowestMethod()
        else:
            sel = defaults.FirstMethod()
        
        # Step 4: Get tile from mosaic
        img, mask = mosaic_tiler(assets, x, y, z, tiler, pixel_selection=sel)
        if img is None:
            return Response(status_code=204)
        
        # Step 5: Normalize, render and response
        # min_val = [np.min(arr, axis=(0, 1)) for arr in img]  # 各个波段 min
        # max_val = [np.max(arr, axis=(0, 1)) for arr in img]  # 各个波段 max
        # img_uint8 = normalize(img, min_val, max_val)
        bands, height, width = img.shape

        if bands == 1:
            # 单波段 → 伪RGB（重复3次）
            img = np.repeat(img, 3, axis=0)
        elif bands == 2:
            # 两波段 → 伪RGB（重复前两个波段）
            img = np.tile(img, (2, 1, 1))[:3]
        elif bands >= 3:
            # 三波段及以上 → 取前三个波段
            img = img[:3]
        else:
            raise ValueError(f"Unsupported number of bands: {bands}")
        content = render(img, mask)
        assert isinstance(content, bytes), f"render 返回类型为 {type(content)}"
        return Response(content=content, media_type="image/png")
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail=str(e))
    

# 针对无云一版图计算结果mosaic进行波段计算分析
@router.get("/analysis/{z}/{x}/{y}.png")
async def analysis_tile(
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
        img, mask = mosaic_tiler(assets, x, y, z, tiler, pixel_selection=sel, expression=expression)
        if img is None:
            return Response(status_code=204)
 
        # Step 5: Render
        img_uint8 = normalize(img[0], np.min(img[0]), np.max(img[0]))
        content = render(img_uint8, mask, img_format="png", colormap=cmap.get(color), **img_profiles.get("png"))
        return Response(content=content, media_type="image/png")
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail=str(e))


# localhost:8000/mosaic/analysis/13/6834/3215.png?mosaic_url=http://192.168.1.105:30900/temp-files/mosaicjson/c6a36030-ae52-4513-8d2f-739895c60c65.json&expression=b1/b2+b3
# http://localhost:8000/mosaic/analysis/13/6834/3215.png?mosaic_url=http://192.168.1.105:30900/temp-files/mosaicjson/c6a36030-ae52-4513-8d2f-739895c60c65.json&expression=b1-b2