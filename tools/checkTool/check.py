import time
import sys, os
from pathlib import Path
import ssl
import json
import traceback
import argparse
import numpy as np
import math

# 新增依赖库 (需 pip install rasterio shapely)
import rasterio
from rasterio.features import shapes
from rasterio.warp import transform_geom
from shapely.geometry import shape, Polygon, box
from shapely.wkt import dumps as wkt_dumps, loads as wkt_loads
from shapely.ops import unary_union
from urllib.parse import urlparse, quote, urlunparse

## 环境变量和文件模块 ##################################################################################
if getattr(sys, 'frozen', False):
    base_path = os.path.dirname(sys.argv[0])
else:
    base_path = os.path.dirname(__file__)

import _cffi_backend
## 此处注意替换本地环境中的两个文件，一个是morecantile，一个是certifi，QQ群里有相应文件

## Main ##############################################################################################
from Utils.mySqlUtils import *
from Utils.minioUtil import *
from Utils.validate import *

# 获取MinIO客户端实例 (假设 minioUtil 中有一个全局或可获取的客户端对象)
# 如果 minioUtil 没有直接暴露 client，你需要根据你的 Utils 里的写法获取

# --- 配置 ---
# 熔断阈值：即将读取的像素数如果超过此值，认为数据量过大，直接跳过
# 例如：5000 * 5000 = 25,000,000 像素
# 如果是一张 uint8 的单波段图，这大约占用 225MB 内存，但在计算几何时会膨胀
MAX_PIXELS_THRESHOLD = 15000 * 15000

def get_url(bucket, object_name):
    try:
        # 核心修改：对 object_name 进行 URL 编码，但保留 '/' 不被转义
        encoded_object_name = quote(object_name, safe='/')
        # 拼接 URL
        url = f'http://{DB_CONFIG["MINIO_IP"]}:{DB_CONFIG["MINIO_PORT"]}/{bucket}/{encoded_object_name}'
        return url
    except Exception as e:
        print(f"[Warn] Generate url failed: {e}")
        return None

def simplify_to_quad(geom):
    """
    将任意多边形简化为近似的四边形（4个点）
    原理：使用 Douglas-Peucker 算法不断增大容差，直到点数 <= 5 (4个角+闭合点)
    """
    # 如果本身就是三角形或四边形，直接返回
    if len(geom.exterior.coords) <= 5:
        return geom
    
    # 获取几何的尺寸，用于计算动态容差
    minx, miny, maxx, maxy = geom.bounds
    max_dim = max(maxx - minx, maxy - miny)
    
    # 动态尝试简化，容差从 0.1% 到 20% 递增
    for factor in [0.001, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2]:
        tolerance = max_dim * factor
        simplified = geom.simplify(tolerance, preserve_topology=True)
        # 如果简化后是四边形（5个坐标点）或三角形（4个坐标点），就停止
        if len(simplified.exterior.coords) <= 5:
            return simplified
    
    # 如果实在简化不下来（极少见），退化回凸包或强制使用 OBB
    return geom.convex_hull

def calculate_valid_polygon(tif_url, metadata_nodata=None, default_nodata=0):
    """
    读取COG，计算严格贴合有效数据的四边形（非矩形）
    """
    try:
        with rasterio.open(tif_url) as src:
            if not src.crs:
                print(f"[Warn] No CRS found. Skipping.")
                return None

            # --- 数据量熔断 ---
            decimation_factor = 1 # 默认读取原始分辨率
            overviews = src.overviews(1)
            if overviews:
                if len(overviews) >= 2:
                    # 如果有多级金字塔，取倒数第二级（比最顶层清晰一倍）
                    decimation_factor = overviews[-2]
                else:
                    # 如果只有一级金字塔，只能取那一级
                    decimation_factor = overviews[-1]
            target_w = int(src.width / decimation_factor)
            target_h = int(src.height / decimation_factor)

            # ### [修改开始/Changed Start] 针对超大图的强制降采样逻辑 ###
            pixel_count = target_w * target_h
            
            if pixel_count > MAX_PIXELS_THRESHOLD:
                # 原逻辑：直接 return None
                # 新逻辑：计算额外的压缩比例，强制把尺寸压到阈值以内
                
                print(f"\033[93m[Warn] Image huge ({target_w}x{target_h}). Forcing downsample to fit threshold...\033[0m")
                
                # 计算缩放比例： sqrt(当前像素量 / 阈值)
                # 例如：当前是4倍阈值，则宽高各除以2
                ratio = math.sqrt(pixel_count / MAX_PIXELS_THRESHOLD)
                
                # 向上取整，并多给一点余量(1.1倍)，确保安全
                safe_scale = math.ceil(ratio * 1.1)
                
                # 重新计算目标宽高
                target_w = int(target_w / safe_scale)
                target_h = int(target_h / safe_scale)
                
                # 防止除到0
                target_w = max(1, target_w)
                target_h = max(1, target_h)
                
                print(f"      -> Downsampled size: {target_w}x{target_h} (Scale factor: {safe_scale})")
            
            # ### [修改结束/Changed End] ############################
                
            # if (target_w * target_h) > MAX_PIXELS_THRESHOLD:
            #     print(f"\033[91m[Skip] Image too huge ({target_w}x{target_h})!\033[0m")
            #     return None

            # 确定 Nodata
            nodata = src.nodata
            if nodata is None:
                nodata = metadata_nodata if metadata_nodata is not None else default_nodata

            # 读取数据
            data = src.read(1, out_shape=(target_h, target_w))
            transform = src.transform * src.transform.scale(
                src.width / data.shape[1], 
                src.height / data.shape[0]
            )

            # 创建 Mask
            if np.isnan(nodata):
                mask = ~np.isnan(data)
            else:
                mask = data != nodata
            
            if not np.any(mask):
                return None

            # 提取几何
            geoms = []
            for geom, val in shapes(mask.astype('uint8'), mask=mask, transform=transform):
                geoms.append(shape(geom))

            if not geoms:
                return None

            merged_geom = unary_union(geoms)
            
            # --- [核心修改] ---
            # 1. 先计算凸包 (Convex Hull) -> 此时紧贴边缘，但有锯齿，可能有几十个点
            raw_hull = merged_geom.convex_hull
            
            # 2. 简化为四边形 -> 去除锯齿，强制逼近为4个角点
            # 这样既保留了非90度角的特征（梯形/平行四边形），又只有4个点
            final_geom = simplify_to_quad(raw_hull)
            # ----------------

            # 坐标系转换
            result_geom = final_geom
            src_crs_code = src.crs.to_string().upper() if src.crs else ""
            
            if src.crs and 'EPSG:4326' not in src_crs_code:
                try:
                    g_mapping = final_geom.__geo_interface__
                    transformed_g = transform_geom(src.crs, 'EPSG:4326', g_mapping)
                    result_geom = shape(transformed_g)
                except Exception as e:
                    print(f"[Error] Reprojection failed: {e}")
                    return None
            
            # 范围检查
            bounds = result_geom.bounds
            if not (-180 <= bounds[0] <= 180 and -90 <= bounds[1] <= 90):
                print(f"[Warn] Bounds out of range: {bounds}")
                return None

            return result_geom

    except Exception as e:
        print(f"[Error] Processing failed for {tif_url}: {e}")
        return None

def is_bbox_match(db_wkt, calc_polygon, iou_threshold=0.85):
    """
    判断数据库中的 BBox 和计算出的 BBox 是否大致贴合
    使用 IoU (交并比) 或 重叠面积占比 来判断
    """
    try:
        if not db_wkt or db_wkt == "GEOMETRYCOLLECTION()":
            return False
            
        db_poly = wkt_loads(db_wkt)
        
        # 异常处理：如果几何无效
        if not db_poly.is_valid or not calc_polygon.is_valid:
            return False

        intersection_area = db_poly.intersection(calc_polygon).area
        union_area = db_poly.union(calc_polygon).area
        
        if union_area == 0: return False
        
        iou = intersection_area / union_area
        
        # 或者使用简单的覆盖率：计算出的有效范围是否大部分都在数据库记录的范围内？
        # coverage = intersection_area / calc_polygon.area 
        
        return iou > iou_threshold
    except Exception as e:
        print(f"Error comparing bbox: {e}")
        return False

def process_scenes():
    append_to_log(args.output_log, f"Start checking scenes...\r\n")
    scenes = get_all_scenes() # 假设这是你的 Utils 获取所有场景的方法

    # ---------------------- 分批请打开以下注释 ---------------------------
    # scenes = get_scenes_by_range(1, 10000)
    
    total = len(scenes)
    for idx, scene in enumerate(scenes):
        scene_id = scene["scene_id"]
        scene_name = scene["scene_name"]
        db_bbox_wkt = scene.get("bounding_box_wkt") # 获取数据库中现有的 WKT
        scene_nodata = scene.get("no_data", 0)  # 获取 scene 表中的 nodata
        
        print(f"[{idx+1}/{total}] Processing {scene_name}...")

        images = get_images_by_scene_id(scene_id)
        
        # 1. 存在性检查 (原有逻辑)
        missing_flag = False
        tif_path = ""
        object_name = ""
        try:
            if images:
                object_name = images[0].get("tif_path", "")
        except:
            pass
        for image in images:
            # === [核心修复] 增加重试机制 ===
            max_retries = 10
            check_success = False
            exists = False
            tif_path = image["tif_path"]

            for attempt in range(max_retries):
                try:
                    # 尝试调用 minio 检查文件
                    if file_exists(DB_CONFIG["MINIO_IMAGES_BUCKET"], tif_path):
                        exists = True
                    else:
                        exists = False
                    check_success = True
                    break # 成功执行检查，跳出重试循环
                except Exception as e:
                    print(f"  [Warn] MinIO connect failed ({attempt+1}/{max_retries}): {e}")
                    time.sleep(2) # 报错后等待2秒再试
            if not check_success:
                print(f"  [Error] Failed to check file after retries: {tif_path}")
                # 如果重试多次都连不上，为了防止程序崩溃，可以选择跳过当前文件或标记为丢失
                # 这里选择跳过当前 Scene，避免误删
                continue
            if not exists:
                missing_flag = True
                break
        
        if missing_flag:
            append_to_log(args.output_log, f"{scene_name} missing files. Deleting...\r\n")
            print(f"\033[91m{scene_name} does not exist in MinIO.\033[0m")
            for image in images:
                delete_image_by_id(image["image_id"])
            delete_scene_by_id(scene_id)
            continue # 删除后直接跳过后续 BBox 检查

        # 2. BBox 校验逻辑 (新功能)
        # 假设每个 Scene 只有一张主要的 TIF 用来定界，或者取列表第一个
        if not object_name: continue
        
        # 构造 MinIO URL
        # 注意：这里需要根据你的 MinIO 策略生成 URL。如果是私有桶，必须用预签名 URL
        img_url = get_url(DB_CONFIG["MINIO_IMAGES_BUCKET"], object_name)
        
        if not img_url:
            continue

        try:
            # 计算真实的有效多边形
            valid_polygon = calculate_valid_polygon(img_url, default_nodata=scene_nodata)
            
            if valid_polygon is None:
                append_to_log(args.output_log, f"{scene_name}: No valid data found in TIF.\r\n")
                continue

            # 比较是否贴合 (IoU 阈值设为 0.9，即90%重合度)
            match = is_bbox_match(db_bbox_wkt, valid_polygon, iou_threshold=0.9)
            
            if match:
                # print(f"  - BBox OK.")
                pass
            else:
                print(f"  - \033[93mBBox Mismatch detected. Updating...\033[0m")
                # 将 Shapely 对象转为 WKT 字符串
                new_wkt = wkt_dumps(valid_polygon)
                
                # 更新数据库
                update_scene_by_id(scene_id=scene_id, new_wkt=new_wkt)
                append_to_log(args.output_log, f"Updated BBox for {scene_name}\r\n")

        except Exception as e:
            err_msg = f"Error processing BBox for {scene_name}: {e}"
            print(err_msg)
            append_to_log(args.output_log, err_msg + "\r\n")

    print("------------ Program End ------------\r\n\r\n")

# 命令行参数
# 获取脚本所在目录
current_dir = Path(__file__).resolve().parent

# 默认参数设置
DEFAULT_DB_CONFIG = str(current_dir / "json" / "dbConfig.json")
DEFAULT_LOG_PATH = str(current_dir / "check.log")

parser = argparse.ArgumentParser(description='Process and fix remote sensing data')
parser.add_argument('--db_config', type=str, default=DEFAULT_DB_CONFIG, help='dbConfig.json path')
parser.add_argument('--output_log', type=str, default=DEFAULT_LOG_PATH, help='log file path')
args = parser.parse_args()

if __name__ == "__main__":
    print("------------ Program Start ------------\r\n\r\n")
    try:
        # 1. 加载配置
        DB_CONFIG = load_db_config(args.db_config)
        verify_db_config(DB_CONFIG)

        # 2. 执行主逻辑
        process_scenes()
        
    except Exception as e:
        exit_with_error(f"\033[91m[ERROR] {e}\n Error details: {traceback.format_exc()}\n --------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\033[0m\n\n")