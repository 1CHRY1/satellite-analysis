from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
from config import minio_config, TRANSPARENT_CONTENT
from typing import Literal
from pydantic import BaseModel
import json

router = APIRouter()

class StdStretchConfig(BaseModel):
    mean_r: float
    std_r: float
    mean_g: float
    std_g: float
    mean_b: float
    std_b: float

def normalize(arr, min_val=0, max_val=5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")

@router.get("/{z}/{x}/{y}.png")
def get_gif_frame(
    z: int, x: int, y: int,
    url_r: str = Query(..., description="Red band URL"),
    url_g: str = Query(..., description="Green band URL"),
    url_b: str = Query(..., description="Blue band URL"),
    min_r: float = Query(0),
    max_r: float = Query(5000),
    min_g: float = Query(0),
    max_g: float = Query(5000),
    min_b: float = Query(0),
    max_b: float = Query(5000),
    nodata: float = Query(0.0),
    stretch_method: Literal["linear", "gamma", "standard"] = Query('gamma', description="Stretch Method"),
    normalize_level: float = Query(1.0, description="Gamma value or level"),
    std_config: str = Query("{}", description="Standard deviation config JSON"),
    bbox: str = Query(None, description="Bounding box 'minx,miny,maxx,maxy'"),
    resolution: int = Query(256, description="Output resolution (256, 512, or 1024)"),
):
    try:
        # Clamp resolution to reasonable values (256 ~ 2048)
        res = max(256, min(2048, resolution))

        if bbox:
            bbox_val = list(map(float, bbox.split(",")))
            # Read Red band
            with COGReader(url_r, options={"nodata": nodata}) as cog_r:
                tile_r = cog_r.part(bbox_val, width=res, height=res)
                data_r = tile_r.data.squeeze()
                mask = tile_r.mask

            # Read Green band
            with COGReader(url_g, options={"nodata": nodata}) as cog_g:
                data_g = cog_g.part(bbox_val, width=res, height=res).data.squeeze()

            # Read Blue band
            with COGReader(url_b, options={"nodata": nodata}) as cog_b:
                data_b = cog_b.part(bbox_val, width=res, height=res).data.squeeze()
        else:
            # Read Red band
            with COGReader(url_r, options={"nodata": nodata}) as cog_r:
                if not cog_r.tile_exists(x, y, z):
                    return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
                tile_r = cog_r.tile(x, y, z)
                data_r = tile_r.data.squeeze()
                mask = tile_r.mask

            # Read Green band
            with COGReader(url_g, options={"nodata": nodata}) as cog_g:
                data_g = cog_g.tile(x, y, z).data.squeeze()

            # Read Blue band
            with COGReader(url_b, options={"nodata": nodata}) as cog_b:
                data_b = cog_b.tile(x, y, z).data.squeeze()

        # Handle Stretching
        b_min_r, b_max_r = min_r, max_r
        b_min_g, b_max_g = min_g, max_g
        b_min_b, b_max_b = min_b, max_b

        if stretch_method == 'standard':
            try:
                config_dict = json.loads(std_config)
                config = StdStretchConfig(**config_dict)
                n = normalize_level if normalize_level > 0 else 2
                b_min_r, b_max_r = config.mean_r - n * config.std_r, config.mean_r + n * config.std_r
                b_min_g, b_max_g = config.mean_g - n * config.std_g, config.mean_g + n * config.std_g
                b_min_b, b_max_b = config.mean_b - n * config.std_b, config.mean_b + n * config.std_b
            except Exception:
                # Fallback or pass (uses default min/max)
                pass

        # Normalize
        r = normalize(data_r, b_min_r, b_max_r)
        g = normalize(data_g, b_min_g, b_max_g)
        b = normalize(data_b, b_min_b, b_max_b)

        # Gamma Correction
        if stretch_method == 'gamma' and normalize_level is not None and normalize_level > 0:
            gamma = float(normalize_level)
            r = np.clip(((r / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
            g = np.clip(((g / 255.0) ** gamma) * 255, 0, 255).astype("uint8")
            b = np.clip(((b / 255.0) ** gamma) * 255, 0, 255).astype("uint8")

        rgb = np.stack([r, g, b])

        # Render with original mask (0=nodata, 255=valid data)
        # The mask creates proper alpha channel in PNG output
        # Frontend will handle converting transparent pixels to GIF transparent color
        content = render(rgb, mask=mask, img_format="png", **img_profiles.get("png"))
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(f"Error generating GIF frame: {e}")
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
