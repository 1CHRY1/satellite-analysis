from flask import send_file, request, jsonify, make_response, Blueprint
import os
import time

from dataProcessing.config import current_config as CONFIG
from dataProcessing.app.resTemplate import api_response
from dataProcessing.model.scheduler import init_scheduler
from service.ray_optimizer import optimize_user_code

# 使用函数获取MINIO_ENDPOINT
def get_minio_endpoint():
    try:
        return f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
    except:
        return "localhost:9000"  # 默认值

MINIO_ENDPOINT = None  # 初始化为None，将在需要时获取

bp = Blueprint('main', __name__)


@bp.route('/tif/<int:id>')
def get_tif(id):
    tif_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, f"{id}.tif")
    if os.path.exists(tif_path):
        return send_file(tif_path, mimetype='image/tiff')
    else:
        return "TIF not found", 404


@bp.route('/png')
def get_png():
    png_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, f"image.png")
    return send_file(png_path, mimetype='image/png')


@bp.route('/geojson')
def get_geojson():
    geojson_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, "grid_polygons.geojson")
    if os.path.exists(geojson_path):
        return send_file(geojson_path, mimetype='application/json')
    else:
        return "GeoJSON not found", 404


@bp.route(CONFIG.API_TASK_STATUS, methods=['GET'])
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


@bp.route(CONFIG.API_TIF_MERGE, methods=['POST'])
def merge_tifs():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('merge_tif', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_MERGE_V2, methods=['POST'])
def merge_tifs_v2():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('merge_tif_v2', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_calc_no_cloud, methods=['POST'])
def calc_no_cloud():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_no_cloud', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_calc_no_cloud_grid, methods=['POST'])
def calc_no_cloud_grid():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_no_cloud_grid', data)
    return api_response(data={'taskId': task_id})


@bp.route(CONFIG.API_TIF_get_spectral_profile, methods=['POST'])
def get_spectral_profile():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('get_spectral_profile', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_calc_raster_point, methods=['POST'])
def calc_raster_point():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_raster_point', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_calc_raster_line, methods=['POST'])
def calc_raster_line():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_raster_line', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_calc_NDVI, methods=['POST'])
def calc_NDVI():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_NDVI', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TASK_RESULT, methods=['GET'])
def get_result():
    scheduler = init_scheduler()
    task_id = request.args.get('id', type=str)
    result = scheduler.get_result(task_id)
    return api_response(data={'result': result})

@bp.route('/v0/ray/optimize', methods=['POST'])
def ray_optimize():
    """
    Ray优化API端点
    """
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({
                'success': False,
                'error': 'No JSON data provided'
            }), 400

        code = data.get('code')
        user_id = data.get('user_id', 'unknown')
        project_id = data.get('project_id', 'unknown')

        if not code:
            return jsonify({
                'success': False,
                'error': 'No code provided'
            }), 400

        # 代码安全检查
        if not is_code_safe(code):
            return jsonify({
                'success': False,
                'error': 'Code contains potentially unsafe operations'
            }), 400

        # 执行Ray优化
        try:
            optimized_code, report = optimize_user_code(code, user_id, project_id)
            
            return jsonify({
                'success': True,
                'optimized_code': optimized_code,
                'report': report,
                'timestamp': time.time()
            })
            
        except Exception as opt_error:
            bp.logger.error(f"Ray optimization failed for project {project_id}: {str(opt_error)}")
            return jsonify({
                'success': False,
                'error': f'Optimization failed: {str(opt_error)}'
            }), 500

    except Exception as e:
        bp.logger.error(f"Ray optimization API error: {str(e)}")
        return jsonify({
            'success': False,
            'error': f'API error: {str(e)}'
        }), 500


def is_code_safe(code):
    """
    检查代码安全性
    """
    # 危险操作列表
    dangerous_operations = [
        'os.system',
        'subprocess',
        'eval(',
        'exec(',
        '__import__',
        'open(',
        'file(',
        'input(',
        'raw_input(',
        'execfile(',
        'compile(',
        'reload(',
        'import os',
        'import subprocess',
        'import sys',
        'from os import',
        'from subprocess import',
        'from sys import'
    ]
    
    code_lower = code.lower()
    
    for dangerous_op in dangerous_operations:
        if dangerous_op in code_lower:
            bp.logger.warning(f"Potentially unsafe operation detected: {dangerous_op}")
            return False
    
    return True


@bp.route('/v0/ray/status', methods=['GET'])
def ray_status():
    """
    获取Ray集群状态
    """
    try:
        import ray
        
        if not ray.is_initialized():
            return jsonify({
                'initialized': False,
                'message': 'Ray not initialized'
            })
        
        # 获取集群信息
        cluster_resources = ray.cluster_resources()
        available_resources = ray.available_resources()
        
        return jsonify({
            'initialized': True,
            'cluster_resources': cluster_resources,
            'available_resources': available_resources,
            'nodes': len(ray.nodes()),
            'timestamp': time.time()
        })
        
    except Exception as e:
        return jsonify({
            'initialized': False,
            'error': str(e)
        }), 500


@bp.route('/v0/ray/metrics', methods=['GET'])
def ray_metrics():
    """
    获取Ray优化指标
    """
    try:
        # 这里可以从数据库或缓存中获取优化统计信息
        # 暂时返回模拟数据
        metrics = {
            'total_optimizations': 0,
            'successful_optimizations': 0,
            'average_speedup': 0.0,
            'most_common_optimizations': [],
            'error_rate': 0.0,
            'timestamp': time.time()
        }
        
        return jsonify(metrics)
        
    except Exception as e:
        return jsonify({
            'error': str(e)
        }), 500
