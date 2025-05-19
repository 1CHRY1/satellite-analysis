from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import math

router = APIRouter()

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()
@router.get("/rgb/tiles/{z}/{x}/{y}.png")
def rgb_tile(
    z: int, x: int, y: int,
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
):

    # 读取三个波段
    with COGReader(url_r) as cog_r:
        if(cog_r.tile_exists(x, y, z)):
            res = cog_r.tile(x, y, z)
            tile_r, _ = res
            # mask = res.mask
        else :
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    with COGReader(url_g) as cog_g:
        tile_g, _ = cog_g.tile(x, y, z)
        
    with COGReader(url_b) as cog_b:
        tile_b, _ = cog_b.tile(x, y, z)

    # 组合成 RGB (3, H, W)
    rgb = np.stack([tile_r.squeeze(), tile_g.squeeze(), tile_b.squeeze()])

    # 渲染为 PNG
    content = render(rgb, img_format="png", **img_profiles.get("png"))

    return Response(content, media_type="image/png")


@router.get("/rgb/preview")
def rgb_preview(
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
    width: int = Query(1024, description="Maximum width of the preview image"),
    height: int = Query(1024, description="Maximum height of the preview image"),
):
    # 分别读取三个波段的 preview（自动缩放）
    with COGReader(url_r) as cog_r:
        img_r, _ = cog_r.preview(width=width, height=height)
    with COGReader(url_g) as cog_g:
        img_g, _ = cog_g.preview(width=width, height=height)
    with COGReader(url_b) as cog_b:
        img_b, _ = cog_b.preview(width=width, height=height)


    # 合成 RGB 图像
    rgb = np.stack([img_r.squeeze(), img_g.squeeze(), img_b.squeeze()]) #(1, 1024, 1024)
    
    print(rgb.shape) # (3, 1, 1024, 1024)

    # 渲染为 PNG
    content = render(rgb, img_format="png", **img_profiles.get("png"))
    
    return Response(content, media_type="image/png")



@router.get("/rgb/box/{z}/{x}/{y}.png")
def rgb_box_tile(
    z: int, x: int, y: int,
    bbox: str = Query(..., description="Bounding box (WGS84): minx,miny,maxx,maxy"),
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
):
    # 1. 解析 bbox 参数
    try:
        bbox_minx, bbox_miny, bbox_maxx, bbox_maxy = map(float, bbox.split(","))
        bbox_extent = (bbox_minx, bbox_miny, bbox_maxx, bbox_maxy)
    except Exception:
        return Response(status_code=400, content=b"Invalid bbox")

    # 2. 获取当前 tile 的地理边界 (WGS84)
    tile_bounds_wgs84 = tile_bounds(x, y, z)

    # 3. 判断当前 tile 是否与 bbox 有交集
    intersection_minx = max(tile_bounds_wgs84['west'], bbox_minx)
    intersection_miny = max(tile_bounds_wgs84['south'], bbox_miny)
    intersection_maxx = min(tile_bounds_wgs84['east'], bbox_maxx)
    intersection_maxy = min(tile_bounds_wgs84['north'], bbox_maxy)


    if intersection_minx >= intersection_maxx or intersection_miny >= intersection_maxy:
        # 无交集，返回透明图
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

    bounds = (intersection_minx, intersection_miny, intersection_maxx, intersection_maxy)
    

    try:
        # 4. 读取三个波段中 bbox 与 tile 区域的交集部分
        with COGReader(url_r) as cog_r:
            imgData = cog_r.part(bounds, width=512, height=512)
            mask = imgData.mask
            tile_r, _ = imgData        
        with COGReader(url_g) as cog_g:
            tile_g, _ = cog_g.part(bounds, width=512, height=512)
        with COGReader(url_b) as cog_b:
            tile_b, _ = cog_b.part(bounds, width=512, height=512)

        rgb = np.ma.stack([tile_r.squeeze(), tile_g.squeeze(), tile_b.squeeze()])

        content = render(rgb, mask=mask, img_format="png", **img_profiles.get("png"))
        return Response(content, media_type="image/png")
    
    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
 

def tile_bounds(x, y, z):

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