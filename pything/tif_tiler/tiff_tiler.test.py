import io, os
from flask import Flask, send_file, abort, request
from flask_cors import CORS
from rio_tiler.io import Reader
from rio_tiler.profiles import img_profiles
import rasterio
import numpy as np

# MinIO Configuration
MINIO_ENDPOINT = "http://223.2.34.7:9000"
MINIO_ACCESS_KEY = "jTbgNHEqQafOpUxVg7Ol"
MINIO_SECRET_KEY = "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
MINIO_IMAGE_BUCKET = "test-images"

# PLACE HOLDER
TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")

app = Flask(__name__)
CORS(app)

# cm = cmap.get("cfastie")

@app.route('/<int:z>/<int:x>/<int:y>.png')  
def dynamic_tile(z, x, y):
    url_field = request.args.get('object')
    minio_raster_url = f"{MINIO_ENDPOINT}{url_field}"
    try:
        with rasterio.open(minio_raster_url) as src:
            # with Reader(None, dataset=src) as image:
            #     img = image.tile(tile_x=x, tile_y=y, tile_z=z)
            #     print(img.data.min(), img.data.max(), img.data.dtype)
            #     # num_array = np.array(img.data)
            #     # print(num_array.min(), num_array.max(), num_array.dtype)
            #     # img.rescale(
            #     #     in_range=(img.data.min(), img.data.max()),
            #     #     out_range=(0, 255),
            #     #     out_dtype="uint8"
            #     # )
            #     image_bytes = img.render(True, "JPEG")
            #     return send_file(io.BytesIO(image_bytes), mimetype='image/jpeg')
            with Reader(None, dataset=src) as cog:
                img = cog.tile(x, y, z)
                num_array = np.array(img.data)
                
                # 获取数据范围
                min_val = img.data.min()
                max_val = img.data.max()
                
                # 使用 rescale 方法进行归一化
                img.rescale(
                    in_range=(min_val, max_val),
                    out_range=(0, 255),
                    out_dtype="uint8"
                )
                
                content = img.render(img_format="PNG", **img_profiles.get("png"))
                return send_file(io.BytesIO(content), mimetype='image/png')
    except Exception as e:
        print(e)
        return send_file(TRANSPARENT_PNG, mimetype='image/png')
        
if __name__ == "__main__":
    app.run(host='0.0.0.0', port=8079)