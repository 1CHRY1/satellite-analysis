from typing import Literal
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
    
@router.get("/tiles/{z}/{x}/{y}.png")
def rgb_tile(
    z: int, x: int, y: int,
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
    grids_boundary: str = Query(None, description="GeoJSON polygon string (JSON.stringify from frontend)"),
    min_r: int = Query(0, description="Minimum value of the red band"),
    min_g: int = Query(0, description="Minimum value of the green band"),
    min_b: int = Query(0, description="Minimum value of the blue band"),
    max_r: int = Query(5000, description="Maximum value of the red band"),
    max_g: int = Query(5000, description="Maximum value of the green band"),
    max_b: int = Query(5000, description="Maximum value of the blue band"),
    nodata: float = Query(0.0, description="No data value"),
):
    '''Get the RGB tile with optional GeoJSON polygon boundary'''
    try: 
        # Step 1: Read the three bands
        with COGReader(url_r, options={"nodata": nodata}) as cog_r:
            if(cog_r.tile_exists(x, y, z)):
                res = cog_r.tile(x, y, z)
                tile_r, _ = res
                mask = res.mask
            else :
                # print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        with COGReader(url_g) as cog_g:
            tile_g, _ = cog_g.tile(x, y, z)
            
        with COGReader(url_b) as cog_b:
            tile_b, _ = cog_b.tile(x, y, z)

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
                H, W = tile_r.squeeze().shape
                
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

        # Step 3: Combine the RGB (3, H, W)
        r = normalize(tile_r.squeeze(),  min_r, max_r)
        g = normalize(tile_g.squeeze(),  min_g, max_g)
        b = normalize(tile_b.squeeze(),  min_b, max_b)
        
        rgb = np.stack([r,g,b])

        # Step 4: Render the PNG
        content = render(rgb, mask=final_mask_uint8, img_format="png", **img_profiles.get("png"))

        return Response(content, media_type="image/png")
    
    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")



@router.get("/box/{z}/{x}/{y}.png")
def rgb_box_tile(
    z: int, x: int, y: int,
    bbox: str = Query(..., description="Bounding box (WGS84): minx,miny,maxx,maxy"),
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
    min_r: int = Query(0),
    min_g: int = Query(0),
    min_b: int = Query(0),
    max_r: int = Query(5000),
    max_g: int = Query(5000),
    max_b: int = Query(5000),
    nodata: float = Query(0.0, description="No data value"),
    stretch_method: Literal["linear", "gamma", "standard"]=Query('gamma', description="The Stretch Method"),
    # normalize_level: int = Query(0, ge=0, le=10, description="归一化分位区间级别，0=全范围，10=50%-50%"
    # normalize_level: float = Query(0, ge=0.1, le=10, description="gamma")
    normalize_level: float = Query(0, description="level of stretching")
):
    try: 
        '''Get the box RGB tile, too many parameters'''
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
            
        # Step 3: Read the three bands
        with COGReader(url_r, options={"nodata": nodata}) as cog_r:
            if(cog_r.tile_exists(x, y, z)):
                res = cog_r.tile(x, y, z)
                tile_r, _ = res
                mask = res.mask
            else :
                # print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        with COGReader(url_g) as cog_g:
            tile_g, _ = cog_g.tile(x, y, z)
            
        with COGReader(url_b) as cog_b:
            tile_b, _ = cog_b.tile(x, y, z)

        
        # Step 4: Generate the bbox mask (pixel level) and Combine the mask (nodata mask & bbox mask)
        H, W = tile_r.squeeze().shape
        xs = np.linspace(tile_wgs_bounds['west'], tile_wgs_bounds['east'], W)
        ys = np.linspace(tile_wgs_bounds['north'], tile_wgs_bounds['south'], H)
        lon, lat = np.meshgrid(xs, ys)  # 每个像素的经纬度坐标，基于此计算掩膜
        bbox_mask = (lon >= bbox_minx) & (lon <= bbox_maxx) & \
                    (lat >= bbox_miny) & (lat <= bbox_maxy)

        final_mask = np.logical_and(mask == 255, bbox_mask)
        final_mask_uint8 = final_mask.astype("uint8") * 255

        match stretch_method:
            case 'linear':
                if normalize_level == 0:
                    b_min_r, b_max_r = float(min_r), float(max_r)
                    b_min_g, b_max_g = float(min_g), float(max_g)
                    b_min_b, b_max_b = float(min_b), float(max_b)
                else:
                    b_min_r, b_max_r = get_percentile_range(min_r, max_r, normalize_level)
                    b_min_g, b_max_g = get_percentile_range(min_g, max_g, normalize_level)
                    b_min_b, b_max_b = get_percentile_range(min_b, max_b, normalize_level)

                r = normalize(tile_r.squeeze(), b_min_r, b_max_r)
                g = normalize(tile_g.squeeze(), b_min_g, b_max_g)
                b = normalize(tile_b.squeeze(), b_min_b, b_max_b)
            case 'gamma':
                b_min_r, b_max_r = float(min_r), float(max_r)
                b_min_g, b_max_g = float(min_g), float(max_g)
                b_min_b, b_max_b = float(min_b), float(max_b)

                r = normalize(tile_r.squeeze(), b_min_r, b_max_r)
                g = normalize(tile_g.squeeze(), b_min_g, b_max_g)
                b = normalize(tile_b.squeeze(), b_min_b, b_max_b)

                if normalize_level is not None and normalize_level > 0:
                    gamma = float(normalize_level)
                    r = np.clip(((r / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
                    g = np.clip(((g / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
                    b = np.clip(((b / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
            case 'standard':
                # TODO
                pass


        ####################### OLD START - 线性拉伸 #########################
        # # Step 5: 分别对每个波段做分位区间拉伸
        # if normalize_level == 0:
        #     b_min_r, b_max_r = float(min_r), float(max_r)
        #     b_min_g, b_max_g = float(min_g), float(max_g)
        #     b_min_b, b_max_b = float(min_b), float(max_b)
        # else:
        #     b_min_r, b_max_r = get_percentile_range(min_r, max_r, normalize_level)
        #     b_min_g, b_max_g = get_percentile_range(min_g, max_g, normalize_level)
        #     b_min_b, b_max_b = get_percentile_range(min_b, max_b, normalize_level)

        # r = normalize(tile_r.squeeze(), b_min_r, b_max_r)
        # g = normalize(tile_g.squeeze(), b_min_g, b_max_g)
        # b = normalize(tile_b.squeeze(), b_min_b, b_max_b)
        #####################################################################

        ####################### NEW START - gamma拉伸 ########################
        # # Step 5: 分别对每个波段做分位gamma拉伸
        # b_min_r, b_max_r = float(min_r), float(max_r)
        # b_min_g, b_max_g = float(min_g), float(max_g)
        # b_min_b, b_max_b = float(min_b), float(max_b)

        # r = normalize(tile_r.squeeze(), b_min_r, b_max_r)
        # g = normalize(tile_g.squeeze(), b_min_g, b_max_g)
        # b = normalize(tile_b.squeeze(), b_min_b, b_max_b)

        # if normalize_level is not None and normalize_level > 0:
        #     gamma = float(normalize_level)
        #     r = np.clip(((r / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
        #     g = np.clip(((g / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
        #     b = np.clip(((b / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
        # #####################################################################


        rgb = np.stack([r,g,b])

        # Step 6: Render the PNG
        content = render(rgb, mask=final_mask_uint8, img_format="png", **img_profiles.get("png"))

        return Response(content, media_type="image/png")
    
    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")




@DeprecationWarning
@router.get("/rgb/preview")
def rgb_preview(
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
    width: int = Query(1024, description="Maximum width of the preview image"),
    height: int = Query(1024, description="Maximum height of the preview image"),
    nodata: float = Query(0.0, description="No data value"),
):
    try:
        # 分别读取三个波段的 preview（自动缩放）
        with COGReader(url_r, options={"nodata": nodata}) as cog_r:
            img_r, _ = cog_r.preview(width=width, height=height)
        with COGReader(url_g) as cog_g:
            img_g, _ = cog_g.preview(width=width, height=height)
        with COGReader(url_b) as cog_b:
            img_b, _ = cog_b.preview(width=width, height=height)
            
        r = normalize(img_r.squeeze())
        g = normalize(img_g.squeeze())
        b = normalize(img_b.squeeze())
        
        rgb = np.stack([r,g,b])


        # 渲染为 PNG
        content = render(rgb, img_format="png", **img_profiles.get("png"))
        
        return Response(content, media_type="image/png")
    
    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    

def get_percentile_range(min_val, max_val, level):
    """
    线性区间拉伸，根据level计算min/max的线性插值
    
    Args:
        min_val: 最小值（用户指定的）
        max_val: 最大值（用户指定的）
        level: 拉伸级别，0=全范围，10=中点
        
    Returns:
        b_min, b_max: 拉伸后的最小最大值
    """
    # 线性区间百分比
    left = 0.05 * level
    right = 1 - 0.05 * level
    b_min = min_val + (max_val - min_val) * left
    b_max = min_val + (max_val - min_val) * right
    return float(b_min), float(b_max)