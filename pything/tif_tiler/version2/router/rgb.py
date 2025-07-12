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


####### Helper ########################################################################################

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()


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
    nodata: float = Query(0.0, description="No data value")
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

        r = normalize(tile_r.squeeze(), min_r, max_r)
        g = normalize(tile_g.squeeze(), min_g, max_g)
        b = normalize(tile_b.squeeze(), min_b, max_b)
        rgb = np.stack([r,g,b])

        # Step 5: Render the PNG
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