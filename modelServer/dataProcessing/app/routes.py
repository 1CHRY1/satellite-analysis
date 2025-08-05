from flask import send_file, request, jsonify, make_response, Blueprint
import os

from dataProcessing.config import current_config as CONFIG
from dataProcessing.app.resTemplate import api_response
from dataProcessing.model.scheduler import init_scheduler

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

@bp.route(CONFIG.API_TIF_calc_no_cloud_complex, methods=['POST'])
def calc_no_cloud_complex():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_no_cloud_complex', data)
    return api_response(data={'taskId': task_id})


# ==================== 新增的镶嵌任务路由 ====================

@bp.route(CONFIG.API_VERSION + '/mosaic/create', methods=['POST'])
def create_mosaic():
    """
    创建镶嵌任务
    """
    scheduler = init_scheduler()
    data = request.json or {}
    
    # 验证必要参数
    required_fields = ['email', 'password']
    for field in required_fields:
        if field not in data:
            return api_response(
                code=400,
                message=f"缺少必要参数: {field}",
                data=None
            )
    
    try:
        task_id = scheduler.start_task('low_level_mosaic', data)
        print(f"创建镶嵌任务: {task_id}, 参数: {data}")
        return api_response(
            code=200,
            message="镶嵌任务已启动，请使用taskId查询任务状态",
            data={
                'taskId': task_id,
                'message': '镶嵌任务已启动，请使用taskId查询任务状态'
            }
        )
    except Exception as e:
        print(f"创建镶嵌任务失败: {str(e)}")
        return api_response(
            code=500,
            message=f"创建任务失败: {str(e)}",
            data=None
        )

@bp.route(CONFIG.API_VERSION + '/mosaic/status/<task_id>', methods=['GET'])
def get_mosaic_status(task_id):
    """
    获取镶嵌任务的详细状态信息
    """
    scheduler = init_scheduler()
    status = scheduler.get_status(task_id)
    
    print(f"查询镶嵌任务状态: {task_id}, 状态: {status}")
    
    if status == 'NONE':
        return api_response(
            code=404,
            message="任务不存在",
            data=None
        )
    
    response_data = {
        'taskId': task_id,
        'status': status,
        'pending_count': scheduler.pending_queue.qsize(),
        'running_count': scheduler.running_queue.qsize(),
        'error_count': scheduler.error_queue.qsize(),
        'complete_count': scheduler.complete_queue.qsize()
    }
    
    if status == 'ERROR':
        error_msg = scheduler.task_results.get(task_id, 'Unknown error')
        response_data['error'] = error_msg
        print(f"镶嵌任务 {task_id} 出错: {error_msg}")
        return api_response(
            code=200,
            message="任务执行失败",
            data=response_data
        )
    elif status == 'COMPLETE':
        result = scheduler.task_results.get(task_id, {})
        response_data['result'] = result
        
        # 如果任务成功完成，返回MosaicJSON信息
        if isinstance(result, dict) and result.get('success'):
            response_data['mosaicjson_path'] = result.get('mosaicjson_path')
            response_data['mosaicjson_url'] = result.get('mosaicjson_url')
            response_data['bounds'] = result.get('bounds')
            response_data['cog_count'] = result.get('cog_count')
            response_data['tile_count'] = result.get('tile_count')
            response_data['processing_time'] = result.get('processing_time')
            print(f"镶嵌任务 {task_id} 完成: {result.get('message')}")
            return api_response(
                code=200,
                message="任务执行成功",
                data=response_data
            )
        else:
            print(f"镶嵌任务 {task_id} 完成但结果异常: {result}")
            return api_response(
                code=200,
                message="任务完成但结果异常",
                data=response_data
            )
    else:
        # PENDING 或 RUNNING 状态
        return api_response(
            code=200,
            message=f"任务状态: {status}",
            data=response_data
        )

@bp.route(CONFIG.API_VERSION + '/mosaic/result/<task_id>', methods=['GET'])
def get_mosaic_result(task_id):
    """
    获取镶嵌任务的结果
    """
    scheduler = init_scheduler()
    status = scheduler.get_status(task_id)
    
    print(f"获取镶嵌任务结果: {task_id}, 状态: {status}")
    
    if status == 'NONE':
        return api_response(
            code=404,
            message="任务不存在",
            data=None
        )
    
    if status != 'COMPLETE':
        return api_response(
            code=400,
            message=f"任务尚未完成，当前状态: {status}",
            data={"status": status}
        )
    
    result = scheduler.get_result(task_id)
    
    if isinstance(result, dict) and result.get('success'):
        print(f"镶嵌任务 {task_id} 结果获取成功")
        return api_response(
            code=200,
            message="获取任务结果成功",
            data=result
        )
    else:
        print(f"镶嵌任务 {task_id} 执行失败: {result}")
        return api_response(
            code=500,
            message="任务执行失败",
            data={"error": result if isinstance(result, str) else str(result)}
        )

@bp.route(CONFIG.API_VERSION + '/mosaic/list', methods=['GET'])
def list_mosaic_tasks():
    """
    列出所有镶嵌任务的状态
    """
    scheduler = init_scheduler()
    
    # 获取所有镶嵌相关的任务
    mosaic_tasks = []
    for task_id, task_instance in scheduler.task_info.items():
        if hasattr(task_instance, '__class__') and 'mosaic' in task_instance.__class__.__name__.lower():
            status = scheduler.get_status(task_id)
            result = scheduler.task_results.get(task_id, {})
            
            task_info = {
                'taskId': task_id,
                'status': status,
                'type': task_instance.__class__.__name__
            }
            
            if status == 'COMPLETE' and isinstance(result, dict):
                task_info.update({
                    'success': result.get('success', False),
                    'mosaicjson_path': result.get('mosaicjson_path'),
                    'cog_count': result.get('cog_count', 0),
                    'processing_time': result.get('processing_time', 0)
                })
            elif status == 'ERROR':
                task_info['error'] = result if isinstance(result, str) else str(result)
            
            mosaic_tasks.append(task_info)
    
    print(f"查询所有镶嵌任务: 找到 {len(mosaic_tasks)} 个任务")
    
    return api_response(
        code=200,
        message="获取镶嵌任务列表成功",
        data={
            'tasks': mosaic_tasks,
            'total': len(mosaic_tasks)
        }
    )