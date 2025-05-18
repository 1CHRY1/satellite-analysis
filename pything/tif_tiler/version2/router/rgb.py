from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import os

router = APIRouter()

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")

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
            mask = res.mask
        else :
            return Response(content=open(TRANSPARENT_PNG, "rb").read(), media_type="image/png")
    with COGReader(url_g) as cog_g:
        tile_g, _ = cog_g.tile(x, y, z)
        
    with COGReader(url_b) as cog_b:
        tile_b, _ = cog_b.tile(x, y, z)

    # 组合成 RGB (3, H, W)
    rgb = np.stack([tile_r.squeeze(), tile_g.squeeze(), tile_b.squeeze()])

    # 渲染为 PNG
    content = render(rgb, mask=mask, img_format="png", **img_profiles.get("png"))

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