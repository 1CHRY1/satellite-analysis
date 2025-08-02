from fastapi import APIRouter, Query, Response, Request
from rio_tiler.io import Reader
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles
from rio_tiler.mosaic.reader import mosaic_reader
from rio_tiler.mosaic.methods import defaults
from rio_tiler.models import ImageData
import numpy as np
import requests
import time
import threading
import logging
from contextvars import ContextVar
from concurrent.futures import ThreadPoolExecutor
from typing import List, Dict, Any, Tuple, Optional, Callable
from dataclasses import dataclass
from functools import lru_cache
from urllib.parse import quote
import rasterio
from config import minio_config, common_config, TRANSPARENT_CONTENT

# 创建一个上下文变量来存储线程标识
thread_context: ContextVar[str] = ContextVar('thread_context', default='')

class ThreadFormatter(logging.Formatter):
    """自定义格式化器，包含线程信息"""
    def format(self, record):
        thread_id = threading.current_thread().ident
        thread_name = threading.current_thread().name
        request_id = thread_context.get('')
        
        record.thread_info = f"[Thread-{thread_id}:{thread_name}]"
        if request_id:
            record.thread_info += f"[{request_id}]"
            
        return super().format(record)

# 配置日志
def setup_logging():
    logger = logging.getLogger('tile_service')
    logger.setLevel(logging.INFO)
    
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    
    formatter = ThreadFormatter(
        '%(asctime)s %(thread_info)s %(levelname)s: %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    console_handler.setFormatter(formatter)
    
    if not logger.handlers:
        logger.addHandler(console_handler)
    
    return logger

logger = setup_logging()

####### 数据结构定义 ########################################################################################

@dataclass
class SceneInfo:
    """场景信息数据类"""
    scene_id: str
    sensor_name: str
    bucket: str
    paths: Dict[str, str]
    cloud_path: Optional[str]
    nodata: Optional[str]
    coverage: float
    cloud: float
    resolution: str

@dataclass 
class BandPaths:
    """波段路径信息"""
    red: str
    green: str  
    blue: str

####### 配置和常量 ########################################################################################

MINIO_ENDPOINT = "http://" + minio_config['endpoint']
BAND_MAPPING = {
    'ZY1_AHSI': {'Red': '4', 'Green': '3', 'Blue': '2'},
    'default': {'Red': 'Red', 'Green': 'Green', 'Blue': 'Blue'}
}

####### 核心工具函数 ########################################################################################

def convert_to_uint8(data, original_dtype):
    """数据类型转换优化"""
    if original_dtype == np.uint8:
        return data.astype(np.uint8)
    elif original_dtype == np.uint16:
        return (data / 65535.0 * 255.0).astype(np.uint8)
    else:
        return np.uint8(np.floor(data.clip(0, 255)))

@lru_cache(maxsize=1000)
def calc_tile_bounds(x: int, y: int, z: int) -> Dict[str, Any]:
    """缓存瓦片边界计算"""
    import math
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

def parse_scenes_config(json_response: Dict[str, Any]) -> Tuple[List[SceneInfo], Dict[str, str]]:
    """解析场景配置数据"""
    mapper = json_response.get('data', {}).get('bandMapper', {})
    scenes_data = json_response.get('data', {}).get('scenesConfig', [])
    
    scenes = []
    for scene_data in scenes_data:
        try:
            scene = SceneInfo(
                scene_id=scene_data.get('sceneId', ''),
                sensor_name=scene_data.get('sensorName', ''),
                bucket=scene_data.get('bucket', ''),
                paths=scene_data.get('path', {}),
                cloud_path=scene_data.get('cloudPath'),
                nodata=scene_data.get('noData'),
                coverage=float(scene_data.get('coverage', 0)),
                cloud=float(scene_data.get('cloud', 0)),
                resolution=scene_data.get('resolution', '')
            )
            scenes.append(scene)
        except (ValueError, TypeError) as e:
            logger.warning(f"解析场景数据失败: {e}")
            continue
    
    return scenes, mapper

def get_band_paths(scene: SceneInfo, mapper: Dict[str, str]) -> Optional[BandPaths]:
    """获取RGB波段路径"""
    try:
        if scene.sensor_name == 'ZY1_AHSI':
            default_mapping = BAND_MAPPING['ZY1_AHSI']
            red_path = scene.paths.get(f"band_{default_mapping['Red']}")
            green_path = scene.paths.get(f"band_{default_mapping['Green']}")  
            blue_path = scene.paths.get(f"band_{default_mapping['Blue']}")
            
            if not all([red_path, green_path, blue_path]):
                available_bands = sorted([k for k in scene.paths.keys() if k.startswith('band_')])
                if available_bands:
                    fallback_path = scene.paths[available_bands[0]]
                    red_path = red_path or fallback_path
                    green_path = green_path or fallback_path
                    blue_path = blue_path or fallback_path
        else:
            red_path = scene.paths.get(f"band_{mapper.get('Red', 'Red')}")
            green_path = scene.paths.get(f"band_{mapper.get('Green', 'Green')}")
            blue_path = scene.paths.get(f"band_{mapper.get('Blue', 'Blue')}")
            
            if not red_path and 'band_1' in scene.paths:
                red_path = scene.paths['band_1']
            if not green_path and 'band_2' in scene.paths:
                green_path = scene.paths['band_2']
            if not blue_path and 'band_3' in scene.paths:
                blue_path = scene.paths['band_3']
        
        if all([red_path, green_path, blue_path]):
            return BandPaths(red=red_path, green=green_path, blue=blue_path)
        else:
            logger.warning(f"场景 {scene.scene_id} 缺少必要的RGB波段路径")
            return None
            
    except Exception as e:
        logger.error(f"获取波段路径失败: {e}")
        return None

####### 云掩膜处理 ########################################################################################

def create_cloud_mask(cloud_data: np.ndarray, sensor_name: str) -> np.ndarray:
    """创建云掩膜"""
    target_h, target_w = 256, 256
    
    if "Landsat" in sensor_name or "Landset" in sensor_name:
        return (cloud_data & (1 << 3)) > 0
    elif "MODIS" in sensor_name:
        cloud_state = (cloud_data & 0b11)
        return (cloud_state == 0) | (cloud_state == 1)
    elif "GF" in sensor_name:    
        return (cloud_data == 2)
    else:
        return np.zeros((target_h, target_w), dtype=bool)

def read_cloud_mask(scene: SceneInfo, x: int, y: int, z: int) -> Optional[np.ndarray]:
    """读取云掩膜数据"""
    if not scene.cloud_path:
        return None
        
    try:
        nodata_int = 0
        if scene.nodata is not None:
            try:
                nodata_int = int(float(scene.nodata))
            except (ValueError, TypeError):
                pass
        
        cloud_path = f"{MINIO_ENDPOINT}/{scene.bucket}/{quote(scene.cloud_path, safe='/')}"
        with Reader(cloud_path, options={'nodata': nodata_int}) as reader:
            cloud_data = reader.tile(x, y, z, tilesize=256)
            return create_cloud_mask(cloud_data.data[0], scene.sensor_name)
    except Exception as e:
        logger.error(f"读取云掩膜失败 {scene.cloud_path}: {e}")
        return None

####### 自定义 Mosaic 方法 ########################################################################################

class CloudAwareMosaicMethod(defaults.FirstMethod):
    """云感知的镶嵌方法 - 优先选择无云像素"""
    
    def __init__(self, cloud_masks: Optional[Dict[str, np.ndarray]] = None):
        super().__init__()
        self.cloud_masks = cloud_masks or {}
    
    def feed(self, tile: ImageData) -> None:
        """重写 feed 方法，考虑云掩膜"""
        if hasattr(tile, 'cutline_mask') and tile.cutline_mask is not None:
            # 如果有对应的云掩膜，将有云区域标记为无效
            asset_id = getattr(tile, 'asset_id', None)
            if asset_id and asset_id in self.cloud_masks:
                cloud_mask = self.cloud_masks[asset_id]
                # 将云掩膜应用到 cutline_mask（255 表示有效像素）
                # 有云的地方设为0（无效），无云的地方保持原值
                combined_mask = tile.cutline_mask.copy()
                combined_mask[cloud_mask] = 0  # 有云区域设为无效
                
                # 创建新的ImageData对象，因为cutline_mask可能是只读的
                tile = ImageData(
                    array=tile.array,
                    cutline_mask=combined_mask,
                    crs=tile.crs
                )
        
        super().feed(tile)

####### Rio-tiler Mosaic 优化实现 ########################################################################################

# 全局存储，用于在mosaic过程中传递场景信息
_GLOBAL_SCENES_CACHE = {}
_GLOBAL_CLOUD_MASKS = {}

def create_mosaic_asset_reader(scenes: List[SceneInfo], band_paths_dict: Dict[str, BandPaths], 
                              mapper: Dict[str, str], cloud_masks: Dict[str, np.ndarray]):
    """创建用于rio_tiler.mosaic的资产读取器"""
    
    # 将场景信息存储到全局缓存
    global _GLOBAL_SCENES_CACHE, _GLOBAL_CLOUD_MASKS
    _GLOBAL_SCENES_CACHE.clear()
    _GLOBAL_CLOUD_MASKS.clear()
    
    for scene in scenes:
        _GLOBAL_SCENES_CACHE[scene.scene_id] = {
            'scene': scene,
            'band_paths': band_paths_dict.get(scene.scene_id)
        }
    
    _GLOBAL_CLOUD_MASKS.update(cloud_masks)
    
    def mosaic_reader_func(src_path: str, x: int, y: int, z: int, **kwargs) -> ImageData:
        """Mosaic读取函数 - 兼容rio_tiler.mosaic接口"""
        try:
            # src_path 是场景ID
            scene_id = src_path
            
            if scene_id not in _GLOBAL_SCENES_CACHE:
                logger.warning(f"场景 {scene_id} 不在缓存中")
                return None
                
            scene_info = _GLOBAL_SCENES_CACHE[scene_id]
            scene = scene_info['scene']
            band_paths = scene_info['band_paths']
            
            if not band_paths:
                logger.warning(f"场景 {scene_id} 没有有效的波段路径")
                return None
            
            # 处理 nodata 值
            nodata_int = 0
            if scene.nodata is not None:
                try:
                    nodata_int = int(float(scene.nodata))
                except (ValueError, TypeError):
                    nodata_int = 0
            
            logger.debug(f"场景 {scene_id} 使用 nodata 值: {nodata_int}")
            
            # 构建完整路径并进行URL编码
            red_path = f"{MINIO_ENDPOINT}/{scene.bucket}/{quote(band_paths.red, safe='/')}"
            green_path = f"{MINIO_ENDPOINT}/{scene.bucket}/{quote(band_paths.green, safe='/')}"
            blue_path = f"{MINIO_ENDPOINT}/{scene.bucket}/{quote(band_paths.blue, safe='/')}"
            
            # 并行读取三个波段
            with ThreadPoolExecutor(max_workers=3) as executor:
                futures = {
                    'red': executor.submit(read_single_band, red_path, x, y, z, nodata_int),
                    'green': executor.submit(read_single_band, green_path, x, y, z, nodata_int),
                    'blue': executor.submit(read_single_band, blue_path, x, y, z, nodata_int)
                }
                
                # 获取结果
                bands = {}
                for band_name, future in futures.items():
                    try:
                        bands[band_name] = future.result(timeout=30)
                    except Exception as e:
                        logger.error(f"读取波段 {band_name} 失败: {e}")
                        return None
            
            # 检查是否有任何波段读取失败
            if any(band is None for band in bands.values()):
                logger.warning(f"场景 {scene_id} 部分波段读取失败")
                return None
            
            # 组合波段数据
            data = np.stack([bands['red'], bands['green'], bands['blue']])
            
            # 创建基础mask（True表示有效像素，0-255范围）
            valid_mask = ~((bands['red'] == nodata_int) | 
                          (bands['green'] == nodata_int) | 
                          (bands['blue'] == nodata_int))
            
            mask = valid_mask.astype(np.uint8) * 255
            
            # 创建ImageData对象，正确处理nodata
            # 检查哪些像素是有效的（非nodata）
            valid_mask = ~((bands['red'] == nodata_int) | 
                          (bands['green'] == nodata_int) | 
                          (bands['blue'] == nodata_int))
            
            # 统计有效像素数量用于调试
            valid_pixels = np.count_nonzero(valid_mask)
            total_pixels = valid_mask.size
            logger.debug(f"场景 {scene_id} 有效像素: {valid_pixels}/{total_pixels} ({valid_pixels/total_pixels:.1%})")
            
            # cutline_mask: 0表示无效像素（透明），255表示有效像素（不透明）
            cutline_mask = valid_mask.astype(np.uint8) * 255
            
            # 如果没有任何有效像素，返回None
            if valid_pixels == 0:
                logger.warning(f"场景 {scene_id} 没有有效像素")
                return None
            
            image_data = ImageData(
                array=data,
                cutline_mask=cutline_mask,
                crs=rasterio.crs.CRS.from_epsg(3857)  # Web Mercator
            )
            
            # 添加场景标识符用于云掩膜处理
            image_data.asset_id = scene.scene_id
            
            return image_data
            
        except Exception as e:
            logger.error(f"Mosaic读取器处理场景 {src_path} 失败: {e}")
            import traceback
            logger.error(f"详细错误: {traceback.format_exc()}")
            return None
    
    return mosaic_reader_func

def read_single_band(path: str, x: int, y: int, z: int, nodata_int: int) -> Optional[np.ndarray]:
    """读取单个波段数据"""
    try:
        with Reader(path, options={'nodata': nodata_int}) as reader:
            band_data = reader.tile(x, y, z, tilesize=256)
            original_data = band_data.data[0]
            return convert_to_uint8(original_data, original_data.dtype)
    except Exception as e:
        logger.error(f"无法读取文件 {path}: {e}")
        return None

####### 场景选择策略 ########################################################################################

def select_optimal_scenes(scenes: List[SceneInfo], max_scenes: int = 10) -> Tuple[List[SceneInfo], bool]:
    """选择最优场景组合"""
    if not scenes:
        return [], False
    
    # 检查是否有全覆盖场景（覆盖率 >= 99.9%）
    full_coverage_scenes = [s for s in scenes if s.coverage >= 0.999]
    
    if full_coverage_scenes:
        # 从全覆盖场景中选择云量最少的
        best_scene = min(full_coverage_scenes, key=lambda s: s.cloud)
        logger.info(f"找到 {len(full_coverage_scenes)} 个全覆盖景")
        logger.info(f"选择全覆盖景中云量最少的景: {best_scene.scene_id} (云量: {best_scene.cloud:.1%})")
        return [best_scene], True
    else:
        # 按覆盖率排序，选择前 max_scenes 个
        sorted_scenes = sorted(scenes, key=lambda s: s.coverage, reverse=True)
        selected = sorted_scenes[:max_scenes]
        logger.info(f"没有找到全覆盖景，将按覆盖率排序选择前{max_scenes}个景")
        logger.info(f"选择的景覆盖率范围: {selected[-1].coverage:.1%} - {selected[0].coverage:.1%}")
        logger.info(f"将处理 {len(selected)} 个景")
        return selected, False

####### 主路由处理函数 ########################################################################################

router = APIRouter()

@router.get("/{z}/{x}/{y}.png")
def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    start_time = time.time()
    request_id = f"tile-{z}/{x}/{y}"
    thread_context.set(request_id)
    
    logger.info(f"开始处理瓦片请求")

    try:
        # 1. 计算瓦片边界
        t1 = time.time()
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']
        logger.info(f"计算 tile 边界耗时: {time.time() - t1:.3f} 秒")

        # 2. 获取后端配置数据
        t2 = time.time()
        url = common_config['create_no_cloud_config_url']
        data = {"sensorName": sensorName, "points": points}
        
        # 获取请求头
        headers = {}
        if auth := request.headers.get('Authorization'):
            headers['Authorization'] = auth
        if cookie := request.headers.get('Cookie'):
            headers['Cookie'] = cookie
            
        json_response = requests.post(url, json=data, headers=headers).json()
        logger.info(f"SpringBoot 请求耗时: {time.time() - t2:.3f} 秒")
        
        # 3. 验证响应数据
        if not json_response or json_response.get('status') == -1:
            if json_response.get('status') == -1:
                logger.info("status为-1，直接返回透明瓦片")
            else:
                logger.warning("后端返回空数据，返回透明瓦片")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        # 4. 解析场景配置
        t3 = time.time()
        scenes, mapper = parse_scenes_config(json_response)
        if not scenes:
            logger.warning("后端返回的json中的sceneConfig为空")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        # 过滤SAR数据
        scenes = [s for s in scenes if 'SAR' not in s.sensor_name]
        if not scenes:
            logger.warning("过滤SAR后没有可用场景")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
        
        logger.info(f"处理 bandMapper 耗时: {time.time() - t3:.3f} 秒")

        # 5. 选择最优场景组合
        t4 = time.time()
        selected_scenes, use_single_scene = select_optimal_scenes(scenes)
        logger.info(f"场景选择耗时: {time.time() - t4:.3f} 秒")

        # 6. 准备镶嵌数据
        t5 = time.time()
        valid_scenes = []
        band_paths_dict = {}
        cloud_masks = {}
        
        logger.info(f"初始化图像数组耗时: 0.000 秒")
        
        # 处理选中的场景
        for i, scene in enumerate(selected_scenes):
            logger.info(f"开始处理场景 {i+1}/{len(selected_scenes)}: {scene.scene_id}")
            
            # 获取波段路径
            band_paths = get_band_paths(scene, mapper)
            if not band_paths:
                logger.warning(f"跳过场景 {scene.scene_id}：无法获取波段路径")
                continue
            
            valid_scenes.append(scene)
            band_paths_dict[scene.scene_id] = band_paths
            
            # 读取云掩膜（多场景模式）
            if not use_single_scene and scene.cloud_path:
                cloud_mask = read_cloud_mask(scene, x, y, z)
                if cloud_mask is not None:
                    cloud_masks[scene.scene_id] = cloud_mask
        
        if not valid_scenes:
            logger.warning("没有可用的有效场景")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            
        logger.info(f"准备镶嵌数据耗时: {time.time() - t5:.3f} 秒")

        # 7. 执行Rio-tiler Mosaic处理
        t6 = time.time()
        
        try:
            # 创建资产列表（使用场景ID作为资产标识）
            assets = [scene.scene_id for scene in valid_scenes]
            
            # 创建mosaic读取器
            mosaic_reader_func = create_mosaic_asset_reader(
                valid_scenes, band_paths_dict, mapper, cloud_masks
            )
            
            # 选择镶嵌方法
            if use_single_scene or not cloud_masks:
                pixel_selection = defaults.FirstMethod()
            else:
                pixel_selection = CloudAwareMosaicMethod(cloud_masks)
            
            # 使用rio_tiler.mosaic进行高效镶嵌
            result_image, used_assets = mosaic_reader(
                assets,
                mosaic_reader_func,
                x, y, z,
                pixel_selection=pixel_selection,
                tilesize=256,
                threads=min(len(valid_scenes), 4),  # 限制线程数
                **{'nodata': 0}
            )
            
            if result_image is None or result_image.data is None:
                logger.warning("镶嵌结果为空")
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
                
            logger.info(f"所有场景处理耗时: {time.time() - t6:.3f} 秒")
            
        except Exception as e:
            logger.error(f"Rio-tiler镶嵌处理失败: {e}")
            import traceback
            logger.error(f"详细错误: {traceback.format_exc()}")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        # 8. 渲染输出
        t7 = time.time()
        
        # 确保数据类型正确
        if result_image.data.dtype != np.uint8:
            result_image.data = result_image.data.astype(np.uint8)
        
        # 渲染PNG
        content = render(
            result_image.data, 
            mask=result_image.mask,
            img_format="png", 
            **img_profiles.get("png")
        )
        
        logger.info(f"图像渲染耗时: {time.time() - t7:.3f} 秒")
        
        # 9. 统计信息和返回
        total_time = time.time() - start_time
        valid_pixels = np.count_nonzero(result_image.mask) if result_image.mask is not None else 0
        total_pixels = 256 * 256
        fill_ratio = valid_pixels / total_pixels if total_pixels > 0 else 0
        
        logger.info(f"请求完成 - 总耗时: {total_time:.3f} 秒, "
                   f"填充率: {fill_ratio:.2%}, "
                   f"云覆盖率: 0.00%")
        
        return Response(content=content, media_type="image/png")

    except Exception as e:
        logger.error(f"处理请求时发生错误: {type(e).__name__}: {str(e)}")
        import traceback
        logger.error(f"错误堆栈:\n{traceback.format_exc()}")
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")