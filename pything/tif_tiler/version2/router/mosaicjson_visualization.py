from fastapi import APIRouter, Query, Response
from rio_tiler.io import Reader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
import numpy as np
import requests
from typing import List, Dict, Any
from config import minio_config, TRANSPARENT_CONTENT


router = APIRouter()


def to_http_url(path: str) -> str:
    if path.startswith("minio://"):
        relative = path[len("minio://"):]
        return f"http://{minio_config['endpoint']}/{relative}"
    return path


def tile_to_quadkey(x: int, y: int, z: int) -> str:
    quadkey = ""
    for i in range(z, 0, -1):
        digit = 0
        mask = 1 << (i - 1)
        if (x & mask) != 0:
            digit += 1
        if (y & mask) != 0:
            digit += 2
        quadkey += str(digit)
    return quadkey


def fetch_mosaicjson(path: str) -> Dict[str, Any]:
    url = to_http_url(path)
    r = requests.get(url, timeout=10)
    r.raise_for_status()
    return r.json()


def read_tile_rgb(x: int, y: int, z: int, url: str) -> Dict[str, np.ndarray]:
    url = to_http_url(url)
    nodata = scene.get('noData') or 0
    with Reader(url, options={"nodata": 0}) as reader:
        tile = reader.tile(x, y, z, tilesize=256)
        data = tile.data
        mask = tile.mask.astype(bool)
        if data.shape[0] >= 3:
            r, g, b = data[0], data[1], data[2]
        elif data.shape[0] == 1:
            r = g = b = data[0]
        else:
            r = g = b = np.zeros_like(mask, dtype=np.uint8)

        if r.dtype == np.uint16:
            r = (r / 65535 * 255).astype(np.uint8)
        if g.dtype == np.uint16:
            g = (g / 65535 * 255).astype(np.uint8)
        if b.dtype == np.uint16:
            b = (b / 65535 * 255).astype(np.uint8)

        if r.dtype != np.uint8:
            r = np.clip(r, 0, 255).astype(np.uint8)
        if g.dtype != np.uint8:
            g = np.clip(g, 0, 255).astype(np.uint8)
        if b.dtype != np.uint8:
            b = np.clip(b, 0, 255).astype(np.uint8)
        return {"r": r, "g": g, "b": b, "mask": mask}


@router.get("/{z}/{x}/{y}.png")
def get_tile(
    z: int,
    x: int,
    y: int,
    mosaicPath: str = Query(..., description="mosaicJSON 的 minio:// 或 http:// 路径"),
):
    try:
        mj = fetch_mosaicjson(mosaicPath)
        quadkey_zoom = int(mj.get("quadkey_zoom", mj.get("minzoom", z)))
        if z != quadkey_zoom:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        tiles_map: Dict[str, List[str]] = mj.get("tiles", {})
        qk = tile_to_quadkey(x, y, z)
        urls = tiles_map.get(qk, [])
        if not urls:
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        H = W = 256
        img_r = np.zeros((H, W), dtype=np.uint8)
        img_g = np.zeros((H, W), dtype=np.uint8)
        img_b = np.zeros((H, W), dtype=np.uint8)
        need_fill_mask = np.ones((H, W), dtype=bool)

        for url in urls:
            try:
                tile_rgb = read_tile_rgb(x, y, z, url)
            except Exception:
                continue
            fill_mask = need_fill_mask & tile_rgb["mask"]
            if not np.any(fill_mask):
                continue
            img_r[fill_mask] = tile_rgb["r"][fill_mask]
            img_g[fill_mask] = tile_rgb["g"][fill_mask]
            img_b[fill_mask] = tile_rgb["b"][fill_mask]
            need_fill_mask[fill_mask] = False
            if np.count_nonzero(need_fill_mask) == 0:
                break

        alpha_mask = (~need_fill_mask).astype(np.uint8) * 255
        img = np.stack([img_r, img_g, img_b])
        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
        return Response(content=content, media_type="image/png")
    except Exception:
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")


