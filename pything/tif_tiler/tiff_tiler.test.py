import io, os
from flask import Flask, send_file, abort, request
from flask_cors import CORS
from rio_tiler.io import Reader
import rasterio

# MinIO Configuration
MINIO_ENDPOINT = "http://223.2.34.7:9000"
MINIO_ACCESS_KEY = "jTbgNHEqQafOpUxVg7Ol"
MINIO_SECRET_KEY = "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
MINIO_IMAGE_BUCKET = "test-images"
# PLACE HOLDER
TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")

app = Flask(__name__)
CORS(app)

@app.route('/<int:z>/<int:x>/<int:y>.png')  
def dynamic_tile(z, x, y):
    
    url_field = request.args.get('object')
    minio_raster_url = f"{MINIO_ENDPOINT}/{MINIO_IMAGE_BUCKET}/{url_field}"
    try:
        with rasterio.open(minio_raster_url) as src:
            with Reader(None, dataset=src) as image:
                if image.tile_exists(tile_x=x, tile_y=y, tile_z=z):
                    img = image.tile(tile_x=x, tile_y=y, tile_z=z)
                    image_bytes = img.render(True, "PNG")
                    return send_file(io.BytesIO(image_bytes), mimetype='image/png')
                else:
                    return send_file(TRANSPARENT_PNG, mimetype='image/png')

    except Exception as e:
        abort(404, description=f"Error fetching tile: {e}")
        

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=8079)