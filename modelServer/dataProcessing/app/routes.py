from flask import send_file, request, jsonify, make_response, Blueprint
import os
import requests

from dataProcessing.config import current_config as CONFIG
from dataProcessing.app.resTemplate import api_response
from dataProcessing.model.scheduler import init_scheduler
import ray

# 使用函数获取MINIO_ENDPOINT
def get_minio_endpoint():
    try:
        return f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
    except:
        return "localhost:9000"  # 默认值

MINIO_ENDPOINT = None  # 初始化为None，将在需要时获取

bp = Blueprint('main', __name__)

# -------------------------- 通用函数 --------------------------#
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

# -------------------------- 任务通用函数 --------------------------#
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

@bp.route(CONFIG.API_TASK_RESULT, methods=['GET'])
def get_result():
    scheduler = init_scheduler()
    task_id = request.args.get('id', type=str)
    result = scheduler.get_result(task_id)
    return api_response(data={'result': result})

@bp.route(CONFIG.API_TASK_CANCEL, methods=['GET'])
def cancel_task():
    scheduler = init_scheduler()
    task_id = request.args.get('id', type=str)
    scheduler.cancel_task(task_id)
    scheduler.set_status(task_id, 'ERROR')
    return api_response(data={'status': 'ERROR'})

# -------------------------- 任务路由 --------------------------#
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

@bp.route(CONFIG.API_TIF_calc_no_cloud_complex, methods=['POST'])
def calc_no_cloud_complex():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('calc_no_cloud_complex', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_create_low_level_mosaic, methods=['POST'])
def create_low_level_mosaic():
    # 提取 Headers和Cookies（转为普通字典）
    headers = dict(request.headers)
    cookies = request.cookies.to_dict()
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('create_low_level_mosaic', data, headers=headers, cookies=cookies)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_TIF_create_low_level_mosaic_threads, methods=['POST'])
def create_low_level_mosaic_threads():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('create_low_level_mosaic_threads', data)
    return api_response(data={'taskId': task_id})

@bp.route(CONFIG.API_METHLIB, methods=['POST'])
def do_methlib():
    scheduler = init_scheduler()
    data = request.json
    task_id = scheduler.start_task('methlib', data)
    return api_response(data={'taskId': task_id})


# ==================== 调试路由 ====================
@bp.route('/test/task', methods=['POST'])
def create_test_task():
    """创建测试任务"""
    scheduler = init_scheduler()
    data = request.json or {}
    
    # 设置默认持续时间
    if 'duration' not in data:
        data['duration'] = 5
    
    try:
        task_id = scheduler.start_task('test', data)
        print(f"创建测试任务: {task_id}, 参数: {data}")
        return api_response(
            code=200,
            message="测试任务已创建",
            data={'taskId': task_id}
        )
    except Exception as e:
        print(f"创建测试任务失败: {str(e)}")
        return api_response(
            code=500,
            message=f"创建测试任务失败: {str(e)}",
            data=None
        )

# ==================== 调试路由 ====================
@bp.route('/debug/scheduler', methods=['GET'])
def debug_scheduler():
    """调试路由：查看调度器状态"""
    scheduler = init_scheduler()
    
    # 获取所有队列的大小
    pending_size = scheduler.pending_queue.qsize()
    running_size = scheduler.running_queue.qsize()
    complete_size = scheduler.complete_queue.qsize()
    error_size = scheduler.error_queue.qsize()
    
    # 获取所有任务状态
    task_statuses = {}
    for task_id, status in scheduler.task_status.items():
        task_info = scheduler.task_info.get(task_id)
        task_class = task_info.__class__.__name__ if task_info else "Unknown"
        task_statuses[task_id] = {
            'status': status,
            'task_type': task_class
        }
    
    # 检查调度器线程是否存活
    scheduler_thread_alive = scheduler.scheduler_thread.is_alive() if hasattr(scheduler, 'scheduler_thread') else False
    
    return jsonify({
        'queues': {
            'pending': pending_size,
            'running': running_size,
            'complete': complete_size,
            'error': error_size
        },
        'tasks': task_statuses,
        'scheduler_thread_alive': scheduler_thread_alive,
        'ray_initialized': ray.is_initialized()
    })

# ==================== 全国范围可视化Mosaic数据加载 ====================

@bp.route(CONFIG.API_VERSION + '/mosaic/create', methods=['POST'])
def create_mosaic_with_query_param():
    """
    创建镶嵌任务 - sensor_name作为查询参数
    URL示例: /v0/mosaic/create?sensor_name=GF-1_PMS
    """
    scheduler = init_scheduler()
    data = request.json or {}
    
    # 透传客户端的 Headers 和 Cookies（供后续取场景与鉴权使用）
    # headers = dict(request.headers)
    # cookies = request.cookies.to_dict()
    
    # 从查询参数获取sensor_name
    sensor_name = request.args.get('sensor_name')
    if not sensor_name:
        return api_response(
            code=400,
            message="缺少必要参数: sensor_name (查询参数)",
            data=None
        )
    
    # 添加到数据中
    data['sensor_name'] = sensor_name
    print(f"从查询参数获取sensor_name: {sensor_name}")
    
    # 验证必要参数
    required_fields = ['email', 'password']
    for field in required_fields:
        if field not in data:
            return api_response(
                code=400,
                message=f"缺少必要参数: {field}",
                data=None
            )
    
    # 若未提供 gridsAndGridsBoundary，则按 region_id/grid_res 从后端补齐
    # region_id = data.get('region_id', CONFIG.MOSAIC_DEFAULT_REGION_ID)
    # grid_res = data.get('grid_res', CONFIG.MOSAIC_DEFAULT_GRID_RES)
    # if not data.get('gridsAndGridsBoundary'):
    #     try:
    #         grids_url = f"{CONFIG.BACK_URL_PREFIX}v1/data/grid/grids/region/{region_id}/resolution/{grid_res}"
    #         resp = requests.get(grids_url, headers=headers, cookies=cookies, timeout=30)
    #         resp.raise_for_status()
    #         data['gridsAndGridsBoundary'] = resp.json()
    #         print(f"已从后端补齐格网: region_id={region_id}, grid_res={grid_res}")
    #     except Exception as e:
    #         print(f"获取格网失败: {e}")
    #         return api_response(
    #             code=502,
    #             message=f"获取格网失败: {str(e)}",
    #             data=None
    #         )

    try:
        # 将 headers/cookies 透传给任务，供 SceneFetcher 使用
        task_id = scheduler.start_task('create_low_level_mosaic', data, headers=headers, cookies=cookies)
        print(f"创建镶嵌任务: {task_id}, sensor_name: {sensor_name}, 参数: {data}")
        return api_response(
            code=200,
            message=f"镶嵌任务已启动，传感器: {sensor_name}",
            data={
                'taskId': task_id,
                'sensor_name': sensor_name,
            }
        )
    except Exception as e:
        print(f"创建镶嵌任务失败: {str(e)}")
        return api_response(
            code=500,
            message=f"创建任务失败: {str(e)}",
            data=None
        )

@bp.route(CONFIG.API_VERSION + '/mosaic/create_threads', methods=['POST'])
def create_mosaic_threads_with_query_param():
    """
    创建多线程镶嵌任务 - sensor_name作为查询参数
    URL示例: /v0/mosaic/create_threads?sensor_name=GF-1_PMS
    """
    scheduler = init_scheduler()
    data = request.json or {}

    sensor_name = request.args.get('sensor_name')
    if not sensor_name:
        return api_response(
            code=400,
            message="缺少必要参数: sensor_name (查询参数)",
            data=None
        )

    data['sensor_name'] = sensor_name
    print(f"从查询参数获取sensor_name: {sensor_name}")

    required_fields = ['email', 'password']
    for field in required_fields:
        if field not in data:
            return api_response(
                code=400,
                message=f"缺少必要参数: {field}",
                data=None
            )

    try:
        task_id = scheduler.start_task('create_low_level_mosaic_threads', data)
        print(f"创建多线程镶嵌任务: {task_id}, sensor_name: {sensor_name}, 参数: {data}")
        return api_response(
            code=200,
            message=f"多线程镶嵌任务已启动，传感器: {sensor_name}",
            data={
                'taskId': task_id,
                'sensor_name': sensor_name,
            }
        )
    except Exception as e:
        print(f"创建多线程镶嵌任务失败: {str(e)}")
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
            response_data['sensor_name'] = result.get('sensor_name', 'Unknown')  # 添加sensor_name信息
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
                    'sensor_name': result.get('sensor_name', 'Unknown'),  # 添加sensor_name信息
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