from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import os

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


def _encode_terrain_rgb(dem: np.ndarray) -> np.ndarray:
    # 替换 NaN 和无穷值 全为 0
    height = np.nan_to_num(dem, nan=0, posinf=0, neginf=0).astype(np.float32)

    # 避免溢出：先限制 height 范围
    height = np.clip(height, -9000, 9000)  # 珠穆朗玛也就8848m，保守限制到9000

    base = (height + 10000) * 10
    R = np.floor(base / (256 * 256))
    G = np.floor((base - R * 256 * 256) / 256)
    B = np.floor(base - R * 256 * 256 - G * 256)

    print("Height min:", np.min(height), "max:", np.max(height))

    rgb = np.stack([R, G, B], axis=-1)
    rgb = np.nan_to_num(rgb, nan=0, posinf=0, neginf=0)  # 安全防护

    # Clamp 到 uint8 合法范围后再转换
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)

    return rgb



####### Router ########################################################################################

router = APIRouter()

@router.get("/terrainRGB/{z}/{x}/{y}.png")
async def get_tile(
    z: int, x: int, y: int,
    url: str = Query(...),
):

    try:
        cog_path = url
        
        with COGReader(cog_path) as cog:
            if(cog.tile_exists(x, y, z)):
                tile_data = cog.tile(x, y, z)

                dem = tile_data.data[0]
            else :
                print("tile not exist", z, x, y)
                return Response(content=ZERO_HEIGHT, media_type="image/png")
            
        rgb = _encode_terrain_rgb(dem)
        # rgb shape --> (H, W, 3)
        # render need shape --> (3, H, W)
        content = render(rgb.transpose(2, 0, 1), img_format="png", **img_profiles.get("png")) 
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print("Terrain tile error:", e)
        return Response(content=ZERO_HEIGHT, media_type="image/png")