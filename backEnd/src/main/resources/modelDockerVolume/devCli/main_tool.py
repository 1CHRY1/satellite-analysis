"""
Minimal Flask tool template (no TiTiler, no extra deps) with simple processing.

Purpose:
- Provide an HTTP endpoint returning a lightweight GeoJSON overlay that the
  dynamic analysis page can display (invoke type: "http+geojson").
- Do a simple processing based on the MosaicJSON bounds (no raster libs).

How it works:
- POST /run accepts JSON: { mosaicUrl: string, params?: { mode?, scale?, grid? } }
- The service tries to fetch the MosaicJSON (standard library urllib only) and
  read its "bounds" [minx, miny, maxx, maxy].
- Depending on "mode", it builds simple derived polygons:
  - mode="bbox": the bounding box polygon
  - mode="inset": an inner rectangle scaled by "scale" (0~1, default 0.8)
  - mode="grid": subdivide bounds into N x N grid (N from "grid", default 3)
                  and return the center cell polygon
- Returns JSON FeatureCollection that the frontend adds as a polygon layer.

Only requires: flask, flask-cors (no other third-party packages)
"""

from flask import Flask, jsonify, request
from flask_cors import CORS
import json
from urllib.request import urlopen, Request
from urllib.error import URLError, HTTPError

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})


def _get_params(body: dict) -> dict:
    params = body.get("params") or {}
    if isinstance(params, str):
        try:
            params = json.loads(params)
        except json.JSONDecodeError:
            params = {}
    if not isinstance(params, dict):
        params = {}
    return params

def _fetch_mosaic_bounds(url: str):
    """Try to fetch MosaicJSON and return bounds [minx, miny, maxx, maxy].
    Returns None if unavailable.
    """
    try:
        req = Request(url, headers={"User-Agent": "MinimalTool/1.0"})
        with urlopen(req, timeout=10) as r:
            data = r.read()
            obj = json.loads(data.decode("utf-8", "ignore"))
            bounds = obj.get("bounds")
            if (
                isinstance(bounds, (list, tuple))
                and len(bounds) == 4
                and all(isinstance(x, (int, float)) for x in bounds)
            ):
                return [float(bounds[0]), float(bounds[1]), float(bounds[2]), float(bounds[3])]
    except (URLError, HTTPError, TimeoutError, ValueError):
        pass
    except Exception:
        pass
    return None

def _rect_to_polygon(minx: float, miny: float, maxx: float, maxy: float):
    return {
        "type": "Polygon",
        "coordinates": [[
            [minx, miny],
            [maxx, miny],
            [maxx, maxy],
            [minx, maxy],
            [minx, miny],
        ]],
    }

def _scale_rect(minx: float, miny: float, maxx: float, maxy: float, scale: float):
    cx = (minx + maxx) / 2.0
    cy = (miny + maxy) / 2.0
    w = (maxx - minx) * scale
    h = (maxy - miny) * scale
    return [cx - w / 2.0, cy - h / 2.0, cx + w / 2.0, cy + h / 2.0]

def _grid_center_cell(minx: float, miny: float, maxx: float, maxy: float, n: int):
    if n < 1:
        n = 1
    dx = (maxx - minx) / n
    dy = (maxy - miny) / n
    ix = n // 2
    iy = n // 2
    cminx = minx + ix * dx
    cminy = miny + iy * dy
    cmaxx = cminx + dx
    cmaxy = cminy + dy
    return [cminx, cminy, cmaxx, cmaxy]


@app.route("/run", methods=["POST"])
def run():
    body = request.get_json(force=True, silent=True) or {}
    mosaic_url = body.get("mosaicUrl")
    if not mosaic_url:
        return jsonify({"error": "mosaicUrl is required"}), 400

    params = _get_params(body)
    mode = str(params.get("mode") or "inset").lower().strip()
    scale = params.get("scale")
    try:
        scale = float(scale) if scale is not None else 0.8
    except Exception:
        scale = 0.8
    grid_n = params.get("grid")
    try:
        grid_n = int(grid_n) if grid_n is not None else 3
    except Exception:
        grid_n = 3

    bounds = _fetch_mosaic_bounds(mosaic_url)
    if not bounds:
        bounds = [-0.5, -0.5, 0.5, 0.5]

    minx, miny, maxx, maxy = bounds

    if mode == "bbox":
        geom = _rect_to_polygon(minx, miny, maxx, maxy)
    elif mode == "grid":
        cminx, cminy, cmaxx, cmaxy = _grid_center_cell(minx, miny, maxx, maxy, grid_n)
        geom = _rect_to_polygon(cminx, cminy, cmaxx, cmaxy)
    else:  # inset (default)
        sminx, sminy, smaxx, smaxy = _scale_rect(minx, miny, maxx, maxy, max(0.01, min(1.0, scale)))
        geom = _rect_to_polygon(sminx, sminy, smaxx, smaxy)

    feature = {"type": "Feature", "properties": {"mode": mode}, "geometry": geom}
    fc = {"type": "FeatureCollection", "features": [feature]}
    return jsonify(fc)


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    # Keep consistent with existing tooling expectations.
    app.run(host="0.0.0.0", port=20080, debug=False)
# -*- coding: utf-8 -*-
import json
from urllib.request import urlopen, Request
from urllib.error import URLError
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
# 允许所有跨域请求
CORS(app, resources={r"/*": {"origins": "*"}})

# -----------------------------------------------------------------------------
# 辅助函数：纯标准库实现几何计算
# -----------------------------------------------------------------------------

def create_polygon_feature(minx, miny, maxx, maxy, properties=None):
    """构建标准的 GeoJSON Polygon Feature"""
    # GeoJSON 坐标顺序：(minx, miny) -> (maxx, miny) -> (maxx, maxy) -> (minx, maxy) -> (minx, miny)
    coordinates = [[
        [minx, miny],
        [maxx, miny],
        [maxx, maxy],
        [minx, maxy],
        [minx, miny]
    ]]
    return {
        "type": "Feature",
        "geometry": {
            "type": "Polygon",
            "coordinates": coordinates
        },
        "properties": properties or {}
    }

def get_mosaic_bounds(url):
    """
    使用标准库 urllib 获取 MosaicJSON 的 bounds。
    如果失败，返回 None，触发后续兜底逻辑。
    """
    try:
        print(f"[INFO] Fetching MosaicJSON: {url}")
        # 设置 User-Agent 以防某些 CDN 拦截
        req = Request(url, headers={"User-Agent": "MinimalFlaskTool/1.0"})
        # 10秒超时
        with urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode("utf-8", "ignore"))
            # MosaicJSON 标准字段 bounds: [minx, miny, maxx, maxy]
            return data.get("bounds")
    except Exception as e:
        print(f"[WARN] Failed to fetch bounds: {e}")
        return None

# -----------------------------------------------------------------------------
# HTTP 接口
# -----------------------------------------------------------------------------

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

@app.route('/run', methods=['POST'])
def run():
    # 1. 解析入参，容错处理
    body = request.get_json(force=True, silent=True) or {}
    mosaic_url = body.get("mosaicUrl")
    
    # 2. 参数校验
    if not mosaic_url:
        return jsonify({"error": "mosaicUrl is required"}), 400
    
    # 获取额外参数 params (mode, scale, grid)
    # 动态分析页传来的 params 可能是字符串也可能是对象，这里假设已解析为 dict
    params = body.get("params") or {}
    if isinstance(params, str):
        try:
            params = json.loads(params)
        except:
            params = {}

    mode = params.get("mode", "inset") # 默认 inset
    
    # 3. 获取 Bounds (网络请求)
    bounds = get_mosaic_bounds(mosaic_url)
    is_fallback = False

    # 4. 兜底逻辑：如果无法获取 bounds，使用默认小矩形确保前端可见
    if not bounds:
        bounds = [-0.5, -0.5, 0.5, 0.5]
        is_fallback = True
        print("[INFO] Using fallback bounds [-0.5, -0.5, 0.5, 0.5]")

    minx, miny, maxx, maxy = bounds

    # 5. 根据 Mode 生成几何
    final_minx, final_miny, final_maxx, final_maxy = minx, miny, maxx, maxy
    
    if mode == "bbox":
        # 原样输出外包框
        pass

    elif mode == "grid":
        # 将 bounds 分割为 N*N，取中心格
        try:
            grid_n = int(params.get("grid", 3))
        except:
            grid_n = 3
        
        width = maxx - minx
        height = maxy - miny
        step_x = width / grid_n
        step_y = height / grid_n
        
        # 计算中心格子的索引 (例如 3x3, 中心索引为 1,1)
        center_idx = grid_n // 2 
        
        final_minx = minx + (step_x * center_idx)
        final_miny = miny + (step_y * center_idx)
        final_maxx = final_minx + step_x
        final_maxy = final_miny + step_y

    else: # default: inset
        # 缩放内框
        try:
            scale = float(params.get("scale", 0.8))
        except:
            scale = 0.8
            
        center_x = (minx + maxx) / 2.0
        center_y = (miny + maxy) / 2.0
        width = (maxx - minx) * scale
        height = (maxy - miny) * scale
        
        final_minx = center_x - (width / 2.0)
        final_maxx = center_x + (width / 2.0)
        final_miny = center_y - (height / 2.0)
        final_maxy = center_y + (height / 2.0)

    # 6. 构造 FeatureCollection 返回
    feature = create_polygon_feature(
        final_minx, final_miny, final_maxx, final_maxy,
        properties={
            "source": mosaic_url,
            "mode": mode,
            "is_fallback": is_fallback,
            "original_bounds": bounds
        }
    )

    feature_collection = {
        "type": "FeatureCollection",
        "features": [feature]
    }

    return jsonify(feature_collection)

if __name__ == '__main__':
    # 绑定 0.0.0.0:20080
    app.run(host='0.0.0.0', port=20080)
