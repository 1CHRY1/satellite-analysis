import tempfile
import uuid
from datetime import datetime

from flask import Flask, send_file, request, jsonify, make_response, Blueprint
import os

from dataProcessing.Utils.mySqlUtils import select_tile_by_ids
from dataProcessing.Utils.osUtils import uploadLocalFile
import dataProcessing.config as config
from dataProcessing.Utils.tifUtils import mtif

MINIO_ENDPOINT = f"{config.MINIO_IP}:{config.MINIO_PORT}"

bp = Blueprint('main', __name__)

@bp.route('/tif/<int:id>')
def get_tif(id):

    tif_path = os.path.join(config.TEMP_OUTPUT_DIR, f"{id}.tif")
    if os.path.exists(tif_path):
        return send_file(tif_path, mimetype='image/tiff')
    else:
        return "TIF not found", 404

@bp.route('/png')
def get_png():

    png_path = os.path.join(config.TEMP_OUTPUT_DIR, f"image.png")
    return send_file(png_path, mimetype='image/png')

@bp.route('/geojson')
def get_geojson():

    geojson_path = os.path.join(config.TEMP_OUTPUT_DIR, "grid_polygons.geojson")
    if os.path.exists(geojson_path):
        return send_file(geojson_path, mimetype='application/json')
    else:
        return "GeoJSON not found", 404

@bp.route(config.API_TIF_MERGE, methods=['POST'])
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
        uploadLocalFile(temp_tif_path, config.MINIO_TEMP_FILES_BUCKET, object_name)

    return jsonify({
        "bucket": config.MINIO_TEMP_FILES_BUCKET,
        "path": object_name
    })

@bp.route('/ndvi', methods=['POST'])
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
