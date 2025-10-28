from flask import Flask, jsonify, request
from flask_cors import CORS
from urllib.parse import quote_plus
import json

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

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

    expression = params.get("expression") or "(b3-b5)/(b3+b5)"
    color = params.get("color") or "rdylgn"
    pixel_method = params.get("pixel_method") or "first"

    tile_template = ("/tiler/mosaic/analysis/{z}/{x}/{y}.png?"f"mosaic_url={quote_plus(mosaic_url)}"f"&expression={quote_plus(expression)}"f"&pixel_method={quote_plus(pixel_method)}"f"&color={quote_plus(color)}")
    return jsonify({"tileTemplate": tile_template})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=20080, debug=False)
