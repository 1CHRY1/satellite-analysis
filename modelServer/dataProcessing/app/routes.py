from flask import Flask, send_file, request, jsonify, make_response, Blueprint
import os

import dataProcessing.config as config
from dataProcessing.app.resTemplate import api_response
from dataProcessing.model.scheduler import init_scheduler

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


@bp.route(config.API_TASK_STATUS, methods=['GET'])
def get_status():
    scheduler = init_scheduler()
    task_id = request.args.get('id', type=str)
    print(f"当前任务的id是{task_id}")
    status = scheduler.get_status(task_id)
    print(scheduler.task_status)
    print(f"等待中的任务数：{scheduler.pending_queue.qsize()}")
    print(f"正在执行的任务数：{scheduler.running_queue.qsize()}")
    print(f"错误的任务数：{scheduler.error_queue.qsize()}")
    print(f"任务完成的任务数：{scheduler.complete_queue.qsize()}")
    if status == 'ERROR':
        print(f"报错信息：{scheduler.task_results[task_id]}")
    elif status == 'COMPLETE':
        print(f"结果信息：{scheduler.task_results[task_id]}")
    return api_response(data={'status': status})


@bp.route(config.API_TIF_MERGE, methods=['POST'])
def merge_tifs():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('merge_tif', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TIF_MERGE_V2, methods=['POST'])
def merge_tifs_v2():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('merge_tif_v2', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TIF_calc_no_cloud, methods=['POST'])
def calc_no_cloud():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_no_cloud', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TIF_calc_no_cloud_grid, methods=['POST'])
def calc_no_cloud_grid():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_no_cloud_grid', data)
    return api_response(data={'taskId': task_id})


@bp.route(config.API_TIF_get_spectral_profile, methods=['POST'])
def get_spectral_profile():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('get_spectral_profile', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TIF_calc_raster_point, methods=['POST'])
def calc_raster_point():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_raster_point', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TIF_calc_raster_line, methods=['POST'])
def calc_raster_line():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_raster_line', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TIF_calc_NDVI, methods=['POST'])
def calc_NDVI():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_NDVI', data)
    return api_response(data={'taskId': task_id})

@bp.route(config.API_TASK_RESULT, methods=['GET'])
def get_result():
    scheduler = init_scheduler()
    task_id = request.args.get('id', type=str)
    result = scheduler.get_result(task_id)
    return api_response(data={'result': result})
