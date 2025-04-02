from flask import Flask, send_file, abort, request
from rio_tiler.io import Reader
# from rio_tiler.colormap import cmap
from flask_cors import CORS
import io, os, argparse
import rasterio
        
# argparse
parser = argparse.ArgumentParser(description='start realtime COG tiler server')
parser.add_argument('--minio_endpoint', type=str, required=True, help='MinIO endpoint')
parser.add_argument('--minio_access_key', type=str, required=True, help='MinIO access key')
parser.add_argument('--minio_secret_key', type=str, required=True, help='MinIO secret key')
parser.add_argument('--port', type=int, default=8888, help='application port')

args = parser.parse_args()

# MinIO
MINIO_ENDPOINT = f"http://{args.minio_endpoint}"
MINIO_ACCESS_KEY = args.minio_access_key
MINIO_SECRET_KEY = args.minio_secret_key

# PLACE HOLDER
TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")

app = Flask(__name__)
CORS(app)

# cm = cmap.get("cfastie")

@app.route('/<int:z>/<int:x>/<int:y>.png')  
def dynamic_tile(z, x, y):
    
    url_field = request.args.get('object')
    minio_raster_url = f"{MINIO_ENDPOINT}{url_field}"
    print("!!!!!!!")
    print(minio_raster_url)
    try:
        with rasterio.open(minio_raster_url) as src:
            with Reader(None, dataset=src) as image:
                img = image.tile(tile_x=x, tile_y=y, tile_z=z)
                image_bytes = img.render(True, "PNG")
                return send_file(io.BytesIO(image_bytes), mimetype='image/png')
    except Exception as e:
        return send_file(TRANSPARENT_PNG, mimetype='image/png')
        
if __name__ == "__main__":
    app.run(host='0.0.0.0', port=args.port)