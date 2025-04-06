from flask import Flask, send_file, abort, request
from rio_tiler.io import Reader
from rio_tiler.colormap import cmap
from flask_cors import CORS
import io, os, argparse
import rasterio
import numpy as np

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

color_style_dict = {
    'gray': 'gray',
    'red2green': 'rdylgn_r',
}

data_range_dict = dict()


@app.route('/<int:z>/<int:x>/<int:y>.png')  
def dynamic_tile(z, x, y):
    
    # object
    url_field = request.args.get('object')
    
    # colorStyle
    have_color_style = False
    image_color_style = color_style_dict['gray']
    cm = cmap.get(image_color_style)
    if request.args.get('colorStyle'):
        image_color_style = color_style_dict[request.args.get('colorStyle')]
        cm = cmap.get(image_color_style)
        have_color_style = True
        
    #rescaleRange
    range = None
    if request.args.get('range'):
        range = request.args.get('range').strip('[]')  # 去掉方括号
        range = range.split(',')
        range = [float(i) for i in range]
        
    minio_raster_url = f"{MINIO_ENDPOINT}{url_field}"
    ndvi_min_val, ndvi_max_val = -1, 1
    try:
        with rasterio.open(minio_raster_url) as src:
            with Reader(None, dataset=src, options={"nodata": 0}) as image:
                img = image.tile(tile_x=x, tile_y=y, tile_z=z)
    
                if range:
                    img.rescale(in_range=((range[0], range[1]),))
                
                if have_color_style:
                    image_bytes = img.render(True, "PNG", colormap=cm)
                else:
                    image_bytes = img.render(True, "PNG")
            
                return send_file(io.BytesIO(image_bytes), mimetype='image/png')
    except Exception as e:
        print(e)
        return send_file(TRANSPARENT_PNG, mimetype='image/png')
        
if __name__ == "__main__":
    app.run(host='0.0.0.0', port=args.port)