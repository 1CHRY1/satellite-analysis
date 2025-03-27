import io, os
from flask import Flask, send_file, abort
from flask_cors import CORS
from rio_tiler.io import Reader

transparent_png = os.path.join(os.path.dirname(__file__), "transparent.png")
tiff_mapping = {
    "tiff1": "D:\\edgedownload\\LT51190382000261BJC00\\LT51190382000261BJC00_B7.TIF",
    "tiff2": "D:\\myProject\\funnyScripts\\resource\\COG.TIF",
}

app = Flask(__name__)
CORS(app)

@app.route('/<tiff_id>/<int:z>/<int:x>/<int:y>.png')
def get_tile(tiff_id, z, x, y):

    raster_path = tiff_mapping.get(tiff_id)
    if not raster_path:
        abort(404, description=f"TIFF {tiff_id} not found")

    try:
        with Reader(raster_path, options={"nodata": 0}) as image:
            if image.tile_exists(tile_x=x, tile_y=y, tile_z=z):
                img = image.tile(tile_x=x, tile_y=y, tile_z=z)
                image_bytes = img.render(True, "PNG")
                return send_file(io.BytesIO(image_bytes), mimetype='image/png')
            else:
                return send_file(transparent_png, mimetype='image/png')
    except Exception as e:
        abort(404, description=f"Error fetching tile: {e}")
        

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)