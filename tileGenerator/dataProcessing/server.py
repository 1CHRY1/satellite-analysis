import io
import tempfile
import uuid
from flask import Flask, send_file, request, jsonify, make_response
from flask_cors import CORS
import os
from osgeo import gdal
import numpy as np
from io import BytesIO
from datetime import datetime
from dataProcessing.Utils.mySqlUtils import select_tile_by_ids
from dataProcessing.Utils.osUtils import configure_minio_access4gdal, uploadFileToMinio, uploadLocalFile

OUTPUT_DIR = "C:\\Users\\lkshi\\Desktop\\warp\\Output"
MINIO_ENDPOINT = "223.2.34.7:9000"
TEMP_FILES_BUCKET = "temp-files"

################### Hepler Methods ###################################

def validate_inputs(tif_paths):
    crs_list = [gdal.Open(path).GetProjection() for path in tif_paths]
    res_list = [gdal.Open(path).GetGeoTransform()[1] for path in tif_paths]
    if len(set(crs_list)) > 1:
        raise ValueError("坐标系不一致")
    if len(set(res_list)) > 1:
        raise ValueError("分辨率不一致")

def mtif(tif_paths, output_path):
    merge_options = gdal.WarpOptions(
        format="GTiff",
        cutlineDSName=None,
        srcSRS=None,  # 自动识别输入投影
        dstSRS=None,  # 保持输入投影
        width=0,      # 自动计算输出尺寸
        height=0,
        resampleAlg="near",  # 重采样算法（near/bilinear等）
        creationOptions=["COMPRESS=LZW"]
    )
    gdal.Warp(
        output_path,
        tif_paths,
        options=merge_options
    )




################### Http Server ###################################
app = Flask(__name__)
CORS(app)

@app.route('/tif/<int:id>')
def get_tif(id):

    tif_path = os.path.join(OUTPUT_DIR, f"{id}.tif")
    if os.path.exists(tif_path):
        return send_file(tif_path, mimetype='image/tiff')
    else:
        return "TIF not found", 404

@app.route('/png')
def get_png():

    png_path = os.path.join(OUTPUT_DIR, f"image.png")
    return send_file(png_path, mimetype='image/png')

@app.route('/geojson')
def get_geojson():

    geojson_path = os.path.join(OUTPUT_DIR, "grid_polygons.geojson")
    if os.path.exists(geojson_path):
        return send_file(geojson_path, mimetype='application/json')
    else:
        return "GeoJSON not found", 404

@app.route('/merge', methods=['POST'])
def merge_tifs():
    tiles = request.json.get('tiles', [])
    imageId = request.json.get('imageId', "")
    if not tiles:
        return "No IDs provided", 400

    #--------- Get Source Data ------------------------------------
    tile_list = select_tile_by_ids(imageId.lower(), tiles)
    tif_paths = [f"http://{MINIO_ENDPOINT}/{tile['bucket']}/{tile['path']}" for tile in tile_list]

    if not tif_paths:
        return "No valid TIFs found", 404

    #--------- Merge and upload tif -------------------------------
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_tif_path = os.path.join(temp_dir, f"{uuid.uuid4()}.tif")
        mtif(tif_paths, temp_tif_path)
        object_name = f"{datetime.now().strftime('%Y-%m/%d')}/{uuid.uuid4()}.tif"
        uploadLocalFile(temp_tif_path, TEMP_FILES_BUCKET, object_name)

    return jsonify({
        "bucket": TEMP_FILES_BUCKET,
        "path": object_name
    })

@app.route('/ndvi', methods=['POST'])
def get_ndvi():
    req = {
        "sensor_id": "SE33955",
        "scene_list": [
            {
                "time": "2021-02-12 00:00:00",
                "images": {
                    "band1": {
                        "path": "landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B1.TIF",
                        "bucket": "test-images"
                    },
                    "band2": {
                        "path": "landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B2.TIF",
                        "bucket": "test-images"
                    },
                    "band3": {
                        "path": "landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B3.TIF",
                        "bucket": "test-images"
                    },
                }
            }
        ],
        "polygon": "POLYGON((113.938600980999 31.2667309978574, 116.52510946083 31.3023454749594, 116.534361143405 29.3665961877802, 113.998134773514 29.3336247013939, 113.938600980999 31.2667309978574)) | 4326"
    }


######################################################################

if __name__ == '__main__':
    # 配置gdal S3访问MinIO
    configure_minio_access4gdal()
    # 打印gdal日志信息
    gdal.SetConfigOption('CPL_LOG', 'YES')
    app.run(host="0.0.0.0", debug=True, port=5000)
