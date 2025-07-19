from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
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

def normalize(arr, min_val = 0 , max_val = 5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")

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





####### Router ########################################################################################
router = APIRouter()

@router.get("/colorband/{z}/{x}/{y}.png")
async def get_tile(
    z: int, x: int, y: int,
    url: str = Query(...),
    min: float = Query(-1, description="Minimum value of the color band"),
    max: float = Query(1, description="Maximum value of the color band"),
    color: str = Query("rdylgn", description="Color map"),
    # colormap Reference: https://cogeotiff.github.io/rio-tiler/colormap/#default-rio-tilers-colormaps
    grids_boundary: str = Query(None, description="Grids boundary"),
    nodata: float = Query(9999.0, description="No data value"),
):

    try:
        cog_path = url
        cm = cmap.get(color)
        with COGReader(cog_path, options={"nodata": nodata}) as cog:
            if(cog.tile_exists(x, y, z)):

                tile_data = cog.tile(x, y, z)
                img = tile_data.data
                mask = tile_data.mask
            else :
                print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            
            # # 如果启用自动计算min/max
            # if min is None or max is None:
            #     # 只考虑有效数据（mask == 255）
            #     valid_data = img[0][mask == 255]
            #     if len(valid_data) > 0:
            #         # 移除NaN和无穷值
            #         valid_data = valid_data[~np.isnan(valid_data) & ~np.isinf(valid_data)]
            #         if len(valid_data) > 0:
            #             min = float(np.min(valid_data))
            #             max = float(np.max(valid_data))
            #             print(f"Auto calculated min: {min}, max: {max}")
            #         else:
            #             print("No valid data found for auto min/max calculation")
            #     else:
            #         print("No valid data found for auto min/max calculation")

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
                
                # 使用 rasterio 的向量化操作生成掩膜
                H, W = tile_data.data.squeeze().shape
                
                # 创建从瓦片边界到像素坐标的变换矩阵
                transform = from_bounds(
                    tile_wgs_bounds['west'], tile_wgs_bounds['south'], 
                    tile_wgs_bounds['east'], tile_wgs_bounds['north'], 
                    W, H
                )
                
                # 使用向量化操作生成多边形掩膜
                # invert=True 表示多边形内部为 True，外部为 False
                polygon_mask = geometry_mask(
                    [boundary_polygon], 
                    out_shape=(H, W), 
                    transform=transform, 
                    invert=True
                )
                
                # 合并掩膜 (nodata mask & polygon mask)
                final_mask = np.logical_and(mask == 255, polygon_mask)
                final_mask_uint8 = final_mask.astype("uint8") * 255
                
            except Exception as e:
                return Response(status_code=400, content=f"Invalid GeoJSON format: {str(e)}".encode())
        else:
            # No boundary provided, use original mask
            final_mask_uint8 = mask
          
        normed = normalize(img[0], min, max)
        
        content = render(normed, mask=final_mask_uint8, img_format="png", colormap=cm, **img_profiles.get("png"))
            
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    

@router.get("/box/{z}/{x}/{y}.png")
async def get_box_tile(
    z: int, x: int, y: int,
    bbox: str = Query(..., description="Bounding box (WGS84): minx,miny,maxx,maxy"),
    url: str = Query(...),
    b_min: float = Query(-1, description="Minimum value of the color band"),
    b_max: float = Query(1, description="Maximum value of the color band"),
    color: str = Query("rdylgn", description="Color map"),
    # colormap Reference: https://cogeotiff.github.io/rio-tiler/colormap/#default-rio-tilers-colormaps
    normalize_level: int = Query(0, description="The normalize level"),
    nodata: float = Query(9999.0, description="No data value"),
):
    # Step 1: Parse the bbox
    try:
        bbox_minx, bbox_miny, bbox_maxx, bbox_maxy = map(float, bbox.split(","))
    except Exception:
        return Response(status_code=400, content=b"Invalid bbox format")

    # Step 2: Calculate the intersection of the tile and the bbox
    tile_wgs_bounds: dict = tile_bounds(x, y, z)

    intersection_minx = max(tile_wgs_bounds["west"], bbox_minx)
    intersection_miny = max(tile_wgs_bounds["south"], bbox_miny)
    intersection_maxx = min(tile_wgs_bounds["east"], bbox_maxx)
    intersection_maxy = min(tile_wgs_bounds["north"], bbox_maxy)
    
    if intersection_minx >= intersection_maxx or intersection_miny >= intersection_maxy:
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

    # Step3: 
    try:
        cog_path = url
        cm = cmap.get(color)
        with COGReader(cog_path, options={"nodata": nodata}) as cog:
            if(cog.tile_exists(x, y, z)):

                tile_data = cog.tile(x, y, z)
                img = tile_data.data
                mask = tile_data.mask
            else :
                print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            
        H,W = img.squeeze().shape
        xs = np.linspace(tile_wgs_bounds['west'], tile_wgs_bounds['east'], W)
        ys = np.linspace(tile_wgs_bounds['north'], tile_wgs_bounds['south'], H)
        lon, lat = np.meshgrid(xs, ys)  # 每个像素的经纬度坐标，基于此计算掩膜
        bbox_mask = (lon >= bbox_minx) & (lon <= bbox_maxx) & \
                    (lat >= bbox_miny) & (lat <= bbox_maxy)
        final_mask = np.logical_and(mask == 255, bbox_mask)
        final_mask_uint8 = final_mask.astype("uint8") * 255

        # 拉伸增强
        b_min, b_max = get_percentile_range(img[0], mask, nodata, normalize_level)

        normed = normalize(img[0], b_min, b_max)
        
        content = render(normed, mask=final_mask_uint8, img_format="png", colormap=cm, **img_profiles.get("png"))
            
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    

def get_percentile_range(data, mask, nodata, level):
    # 只考虑有效像素
    valid = (mask == 255) & (~np.isnan(data)) & (~np.isinf(data)) & (data != nodata)
    valid_data = data[valid]
    if len(valid_data) == 0:
        return float(np.nanmin(data)), float(np.nanmax(data))
    # 计算分位区间
    left = 0.5 * level
    right = 100 - 0.5 * level
    b_min = float(np.percentile(valid_data, left))
    b_max = float(np.percentile(valid_data, right))
    return b_min, b_max