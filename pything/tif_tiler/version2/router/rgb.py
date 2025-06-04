from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import math

####### Helper ########################################################################################

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()

def normalize(arr, min_val = 0 , max_val = 5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")
    
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


####### Router ########################################################################################

router = APIRouter()
    
@router.get("/tiles/{z}/{x}/{y}.png")
def rgb_tile(
    z: int, x: int, y: int,
    url_r: str = Query(...),
    url_g: str = Query(...),
    url_b: str = Query(...),
    min_r: int = Query(0, description="Minimum value of the red band"),
    min_g: int = Query(0, description="Minimum value of the green band"),
    min_b: int = Query(0, description="Minimum value of the blue band"),
    max_r: int = Query(5000, description="Maximum value of the red band"),
    max_g: int = Query(5000, description="Maximum value of the green band"),
    max_b: int = Query(5000, description="Maximum value of the blue band"),
    nodata: float = Query(0.0, description="No data value"),
):

    try: 
        # 读取三个波段
        with COGReader(url_r, options={"nodata": nodata}) as cog_r:
            if(cog_r.tile_exists(x, y, z)):
                res = cog_r.tile(x, y, z)
                tile_r, _ = res
                mask = res.mask
            else :
                print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        with COGReader(url_g) as cog_g:
            tile_g, _ = cog_g.tile(x, y, z)
            
        with COGReader(url_b) as cog_b:
            tile_b, _ = cog_b.tile(x, y, z)

        # 组合成 RGB (3, H, W)
        r = normalize(tile_r.squeeze(),  min_r, max_r)
        g = normalize(tile_g.squeeze(),  min_g, max_g)
        b = normalize(tile_b.squeeze(),  min_b, max_b)
        
        rgb = np.stack([r,g,b])

        # 渲染为 PNG
        content = render(rgb, mask=mask, img_format="png", **img_profiles.get("png"))

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
):
    try: 

        try:
            bbox_minx, bbox_miny, bbox_maxx, bbox_maxy = map(float, bbox.split(","))
        except Exception:
            return Response(status_code=400, content=b"Invalid bbox format")
        
        tile_wgs_bounds: dict = tile_bounds(x, y, z)

        # 2. 计算 tile 与 bbox 的空间交集
        intersection_minx = max(tile_wgs_bounds["west"], bbox_minx)
        intersection_miny = max(tile_wgs_bounds["south"], bbox_miny)
        intersection_maxx = min(tile_wgs_bounds["east"], bbox_maxx)
        intersection_maxy = min(tile_wgs_bounds["north"], bbox_maxy)
        
        if intersection_minx >= intersection_maxx or intersection_miny >= intersection_maxy:
            # 没有交集，返回透明瓦片
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            
        # 读取三个波段
        with COGReader(url_r, options={"nodata": nodata}) as cog_r:
            if(cog_r.tile_exists(x, y, z)):
                res = cog_r.tile(x, y, z)
                tile_r, _ = res
                mask = res.mask
            else :
                print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        with COGReader(url_g) as cog_g:
            tile_g, _ = cog_g.tile(x, y, z)
            
        with COGReader(url_b) as cog_b:
            tile_b, _ = cog_b.tile(x, y, z)

        
        # 4. 生成 bbox 掩膜（像素级别）
        H, W = tile_r.squeeze().shape
        xs = np.linspace(tile_wgs_bounds['west'], tile_wgs_bounds['east'], W)
        ys = np.linspace(tile_wgs_bounds['north'], tile_wgs_bounds['south'], H)
        lon, lat = np.meshgrid(xs, ys)  # 每个像素的 WGS84 坐标
        bbox_mask = (lon >= bbox_minx) & (lon <= bbox_maxx) & \
                    (lat >= bbox_miny) & (lat <= bbox_maxy)

        # 5. 合并掩膜（COG 内部掩膜 & bbox 掩膜）
        final_mask = np.logical_and(mask == 255, bbox_mask)
        final_mask_uint8 = final_mask.astype("uint8") * 255


        # 组合成 RGB (3, H, W)
        r = normalize(tile_r.squeeze(), min_r, max_r)
        g = normalize(tile_g.squeeze(), min_g, max_g)
        b = normalize(tile_b.squeeze(), min_b, max_b)
        
        rgb = np.stack([r,g,b])

        # 渲染为 PNG
        content = render(rgb, mask=final_mask_uint8, img_format="png", **img_profiles.get("png"))

        return Response(content, media_type="image/png")
    
    except Exception as e:
        print(e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")





############### Deprecated Temporarily ###############################################################
# @router.get("/rgb/preview")
# def rgb_preview(
#     url_r: str = Query(...),
#     url_g: str = Query(...),
#     url_b: str = Query(...),
#     width: int = Query(1024, description="Maximum width of the preview image"),
#     height: int = Query(1024, description="Maximum height of the preview image"),
#     nodata=nodata
# ):
#     try:
#         # 分别读取三个波段的 preview（自动缩放）
#         with COGReader(url_r, options={"nodata": nodata}) as cog_r:
#             img_r, _ = cog_r.preview(width=width, height=height)
#         with COGReader(url_g) as cog_g:
#             img_g, _ = cog_g.preview(width=width, height=height)
#         with COGReader(url_b) as cog_b:
#             img_b, _ = cog_b.preview(width=width, height=height)
            
#         r = normalize(img_r.squeeze())
#         g = normalize(img_g.squeeze())
#         b = normalize(img_b.squeeze())
        
#         rgb = np.stack([r,g,b])


#         # 渲染为 PNG
#         content = render(rgb, img_format="png", **img_profiles.get("png"))
        
#         return Response(content, media_type="image/png")
    
#     except Exception as e:
#         print(e)
#         return Response(content=TRANSPARENT_CONTENT, media_type="image/png")