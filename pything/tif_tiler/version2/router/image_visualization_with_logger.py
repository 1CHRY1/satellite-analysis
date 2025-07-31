import logging
import threading
import time
from contextvars import ContextVar
from fastapi import APIRouter, Query, Response, Request
# ... 其他导入

# 创建一个上下文变量来存储线程标识
thread_context: ContextVar[str] = ContextVar('thread_context', default='')

class ThreadFormatter(logging.Formatter):
    """自定义格式化器，包含线程信息"""
    def format(self, record):
        # 获取线程ID和名称
        thread_id = threading.current_thread().ident
        thread_name = threading.current_thread().name
        
        # 从上下文变量获取请求标识
        request_id = thread_context.get('')
        
        # 添加线程信息到日志记录
        record.thread_info = f"[Thread-{thread_id}:{thread_name}]"
        if request_id:
            record.thread_info += f"[{request_id}]"
            
        return super().format(record)

# 配置日志
def setup_logging():
    # 创建logger
    logger = logging.getLogger('tile_service')
    logger.setLevel(logging.INFO)
    
    # 创建控制台处理器
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    
    # 创建格式化器
    formatter = ThreadFormatter(
        '%(asctime)s %(thread_info)s %(levelname)s: %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    console_handler.setFormatter(formatter)
    
    # 添加处理器到logger
    if not logger.handlers:
        logger.addHandler(console_handler)
    
    return logger

# 初始化logger
logger = setup_logging()

####### Helper Functions ########################################################################################

def normalize(arr, min_val=0, max_val=5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")

def convert_to_uint8(data, original_dtype):
    if original_dtype == np.uint8:
        return data.astype(np.uint8)
    elif original_dtype == np.uint16:
        return (data / 65535.0 * 255.0).astype(np.uint8)
    else:
        return np.uint8(np.floor(data.clip(0, 255)))

def calc_tile_bounds(x, y, z):
    Z2 = math.pow(2, z)
    ul_lon_deg = x / Z2 * 360.0 - 180.0
    ul_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y / Z2)))
    ul_lat_deg = math.degrees(ul_lat_rad)
    
    lr_lon_deg = (x + 1) / Z2 * 360.0 - 180.0
    lr_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / Z2)))
    lr_lat_deg = math.degrees(lr_lat_rad)
    
    return {
        "west": ul_lon_deg,
        "east": lr_lon_deg,
        "south": lr_lat_deg,
        "north": ul_lat_deg,
        "bbox": [ul_lon_deg, lr_lat_deg, lr_lon_deg, ul_lat_deg]
    }

def read_band(x, y, z, bucket_path, band_path, nodata_int):
    full_path = MINIO_ENDPOINT + "/" + bucket_path + "/" + band_path
    
    try:
        with COGReader(full_path, options={'nodata': int(nodata_int)}) as reader:
            band_data = reader.tile(x, y, z, tilesize=256)
            original_data = band_data.data[0]
            original_dtype = original_data.dtype
            converted_data = convert_to_uint8(original_data, original_dtype)
            return converted_data
    except Exception as e:
        logger.error(f"无法读取文件 {full_path}: {str(e)}")
        return None

####### Router ########################################################################################

router = APIRouter()

@router.get("/{z}/{x}/{y}.png")
def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    start_time = time.time()
    
    # 设置请求上下文
    request_id = f"tile-{z}/{x}/{y}"
    thread_context.set(request_id)
    
    logger.info(f"开始处理瓦片请求")
    
    try:
        t1 = time.time()
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']
        logger.info(f"计算 tile 边界耗时: {time.time() - t1:.3f} 秒")
        
        # Spring Boot 接口调用
        t2 = time.time()
        url = common_config['create_no_cloud_config_url']
        data = {"sensorName": sensorName, "points": points}
        
        headers = {}
        authorization = request.headers.get('Authorization')
        if authorization:
            headers['Authorization'] = authorization
        cookie = request.headers.get('Cookie')
        if cookie:
            headers['Cookie'] = cookie
            
        json_response = requests.post(url, json=data, headers=headers).json()
        logger.info(f"SpringBoot 请求耗时: {time.time() - t2:.3f} 秒")
        
        if not json_response:
            logger.warning("后端返回的json中为空")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        t3 = time.time()
        mapper = json_response.get('data', {}).get('bandMapper', {})
        json_data = json_response.get('data', {}).get('scenesConfig', [])
        
        if not json_data:
            logger.warning("后端返回的json中的sceneConfig为空")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        # 景与瓦片相交判断
        min_lon_scene = min([float(scene['bbox']['geometry']['coordinates'][0][0][0]) for scene in json_data])
        max_lat_scene = max([float(scene['bbox']['geometry']['coordinates'][0][0][1]) for scene in json_data])
        max_lon_scene = max([float(scene['bbox']['geometry']['coordinates'][0][2][0]) for scene in json_data])
        min_lat_scene = min([float(scene['bbox']['geometry']['coordinates'][0][2][1]) for scene in json_data])
        
        min_lon_tile, min_lat_tile, max_lon_tile, max_lat_tile = points
        
        if (min_lon_scene > max_lon_tile or min_lat_scene > max_lat_tile or 
            max_lon_scene < min_lon_tile or max_lat_scene < min_lat_tile):
            logger.info("景与瓦片不相交，返回空内容")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        # 场景处理逻辑
        full_coverage_scenes = [scene for scene in json_data if float(scene.get('coverage', 0)) >= 0.999]
        sorted_scene = sorted(json_data, key=lambda x: int(x.get('cloud', 0)))
        
        if full_coverage_scenes:
            logger.info(f"找到 {len(full_coverage_scenes)} 个全覆盖景")
            scenes_to_process = [sorted_scene[0]]
        else:
            logger.info("没有找到全覆盖景，将使用前5个覆盖率最高的scene")
            scenes_to_process = sorted_scene[:5]
        
        logger.info(f"将处理 {len(scenes_to_process)} 个景")
        logger.info(f"处理 bandMapper 耗时: {time.time() - t3:.3f} 秒")
        
        # 图像处理部分...
        t4 = time.time()
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
        total_cloud_mask = np.zeros((target_H, target_W), dtype=bool)
        
        logger.info(f"初始化图像数组耗时: {time.time() - t4:.3f} 秒")
        
        processed_scenes = 0
        filled_ratio = 0.0
        
        # 处理每个场景...
        for i, scene in enumerate(scenes_to_process):
            scene_start = time.time()
            logger.info(f"开始处理场景 {i+1}/{len(scenes_to_process)}: {scene.get('sceneId')}")
            
            # ... 场景处理逻辑 ...
            
            logger.info(f"场景 {scene.get('sceneId')} 处理完成，耗时: {time.time() - scene_start:.3f} 秒")
            
            if not np.any(need_fill_mask):
                logger.info(f"瓦片已填满，共处理 {processed_scenes} 个场景")
                break
        
        # 最终渲染
        t6 = time.time()
        img = np.stack([img_r, img_g, img_b])
        transparent_mask = need_fill_mask | total_cloud_mask
        alpha_mask = (~transparent_mask).astype(np.uint8) * 255
        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
        
        logger.info(f"图像渲染耗时: {time.time() - t6:.3f} 秒")
        logger.info(f"请求完成 - 总耗时: {time.time() - start_time:.3f} 秒, "
                   f"填充率: {filled_ratio:.2%}, 云覆盖率: {np.count_nonzero(total_cloud_mask) / total_cloud_mask.size:.2%}")
        
        return Response(content=content, media_type="image/png")
        
    except Exception as e:
        logger.error(f"处理请求时发生错误: {type(e).__name__}: {str(e)}")
        import traceback
        logger.error(f"错误堆栈:\n{traceback.format_exc()}")
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")