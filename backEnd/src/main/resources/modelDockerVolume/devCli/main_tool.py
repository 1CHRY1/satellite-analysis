"""
Flask-based dynamic tile tool (no Titiler).

It renders analysis tiles directly from a MosaicJSON using rio-tiler.

Endpoints:
- POST /run -> returns {"tileTemplate": "<this-service>/tile/{z}/{x}/{y}.png?..."}
- GET  /tile/{z}/{x}/{y}.png -> serves PNG tiles

Required pip packages (install via 依赖管理):
- rasterio, numpy, mercantile, requests
- rio-tiler>=5, rio-tiler-mosaic, cogeo-mosaic
"""

from flask import Flask, jsonify, request, Response as FlaskResponse
from flask_cors import CORS
from urllib.parse import quote_plus
import json
import requests
import numpy as np

import mercantile
from rio_tiler.io import COGReader
from rio_tiler_mosaic.mosaic import mosaic_tiler
from rio_tiler_mosaic.methods import defaults
from rio_tiler.colormap import cmap
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})


def tiler(src_path: str, *args, nodata=None, **kwargs):
    with COGReader(src_path, options={"nodata": nodata}) as cog:
        return cog.tile(*args, **kwargs)


def normalize(arr, min_val=-1.0, max_val=1.0):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")


def fetch_mosaic_definition(mosaic_url: str):
    resp = requests.get(mosaic_url, timeout=10)
    resp.raise_for_status()
    return resp.json()


@app.route("/run", methods=["POST"])
def run():
    body = request.get_json(force=True, silent=True) or {}
    mosaic_url = body.get("mosaicUrl")
    params = body.get("params") or {}

    if isinstance(params, str):
        try:
            params = json.loads(params)
        except json.JSONDecodeError:
            params = {}

    if not isinstance(params, dict):
        params = {}

    if not mosaic_url:
        return jsonify({"error": "mosaicUrl is required"}), 400

    expression = params.get("expression") or "2*b2-b1-b3"
    color = params.get("color") or "rdylgn"
    pixel_method = params.get("pixel_method") or "first"

    # 生成绝对 URL，确保前端能直接请求此服务自身的瓦片接口
    base = request.url_root.rstrip("/")
    tile_template = (
        f"{base}/tile/{{z}}/{{x}}/{{y}}.png?"
        f"mosaic_url={quote_plus(mosaic_url)}"
        f"&expression={quote_plus(expression)}"
        f"&pixel_method={quote_plus(pixel_method)}"
        f"&color={quote_plus(color)}"
    )
    return jsonify({"tileTemplate": tile_template})


@app.get("/tile/<int:z>/<int:x>/<int:y>.png")
def tile(z: int, x: int, y: int):
    try:
        mosaic_url = request.args.get("mosaic_url")
        expression = request.args.get("expression", type=str)
        pixel_method = request.args.get("pixel_method", default="first", type=str)
        color = request.args.get("color", default="rdylgn", type=str)

        if not mosaic_url or not expression:
            return FlaskResponse("Missing required params", status=400)

        def reader(x, y, z, **kwargs):
            with COGReader(mosaic_url) as src:
                return src.tile(x, y, z, **kwargs)

        tiler_func = mosaic_tiler(reader, allowed_methods=[
            defaults.FirstMethod(),
            defaults.LastMethod(),
            defaults.MeanMethod(),
            defaults.MinMethod(),
            defaults.MaxMethod(),
        ])

        img, mask = tiler_func(z=z, x=x, y=y)
        arr = img[0]
        out = normalize(arr, -1.0, 1.0)
        if color in cmap.list():
            colormap = cmap.get(color)
            data = render(out, mask=mask, colormap=colormap)
            profile = img_profiles.get("png")
            return FlaskResponse(data, mimetype=profile.get("content_type", "image/png"))
        else:
            data = render(out, mask=mask)
            profile = img_profiles.get("png")
            return FlaskResponse(data, mimetype=profile.get("content_type", "image/png"))
    except Exception as e:
        return FlaskResponse(str(e), status=500)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=20080, debug=False)

