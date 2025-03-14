from flask import Flask, send_file, request, jsonify, make_response
from flask_cors import CORS
import os
from osgeo import gdal
import numpy as np
from io import BytesIO

OUTPUT_DIR = "D:\\myProject\\2025\\satellite-analysis\\tileGenerator\\dataProcessing\\output"



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
    
    id_list = request.json.get('id_list', [])
    if not id_list:
        return "No IDs provided", 400

    tif_paths = [os.path.join(OUTPUT_DIR, f"{id}.tif") for id in id_list]
    tif_paths = [path for path in tif_paths if os.path.exists(path)]

    if not tif_paths:
        return "No valid TIFs found", 404
    
    out_path = os.path.join(OUTPUT_DIR, "tempMerge.tif")
    mtif(tif_paths, out_path)

    return send_file(out_path, mimetype='image/tiff', as_attachment=True, download_name='merged.tif')


######################################################################

if __name__ == '__main__':
    app.run(debug=True, port=5000)