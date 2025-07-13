from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import requests
import math

####### Helper ########################################################################################
# MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
MINIO_ENDPOINT = "http://192.168.1.110:30900"
TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()


def normalize(arr, min_val = 0 , max_val = 5000):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")


def convert_to_uint8(data, original_dtype):
    """
    自动将不同数据类型转换为uint8
    - Byte (uint8): 直接返回
    - UInt16: 映射到0-255范围
    """
    if original_dtype == np.uint8:
        return data.astype(np.uint8)
    elif original_dtype == np.uint16:
        # 将 uint16 (0-65535) 线性映射到 uint8 (0-255)
        return (data / 65535.0 * 255.0).astype(np.uint8)
    else:
        # 其他类型，先归一化到0-1，再映射to 0-255
        data_min = np.min(data)
        data_max = np.max(data)
        if data_max > data_min:
            normalized = (data - data_min) / (data_max - data_min)
            return (normalized * 255.0).astype(np.uint8)
        else:
            return np.zeros_like(data, dtype=np.uint8)


def tile_bounds(x, y, z):
    """ Calculate the bounds of a tile in degrees """
    Z2 = math.pow(2, z)

    ul_lon_deg = x / Z2 * 360.0 - 180.0
    ul_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y / Z2)))
    ul_lat_deg = math.degrees(ul_lat_rad)

    lr_lon_deg = (x + 1) / Z2 * 360.0 - 180.0
    lr_lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / Z2)))
    lr_lat_deg = math.degrees(lr_lat_rad)

    result = {
        "west": ul_lon_deg,
        "east": lr_lon_deg,
        "south": lr_lat_deg,
        "north": ul_lat_deg,
        "bbox": [ul_lon_deg, lr_lat_deg, lr_lon_deg, ul_lat_deg]
    }
    
    return result


def is_tile_covered(scene, lon_lats):
    from shapely.geometry import shape, box
    bbox_wgs84 = lon_lats.get('bbox')
    scene_geom = scene.get('bbox').get('geometry')
    polygon = shape(scene_geom)
    bbox_polygon = box(*bbox_wgs84)
    return polygon.contains(bbox_polygon)


####### Router ########################################################################################
router = APIRouter()

@router.get("/{z}/{x}/{y}")
async def get_tile(
    z: int, x: int, y: int,
    jsonUrl: str = Query(...),
):
    
    try:
        json_response = requests.get(jsonUrl).json()
        # 获取scenes数组
        json_data = json_response.get('scenes', [])
        lon_lats = tile_bounds(x, y, z)
        bbox = lon_lats.get('bbox')
        
        ############### Prepare #########################
        # 按分辨率排序，格网的分辨率是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
        sorted_scene = sorted(json_data, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)
        
        # 处理bandMapper波段映射，之后直接从scene_band_paths[sceneId]['Red']这样取就行
        scene_band_paths = {} # as cache
        bandList = ['Red', 'Green', 'Blue']
        for scene in json_data:
            mapper = scene['bandMapper']
            bands = { band: None for band in bandList}
            paths = scene.get('paths', {})
            
            # 特殊处理某些卫星的波段映射问题
            if scene['sensorName'] == 'ZY1_AHSI':
                # ZY1_AHSI 的波段映射可能有误，使用默认映射
                print(f"警告: {scene['sceneId']} (ZY1_AHSI) 使用特殊波段映射")
                # 尝试使用合理的默认值
                default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
                for band in bandList:
                    band_key = f"band_{default_mapping.get(band, '1')}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        # 如果还是找不到，使用第一个可用的波段
                        available_bands = sorted([k for k in paths.keys() if k.startswith('band_')])
                        if available_bands:
                            bands[band] = paths[available_bands[0]]
                            print(f"  {band} -> {available_bands[0]} (fallback)")
            else:
                # 正常处理其他卫星
                for band in bandList:
                    band_key = f"band_{mapper[band]}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        print(f"警告: {scene['sceneId']} 缺少 {band} 波段 ({band_key})")
                        # 尝试查找备选波段
                        if band == 'Red' and 'band_1' in paths:
                            bands[band] = paths['band_1']
                        elif band == 'Green' and 'band_2' in paths:
                            bands[band] = paths['band_2']
                        elif band == 'Blue' and 'band_3' in paths:
                            bands[band] = paths['band_3']
            
            scene_band_paths[scene['sceneId']] = bands
        
        print(f"处理瓦片 {z}/{x}/{y}")
        print(f"找到 {len(json_data)} 个场景")
        
        # 打印每个场景的波段映射情况
        for sceneId, bands in scene_band_paths.items():
            scene = next(s for s in json_data if s['sceneId'] == sceneId)
            print(f"\n场景 {sceneId} ({scene['sensorName']}):")
            print(f"  分辨率: {scene['resolution']}")
            print(f"  波段映射: Red={bands['Red'] is not None}, Green={bands['Green'] is not None}, Blue={bands['Blue'] is not None}")
            if any(b is None for b in bands.values()):
                print(f"  警告: 有缺失的波段！")
        
        # 格网 target_H, target_W 固定为256x256
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，待填充
        
        
        ############### Core #########################################
        print(f"\n瓦片边界: {bbox}")
        print(f"开始处理 {len(sorted_scene)} 个场景...")
        
        processed_scenes = 0
        for scene in sorted_scene:
            nodata = scene.get('noData')
            
            scene_label = f"{scene.get('sensorName')}-{scene.get('sceneId')}-{scene.get('resolution')}"
            ########### Check cover #################################
            if not is_tile_covered(scene, lon_lats):
                print(f"\n{scene_label}: 不覆盖瓦片，跳过")
                continue
            
            print(f"\n处理场景 {processed_scenes + 1}: {scene_label}")
            processed_scenes += 1    
            
            ########### Get Fill Mask ###############################
            cloud_band_path = scene.get('cloudPath')

            if not cloud_band_path:
                # 无云波段，读取任意波段，获取nodata掩膜
                red_band_path = scene_band_paths[scene['sceneId']]['Red']
                if not red_band_path:
                    print(f"场景 {scene['sceneId']} 没有找到红色波段路径")
                    continue
                full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
                print(f"读取红色波段: {full_red_path}")
                with COGReader(full_red_path, options={'nodata': int(nodata)}) as reader:
                    # 默认全部无云，只考虑nodata
                    temp_img_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    nodata_mask = temp_img_data.mask
                
                valid_mask = (nodata_mask.astype(bool))
                
            else:
                # 读原始影像获取 nodata_mask
                red_band_path = scene_band_paths[scene['sceneId']]['Red']
                if not red_band_path:
                    print(f"场景 {scene['sceneId']} 没有找到红色波段路径")
                    continue
                full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
                print(f"读取红色波段: {full_red_path}")
                with COGReader(full_red_path, options={'nodata': int(nodata)}) as reader:
        
                    temp_img_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    nodata_mask = temp_img_data.mask
                    
                # 读云波段获取 cloud_mask
                cloud_full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                print(f"读取云掩膜: {cloud_full_path}")
                with COGReader(cloud_full_path, options={'nodata': int(nodata)}) as reader:
                    cloud_img_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    img_data = cloud_img_data.data[0] # 存储了QA波段的数据

                    sensorName = scene.get('sensorName')
                    if "Landsat" in sensorName or "Landset" in sensorName:
                        cloud_mask = (img_data & (1 << 3)) > 0

                    elif "MODIS" in sensorName:
                        cloud_state = (img_data & 0b11)
                        cloud_mask = (cloud_state == 0) | (cloud_state == 1)

                    elif "GF" in sensorName:
                        cloud_mask = (img_data == 2)

                    else:
                        print("UNKNOWN :" , sensorName)
                        continue
                    
                # 非云 & 非nodata <--> 该景有效区域
                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool))
            
            # 待填充的区域 & 该景有效区域 <--> 该景应填充的区域
            fill_mask = need_fill_mask & valid_mask

            if np.any(fill_mask):
                # print(scene_label, ':: fill')
                
                def read_band(band_path):
                    full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + band_path
                    with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                        band_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                        original_data = band_data.data[0]
                        original_dtype = original_data.dtype

                        # 【新增】自动转换为uint8
                        converted_data = convert_to_uint8(original_data, original_dtype)
                        print(f"Band data converted from {original_dtype} to uint8")

                        return converted_data
                    
                ################# NEW END ######################
                # paths = list(paths.values())
                # band_1 = read_band(paths[0])
                # band_2 = read_band(paths[1])
                # band_3 = read_band(paths[2])
                
                bands = scene_band_paths[scene['sceneId']]
                band_1 = read_band(bands['Red'])
                band_2 = read_band(bands['Green'])
                band_3 = read_band(bands['Blue'])

                img_r[fill_mask] = band_1[fill_mask]
                img_g[fill_mask] = band_2[fill_mask]
                img_b[fill_mask] = band_3[fill_mask]

                need_fill_mask[fill_mask] = False # False if pixel filled
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                
            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                print("填充完毕")
                break
            
            
        ########### Stack To RGB ####################################
        img = np.stack([img_r, img_g, img_b])
        
        # 统计信息
        filled_pixels = np.sum(~need_fill_mask)
        total_pixels = need_fill_mask.size
        fill_percentage = (filled_pixels / total_pixels) * 100
        
        print(f"\n=== 瓦片处理完成 ===")
        print(f"处理了 {processed_scenes} 个场景")
        print(f"填充像素: {filled_pixels}/{total_pixels} ({fill_percentage:.1f}%)")
        
        if fill_percentage == 0:
            print("警告: 没有任何像素被填充！可能原因：")
            print("  1. 所有场景都有云遮挡")
            print("  2. 瓦片区域全部是nodata")
            print("  3. 场景数据读取失败")
        
        ########### Convert to PNG and Response ######################
        # 创建一个与图像相同大小的mask，用于处理透明度
        alpha_mask = (~need_fill_mask).astype(np.uint8) * 255
        
        # 使用rio_tiler的render函数将numpy数组转换为PNG
        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
        
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(f"\n!!! 错误: 处理瓦片 {z}/{x}/{y} 时发生异常 !!!")
        print(f"错误类型: {type(e).__name__}")
        print(f"错误信息: {str(e)}")
        import traceback
        traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    