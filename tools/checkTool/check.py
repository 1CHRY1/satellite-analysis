import sys, os
from pathlib import Path
import ssl
import json
import traceback
import argparse
import numpy as np

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
# 如果是一张 uint8 的单波段图，这大约占用 25MB 内存，但在计算几何时会膨胀
MAX_PIXELS_THRESHOLD = 5000 * 5000

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

def calculate_valid_polygon(tif_url, metadata_nodata=None, default_nodata=0):
    """
    读取COG的顶层金字塔，计算有效值的几何范围 (OBB)
    包含：CRS检查、数据量熔断机制
    """

    try:
        with rasterio.open(tif_url) as src:
            # --- [检查 CRS] ---
            if not src.crs:
                print(f"[Warn] No CRS found for {tif_url}. Skipping.")
                return None

            # --- [核心新增：数据量预判与熔断] ---
            overviews = src.overviews(1)
            
            # 预计算即将读取的目标宽和高
            if overviews:
                # 场景 A: 存在金字塔
                decimation_factor = overviews[-1] # 取最顶层（最小的图）
                target_w = int(src.width / decimation_factor)
                target_h = int(src.height / decimation_factor)
                log_msg = f"Top level size: {target_w}x{target_h}"
            else:
                # 场景 B: 没有金字塔，必须读原图
                decimation_factor = 1
                target_w = src.width
                target_h = src.height
                log_msg = f"No overviews. Full size: {target_w}x{target_h}"

            # 计算像素总数
            total_pixels = target_w * target_h

            # 判断是否超过阈值
            if total_pixels > MAX_PIXELS_THRESHOLD:
                print(f"\033[91m[Skip] Image too huge to process! {log_msg} (Threshold: {MAX_PIXELS_THRESHOLD})\033[0m")
                return None # 直接熔断，保护内存和网络
            # ----------------------------------------

            # 2. 确定 Nodata
            nodata = src.nodata
            if nodata is None:
                nodata = metadata_nodata if metadata_nodata is not None else default_nodata

            # 3. 安全读取数据 (Safe Read)
            # 即使有 overview，我们显式指定 out_shape 也是个好习惯，双重保险
            data = src.read(1, out_shape=(target_h, target_w))
            
            # 计算变换矩阵
            # 注意：如果 decimation_factor 为 1，scale 就是 1，相当于原 transform
            transform = src.transform * src.transform.scale(
                src.width / data.shape[1], 
                src.height / data.shape[0]
            )

            # 4. 创建 Mask
            if np.isnan(nodata):
                mask = ~np.isnan(data)
            else:
                mask = data != nodata
            
            if not np.any(mask):
                return None

            # 5. 提取几何并计算 OBB (4点)
            geoms = []
            for geom, val in shapes(mask.astype('uint8'), mask=mask, transform=transform):
                geoms.append(shape(geom))

            if not geoms:
                return None

            merged_geom = unary_union(geoms)
            final_geom = merged_geom.convex_hull.minimum_rotated_rectangle

            # 6. 坐标系转换与安全检查
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
            
            bounds = result_geom.bounds
            if not (-180 <= bounds[0] <= 180 and -90 <= bounds[1] <= 90 and
                    -180 <= bounds[2] <= 180 and -90 <= bounds[3] <= 90):
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
            object_name = images[0]["tif_path"]
        except:
            pass
        for image in images:
            tif_path = image["tif_path"]
            if not file_exists(DB_CONFIG["MINIO_IMAGES_BUCKET"], tif_path):
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