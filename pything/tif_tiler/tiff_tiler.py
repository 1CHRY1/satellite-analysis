import io, os
from flask import Flask, send_file, abort
from flask_cors import CORS
from rio_tiler.io import Reader
import rasterio
from rasterio.session import AWSSession
import boto3

# MinIO 配置
MINIO_ENDPOINT = "http://223.2.34.7:9000"
MINIO_ACCESS_KEY = "jTbgNHEqQafOpUxVg7Ol"
MINIO_SECRET_KEY = "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
MINIO_BUCKET = "test-images"

# 设置环境变量
os.environ["AWS_ACCESS_KEY_ID"] = MINIO_ACCESS_KEY
os.environ["AWS_SECRET_ACCESS_KEY"] = MINIO_SECRET_KEY
os.environ["AWS_S3_ENDPOINT"] = MINIO_ENDPOINT
os.environ["AWS_HTTPS"] = "NO"
os.environ["AWS_VIRTUAL_HOSTING"] = "FALSE"

# 创建 S3 Session
session = boto3.Session(
    aws_access_key_id=MINIO_ACCESS_KEY,
    aws_secret_access_key=MINIO_SECRET_KEY
)
aws_session = AWSSession(session, region_name="", endpoint_url=MINIO_ENDPOINT)

# 透明图
TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")

# MinIO URL 模板
MINIO_URL_TEMPLATE = "http://223.2.34.7:9001/api/v1/buckets/test-images/objects/download?prefix={}"

# 本地 TIFF 映射
minio_tiff_mapping = {
    "tiff1": "landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B6.TIF", 
    "tiff2": "landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20240320_20240402_02_T1/LC08_L2SP_118038_20240320_20240402_02_T1_SR_B5.TIF"
}

local_tiff_mapping = {
    "tiff1": "D:\\t\\4\\testtt.TIF",
}


app = Flask(__name__)
CORS(app)

@app.route('/<tiff_id>/<int:z>/<int:x>/<int:y>.png')
def get_tile(tiff_id, z, x, y):

    # minio_raster_url = MINIO_URL_TEMPLATE.format(minio_tiff_mapping.get(tiff_id))
    # local_raster_path = local_tiff_mapping.get(tiff_id)
    # print(minio_raster_url)
    # print(local_raster_path)
    
    minio_raster_url = f"{MINIO_ENDPOINT}/{MINIO_BUCKET}/{minio_tiff_mapping.get(tiff_id)}"
    
    try:
        with rasterio.Env(aws_session):
            with rasterio.open(minio_raster_url) as src:
                with Reader(None, dataset=src) as image:
                    if image.tile_exists(tile_x=x, tile_y=y, tile_z=z):
                        img = image.tile(tile_x=x, tile_y=y, tile_z=z)
                        image_bytes = img.render(True, "PNG")
                        return send_file(io.BytesIO(image_bytes), mimetype='image/png')
                    else:
                        return send_file(TRANSPARENT_PNG, mimetype='image/png')
            
        # with Reader(minio_raster_url, options={"nodata": 0}) as image:
        #     if image.tile_exists(tile_x=x, tile_y=y, tile_z=z):
        #         img = image.tile(tile_x=x, tile_y=y, tile_z=z)
        #         image_bytes = img.render(True, "PNG")
        #         return send_file(io.BytesIO(image_bytes), mimetype='image/png')
        #     else:
        #         return send_file(TRANSPARENT_PNG, mimetype='image/png')
            
    except Exception as e:
        abort(404, description=f"Error fetching tile: {e}")
        

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)