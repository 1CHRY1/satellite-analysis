from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import requests
import math
from shapely.geometry import shape, box
from config import minio_config, TRANSPARENT_CONTENT
import time

# ---------------------------------- Constants & Config ---------------------------------- #
MINIO_ENDPOINT = f"http://{minio_config['endpoint']}"
BAND_LIST = ['Red', 'Green', 'Blue']
TARGET_SIZE = (256, 256)  # (Height, Width)

# ---------------------------------- Helper Functions ---------------------------------- #
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

def tile_bounds(x, y, z):
    Z2 = math.pow(2, z)
    ul_lon, lr_lon = x / Z2 * 360.0 - 180.0, (x + 1) / Z2 * 360.0 - 180.0
    ul_lat = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * y / Z2))))
    lr_lat = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / Z2))))
    return {
        "west": ul_lon, "east": lr_lon,
        "south": lr_lat, "north": ul_lat,
        "bbox": [ul_lon, lr_lat, lr_lon, ul_lat]
    }

def is_tile_covered(scene, lon_lats):
    scene_geom = scene.get('bbox', {}).get('geometry')
    if not scene_geom:
        return False
    polygon = shape(scene_geom)
    bbox_polygon = box(*lon_lats['bbox'])
    return polygon.intersects(bbox_polygon)

def read_band_data(full_path, nodata):
    try:
        with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
            tile = reader.tile(x, y, z, tilesize=256)
            return tile.data[0], tile.mask
    except Exception as e:
        print(f"读取失败: {full_path}, 错误: {str(e)}")
        return None, None

def extract_cloud_mask(sensor_name, data):
    if "Landsat" in sensor_name:
        return (data & (1 << 3)) > 0
    elif "MODIS" in sensor_name:
        return (data & 0b11) in [0, 1]
    elif "GF" in sensor_name:
        return data == 2
    return np.zeros_like(data, dtype=bool)

def resolve_band_paths(scene):
    mapper, paths = scene.get('bandMapper', {}), scene.get('paths', {})
    resolved = {band: None for band in BAND_LIST}
    if scene['sensorName'] == 'ZY1_AHSI':
        default = {'Red': '4', 'Green': '3', 'Blue': '2'}
        for band in BAND_LIST:
            key = f"band_{default.get(band, '1')}"
            resolved[band] = paths.get(key) or next((paths[k] for k in sorted(paths) if k.startswith('band_')), None)
    else:
        for band in BAND_LIST:
            key = f"band_{mapper.get(band)}"
            resolved[band] = paths.get(key) or paths.get(f"band_{BAND_LIST.index(band)+1}")
    return resolved

# ---------------------------------- API Router ---------------------------------- #
router = APIRouter()

@router.get("/{z}/{x}/{y}")
def get_tile(z: int, x: int, y: int, jsonUrl: str = Query(...)):
    start_time = time.time()
    try:
        json_data = requests.get(jsonUrl).json().get('scenes', [])
        lon_lats = tile_bounds(x, y, z)
        sorted_scenes = sorted(json_data, key=lambda s: float(s['resolution'].replace("m", "")))
        scene_band_paths = {s['sceneId']: resolve_band_paths(s) for s in json_data}

        H, W = TARGET_SIZE
        img_r, img_g, img_b = [np.zeros((H, W), dtype=np.uint8) for _ in range(3)]
        need_fill = np.ones((H, W), dtype=bool)

        for scene in sorted_scenes:
            if 'SAR' in scene.get('sensorName') or not is_tile_covered(scene, lon_lats):
                continue

            scene_id, nodata = scene['sceneId'], scene.get('noData')
            bands = scene_band_paths[scene_id]
            cloud_path = scene.get('cloudPath')

            red_path = MINIO_ENDPOINT + f"/{scene['bucket']}/{bands['Red']}"
            red_data, nodata_mask = read_band_data(red_path, nodata)
            if red_data is None:
                continue

            valid_mask = nodata_mask.astype(bool)
            if cloud_path:
                cloud_full_path = MINIO_ENDPOINT + f"/{scene['bucket']}/{cloud_path}"
                cloud_data, _ = read_band_data(cloud_full_path, nodata)
                if cloud_data is not None:
                    cloud_mask = extract_cloud_mask(scene['sensorName'], cloud_data)
                    valid_mask = valid_mask & (~cloud_mask)

            fill_mask = need_fill & valid_mask
            if not np.any(fill_mask):
                continue

            def load_and_convert(band):
                path = MINIO_ENDPOINT + f"/{scene['bucket']}/{bands[band]}"
                data, _ = read_band_data(path, nodata)
                if data is not None:
                    return convert_to_uint8(data, data.dtype)
                return None

            r, g, b = map(load_and_convert, BAND_LIST)
            if None in (r, g, b):
                continue

            img_r[fill_mask], img_g[fill_mask], img_b[fill_mask] = r[fill_mask], g[fill_mask], b[fill_mask]
            need_fill[fill_mask] = False

            if not np.any(need_fill):
                break

        img = np.stack([img_r, img_g, img_b])
        alpha = (~need_fill).astype(np.uint8) * 255
        content = render(img, mask=alpha, img_format="png", **img_profiles.get("png"))
        print(f"[⏱] 总耗时: {time.time() - start_time:.3f} 秒")
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(f"错误类型: {type(e).__name__}\n错误信息: {e}")
        import traceback; traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
