from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import os

####### Helper ########################################################################################

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()

def _encode_terrain_rgb(dem: np.ndarray) -> bytes:
    """ Terrain-RGB """
    height = dem.astype(np.float32)

    r = np.floor((height + 10000) / 0.1)
    g = np.floor((height + 10000 - r * 0.1) / 0.001)
    b = np.floor((height + 10000 - r * 0.1 - g * 0.001) / 0.00001)
    rgb = np.stack([r, g, b], axis=2).astype(np.uint8)

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
            # dem = tile_data.data[0]  # 高程数据数组
            if(cog.tile_exists(x, y, z)):
                tile_data, mask = cog.tile(x, y, z)
                # img, _ = tile_data
                mask = tile_data.mask
                dem = tile_data.data[0]
            else :
                print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            
        rgb = _encode_terrain_rgb(dem)
        content = render(rgb, mask=mask, img_format="png", **img_profiles.get("png"))
        
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print("Terrain tile error:", e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        