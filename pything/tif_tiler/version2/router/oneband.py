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


def normalize(arr, min_val = 0 , max_val = 5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")






####### Router ########################################################################################

router = APIRouter()

@router.get("/colorband/{z}/{x}/{y}.png")
async def get_tile(
    z: int, x: int, y: int,
    url: str = Query(...),
    min: float = Query(-1.0, description="Minimum value of the color band"),
    max: float = Query(1.0, description="Maximum value of the color band"),
    cmap: str = Query("rdylgn", description="Color map"),
    nodata: float = Query(9999.0, description="No data value"),
):

    try:
        cog_path = url
        
        with COGReader(cog_path, {'nodata': nodata}) as cog:
            if(cog.tile_exists(x, y, z)):
                res = cog.tile(x, y, z)
                img, _ = res
                mask = res.mask
            else :
                print("tile not exist", z, x, y)
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            
            normed = normalize(img.squeeze(), min, max)
            
            content = render(normed, mask=mask, img_format="png", colormap=cmap, **img_profiles.get("png"))
            
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print("color band tile error:", e)
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")