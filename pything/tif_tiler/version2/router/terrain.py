from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import math
import json
from shapely.geometry import Polygon, Point
from shapely.ops import unary_union
from rasterio.features import geometry_mask
from rasterio.transform import from_bounds
from config import minio_config, TRANSPARENT_CONTENT

####### Helper ########################################################################################
# Terrain-RGB Reference: https://docs.mapbox.com/data/tilesets/reference/mapbox-terrain-rgb-v1/

def zero_height_rgb_tile(size: int = 256) -> bytes:
    
    # 0m -> Terrain-RGB编码：(0 + 10000) * 10 = 100000
    # R = floor(100000 / (256*256)) = 1
    # G = floor((100000 - 1*256*256) / 256) = 134
    # B = 100000 - 1*256*256 - 134*256 = 160
    rgb = np.full((size, size, 3), [1, 134, 160], dtype=np.uint8)
    return render(rgb.transpose(2, 0, 1), img_format="png", **img_profiles.get("png"))

ZERO_HEIGHT = zero_height_rgb_tile()

def tile_bounds(x, y, z):
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
    }
    
    return result

def _encode_terrain_rgb(dem: np.ndarray, mask: np.ndarray = None, scale_factor: float = 1.0) -> np.ndarray:
    """
    修改后的函数，正确处理掩膜，并支持高程缩放
    Args:
        dem: DEM数据
        mask: 掩膜数据
        scale_factor: 高程缩放因子，1.0表示不缩放，0.1表示缩小10倍
    """
    # 先保存原始的有效数据位置
    if mask is not None:
        valid_mask = mask == 255
        # 将掩膜外的数据设为 NaN，保持掩膜内的原始高程不变
        height = dem.copy().astype(np.float32)
        height[~valid_mask] = np.nan
    else:
        height = dem.astype(np.float32)
    
    # 只对真正的 NaN 和无穷值进行处理（不包括我们刚才设置的掩膜外NaN）
    original_nan_mask = np.isnan(dem) | np.isinf(dem)
    height = np.nan_to_num(height, nan=0, posinf=0, neginf=0)

    # 应用缩放因子
    height = height * scale_factor

    # 避免溢出：先限制 height 范围
    height = np.clip(height, -9000, 9000)

    base = (height + 10000) * 10
    R = np.floor(base / (256 * 256))
    G = np.floor((base - R * 256 * 256) / 256)
    B = np.floor(base - R * 256 * 256 - G * 256)

    print("Height min:", np.min(height[mask == 255] if mask is not None else height), 
          "max:", np.max(height[mask == 255] if mask is not None else height),
          "scale_factor:", scale_factor)

    rgb = np.stack([R, G, B], axis=-1)
    rgb = np.nan_to_num(rgb, nan=0, posinf=0, neginf=0)

    # Clamp 到 uint8 合法范围后再转换
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)

    return rgb



####### Router ########################################################################################

router = APIRouter()

# 修改主要的路由函数中的掩膜处理部分
@router.get("/terrainRGB/{z}/{x}/{y}.png")
def get_tile(
    z: int, x: int, y: int,
    url: str = Query(...),
    grids_boundary: str = Query(None, description="GeoJSON polygon string (JSON.stringify from frontend)"),
    scale_factor: float = Query(1.0, description="高程缩放因子，1.0表示不缩放，0.1表示缩小10倍"),
):
    try:
        cog_path = url
        
        # Step 1: Get the tile data
        with COGReader(cog_path) as cog:
            if(cog.tile_exists(x, y, z)):
                tile_data = cog.tile(x, y, z)
                dem = tile_data.data[0]
                mask = tile_data.mask
            else :
                print("tile not exist", z, x, y)
                return Response(content=ZERO_HEIGHT, media_type="image/png")
            
        # Step 2: Process GeoJSON boundary if provided
        if grids_boundary:
            try:
                # Parse the GeoJSON polygon
                geojson_data = json.loads(grids_boundary)
                if geojson_data.get("type") == "FeatureCollection":
                    # 处理 FeatureCollection
                    polygons = []
                    for feature in geojson_data["features"]:
                        if feature["geometry"]["type"] == "Polygon":
                            coords = feature["geometry"]["coordinates"][0]  # 取外环
                            polygons.append(Polygon(coords))
                        elif feature["geometry"]["type"] == "MultiPolygon":
                            for poly_coords in feature["geometry"]["coordinates"]:
                                polygons.append(Polygon(poly_coords[0]))  # 取每个多边形的外环
                    if polygons:
                        boundary_polygon = unary_union(polygons)
                    else:
                        return Response(status_code=400, content=b"No valid polygons found in GeoJSON")
                elif geojson_data.get("type") == "Feature":
                    # 处理单个 Feature
                    if geojson_data["geometry"]["type"] == "Polygon":
                        coords = geojson_data["geometry"]["coordinates"][0]
                        boundary_polygon = Polygon(coords)
                    elif geojson_data["geometry"]["type"] == "MultiPolygon":
                        polygons = []
                        for poly_coords in geojson_data["geometry"]["coordinates"]:
                            polygons.append(Polygon(poly_coords[0]))
                        boundary_polygon = unary_union(polygons)
                    else:
                        return Response(status_code=400, content=b"Unsupported geometry type")
                elif geojson_data.get("type") == "Polygon":
                    # 处理单个 Polygon
                    coords = geojson_data["coordinates"][0]
                    boundary_polygon = Polygon(coords)
                else:
                    return Response(status_code=400, content=b"Unsupported GeoJSON type")
                
                # 优化方案：先检查瓦片是否与多边形相交
                tile_wgs_bounds = tile_bounds(x, y, z)
                tile_bbox = Polygon([
                    (tile_wgs_bounds["west"], tile_wgs_bounds["south"]),
                    (tile_wgs_bounds["east"], tile_wgs_bounds["south"]),
                    (tile_wgs_bounds["east"], tile_wgs_bounds["north"]),
                    (tile_wgs_bounds["west"], tile_wgs_bounds["north"]),
                    (tile_wgs_bounds["west"], tile_wgs_bounds["south"])
                ])
                
                # 如果瓦片与多边形不相交，直接返回透明瓦片
                if not tile_bbox.intersects(boundary_polygon):
                    return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
                
                H, W = dem.shape
                transform = from_bounds(
                    tile_wgs_bounds['west'], tile_wgs_bounds['south'], 
                    tile_wgs_bounds['east'], tile_wgs_bounds['north'], 
                    W, H
                )
                
                # 生成多边形掩膜 (True = 多边形内部)
                polygon_mask = geometry_mask(
                    [boundary_polygon], 
                    out_shape=(H, W), 
                    transform=transform, 
                    invert=True
                )
                
                # 关键修改：创建最终掩膜
                # 只保留原始有效数据 AND 在多边形内部的像素
                final_mask = np.logical_and(mask == 255, polygon_mask)
                final_mask_uint8 = final_mask.astype("uint8") * 255
                
            except Exception as e:
                return Response(status_code=400, content=f"Invalid GeoJSON format: {str(e)}".encode())
        else:
            # No boundary provided, use original mask
            final_mask_uint8 = mask
        
        # 修改这里：将掩膜传递给编码函数
        rgb = _encode_terrain_rgb(dem, final_mask_uint8, scale_factor)
        
        # rgb shape --> (H, W, 3)
        # render need shape --> (3, H, W)
        content = render(rgb.transpose(2, 0, 1), img_format="png", **img_profiles.get("png")) 
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print("Terrain tile error:", e)
        return Response(content=ZERO_HEIGHT, media_type="image/png")