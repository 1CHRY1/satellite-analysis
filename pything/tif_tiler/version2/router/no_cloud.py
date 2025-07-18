from fastapi import APIRouter, Query, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import requests
import math
from config import minio_config, TRANSPARENT_CONTENT

####### Helper ########################################################################################
# MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
MINIO_ENDPOINT = minio_config['endpoint']

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
        return np.uint8(np.floor(data.clip(0, 255)))

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
    return polygon.intersects(bbox_polygon)


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
        # bbox = lon_lats.get('bbox')
        
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
                            # print(f"  {band} -> {available_bands[0]} (fallback)")
            else:
                # 正常处理其他卫星
                for band in bandList:
                    band_key = f"band_{mapper[band]}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        # print(f"警告: {scene['sceneId']} 缺少 {band} 波段 ({band_key})")
                        # 尝试查找备选波段
                        if band == 'Red' and 'band_1' in paths:
                            bands[band] = paths['band_1']
                        elif band == 'Green' and 'band_2' in paths:
                            bands[band] = paths['band_2']
                        elif band == 'Blue' and 'band_3' in paths:
                            bands[band] = paths['band_3']
            
            scene_band_paths[scene['sceneId']] = bands
        
        # 格网 target_H, target_W 固定为256x256
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，待填充
        
        
        ############### Core #########################################
        
        processed_scenes = 0
        filled_ratio = 0.0
        for scene in sorted_scene:
            
            if 'SAR' in scene.get('sensorName'): 
                continue
            
            nodata = scene.get('noData')
            
            scene_label = f"{scene.get('sensorName')}-{scene.get('sceneId')}-{scene.get('resolution')}"
            ########### Check cover #################################
            if not is_tile_covered(scene, lon_lats):
                continue
            
            processed_scenes += 1    
            
            ########### Get Fill Mask ###############################
            cloud_band_path = scene.get('cloudPath')

            if not cloud_band_path:
                # 无云波段，读取任意波段，获取nodata掩膜
                red_band_path = scene_band_paths[scene['sceneId']]['Red']
                if not red_band_path:
                    continue
                full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
                print('full_red_path', full_red_path)
                try:
                    with COGReader(full_red_path, options={'nodata': int(nodata)}) as reader:
                        # 默认全部无云，只考虑nodata
                        temp_img_data = reader.tile(x, y, z, tilesize=256)
                        nodata_mask = temp_img_data.mask
                except Exception as e:
                    print(f"无法读取文件 {full_red_path}: {str(e)}")
                    continue
                

                valid_mask = (nodata_mask.astype(bool))
                
            else:
                # 读原始影像获取 nodata_mask
                red_band_path = scene_band_paths[scene['sceneId']]['Red']
                if not red_band_path:
                    continue
                full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
                try:
                    with COGReader(full_red_path, options={'nodata': int(nodata)}) as reader:
        
                        # temp_img_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                        temp_img_data = reader.tile(x, y, z, tilesize=256)
                        nodata_mask = temp_img_data.mask
                except Exception as e:
                    print(f"无法读取文件 {full_red_path}: {str(e)}")
                    continue
                    
                # 读云波段获取 cloud_mask
                cloud_full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                try:
                    with COGReader(cloud_full_path, options={'nodata': int(nodata)}) as reader:
                        # cloud_img_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                        cloud_img_data = reader.tile(x, y, z, tilesize=256)
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
                            continue

                except Exception as e:
                    print(f"无法读取文件 {cloud_full_path}: {str(e)}")
                    continue
                
                # 非云 & 非nodata <--> 该景有效区域
                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool))
            
            # 待填充的区域 & 该景有效区域 <--> 该景应填充的区域
            fill_mask = need_fill_mask & valid_mask

            if np.any(fill_mask):
                
                def read_band(band_path):
                    full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + band_path

                    try:
                        with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                            # band_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                            band_data = reader.tile(x, y, z, tilesize=256)
                            original_data = band_data.data[0]
                            original_dtype = original_data.dtype

                            # 【新增】自动转换为uint8
                            converted_data = convert_to_uint8(original_data, original_dtype)

                            return converted_data
                    except Exception as e:
                        print(f"无法读取文件 {full_path}: {str(e)}")
                        return None
                    
                ################# NEW END ######################

                bands = scene_band_paths[scene['sceneId']]
                band_1 = read_band(bands['Red'])
                band_2 = read_band(bands['Green'])
                band_3 = read_band(bands['Blue'])
                
                if band_1 is None or band_2 is None or band_3 is None:
                    print(f"警告: {scene_label} 读取波段失败")
                    continue

                img_r[fill_mask] = band_1[fill_mask]
                img_g[fill_mask] = band_2[fill_mask]
                img_b[fill_mask] = band_3[fill_mask]

                need_fill_mask[fill_mask] = False # False if pixel filled
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                
            if not np.any(need_fill_mask):
                break
            
            
        ########### Stack To RGB ####################################
        img = np.stack([img_r, img_g, img_b])
        
        ########### Convert to PNG and Response ######################
        # 创建一个与图像相同大小的mask，用于处理透明度
        alpha_mask = (~need_fill_mask).astype(np.uint8) * 255
        
        # 使用rio_tiler的render函数将numpy数组转换为PNG
        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
        
        return Response(content=content, media_type="image/png")

    except Exception as e:
        print(f"错误类型: {type(e).__name__}")
        print(f"错误信息: {str(e)}")
        import traceback
        traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    
    
    
# # 2025-07-15 突然说要加什么全国亚米的，这里采用和上面相似的jsonUrl结构实现
# # 注：大部分拷贝自无云一版图代码，只不过这里不用考虑云了，只考虑nodata
# @router.get("/submeter/{z}/{x}/{y}")
# async def get_submeter_tile(
#     z: int, x: int, y: int,
#     jsonUrl: str = Query(...),
# ):
#     try:
#         json_response = requests.get(jsonUrl).json()
#         json_data = json_response.get('scenes', [])
#         lon_lats = tile_bounds(x, y, z)
        
#         ############### Prepare #########################
#         sorted_scene = sorted(json_data, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)
        
#         ######## 波段映射 bandMapper => scene_band_paths ########
#         # 之后直接从scene_band_paths[sceneId]['Red']这样取就行
#         scene_band_paths = {} # as cache
#         bandList = ['Red', 'Green', 'Blue']
#         for scene in json_data:
#             mapper = scene['bandMapper']
#             bands = { band: None for band in bandList}
#             paths = scene.get('paths', {})
            
#             # 特殊处理某些卫星的波段映射问题
#             if scene['sensorName'] == 'ZY1_AHSI':
#                 # ZY1_AHSI 的波段映射可能有误，使用默认映射
#                 print(f"警告: {scene['sceneId']} (ZY1_AHSI) 使用特殊波段映射")
#                 # 尝试使用合理的默认值
#                 default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
#                 for band in bandList:
#                     band_key = f"band_{default_mapping.get(band, '1')}"
#                     if band_key in paths:
#                         bands[band] = paths[band_key]
#                     else:
#                         # 如果还是找不到，使用第一个可用的波段
#                         available_bands = sorted([k for k in paths.keys() if k.startswith('band_')])
#                         if available_bands:
#                             bands[band] = paths[available_bands[0]]
#                             print(f"  {band} -> {available_bands[0]} (fallback)")
#             else:
#                 # 正常处理其他卫星
#                 for band in bandList:
#                     band_key = f"band_{mapper[band]}"
#                     if band_key in paths:
#                         bands[band] = paths[band_key]
#                     else:
#                         print(f"警告: {scene['sceneId']} 缺少 {band} 波段 ({band_key})")
#                         # 尝试查找备选波段
#                         if band == 'Red' and 'band_1' in paths:
#                             bands[band] = paths['band_1']
#                         elif band == 'Green' and 'band_2' in paths:
#                             bands[band] = paths['band_2']
#                         elif band == 'Blue' and 'band_3' in paths:
#                             bands[band] = paths['band_3']
            
#             scene_band_paths[scene['sceneId']] = bands

        
#         # 格网 target_H, target_W 固定为256x256
#         target_H, target_W = 256, 256
#         img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
#         img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
#         img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
#         need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，待填充
        
        
#         ############### Core #########################################
        
#         processed_scenes = 0
#         for scene in sorted_scene:
#             nodata = scene.get('noData')
            
#             scene_label = f"{scene.get('sensorName')}-{scene.get('sceneId')}-{scene.get('resolution')}"
#             ########### Check cover #################################
#             if not is_tile_covered(scene, lon_lats):
#                 print(f"\n{scene_label}: 不覆盖瓦片，跳过")
#                 continue
            
#             print(f"\n处理场景 {processed_scenes + 1}: {scene_label}")
#             processed_scenes += 1    
            
#             ########### Get Fill Mask ###############################

#             # 获取nodata掩膜
#             red_band_path = scene_band_paths[scene['sceneId']]['Red']
#             if not red_band_path:
#                 continue
#             full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
#             try:
#                 with COGReader(full_red_path, options={'nodata': int(nodata)}) as reader:
#                     temp_img_data = reader.tile(x, y, z, tilesize=256)
#                     nodata_mask = temp_img_data.mask
#             except Exception as e:
#                 print(f"无法读取文件 {full_red_path}: {str(e)}")
#                 continue
            
#             valid_mask = (nodata_mask.astype(bool))
            
            
#             # 待填充的区域 & 该景有效区域 <--> 该景应填充的区域
#             fill_mask = need_fill_mask & valid_mask

#             if np.any(fill_mask):
                
#                 def read_band(band_path):
#                     full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + band_path
#                     try:
#                         with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
#                             band_data = reader.tile(x, y, z, tilesize=256)
#                             original_data = band_data.data[0]
#                             original_dtype = original_data.dtype

#                             # 【新增】自动转换为uint8
#                             converted_data = convert_to_uint8(original_data, original_dtype)
#                             print(f"Band data converted from {original_dtype} to uint8")

#                             return converted_data
#                     except Exception as e:
#                         print(f"无法读取文件 {full_path}: {str(e)}")
#                         return None
                    
#                 ################# NEW END ######################

#                 bands = scene_band_paths[scene['sceneId']]
#                 band_1 = read_band(bands['Red'])
#                 band_2 = read_band(bands['Green'])
#                 band_3 = read_band(bands['Blue'])

#                 if band_1 is None or band_2 is None or band_3 is None:
#                     print(f"警告: {scene_label} 读取波段失败")
#                     continue

#                 img_r[fill_mask] = band_1[fill_mask]
#                 img_g[fill_mask] = band_2[fill_mask]
#                 img_b[fill_mask] = band_3[fill_mask]

#                 need_fill_mask[fill_mask] = False # False if pixel filled
#                 filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                
#             if not np.any(need_fill_mask) or filled_ratio > 0.995:
#                 print("填充完毕")
#                 break
            
            
#         ########### Stack To RGB ####################################
#         img = np.stack([img_r, img_g, img_b])
        
        
#         ########### Convert to PNG and Response ######################
#         alpha_mask = (~need_fill_mask).astype(np.uint8) * 255
#         content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))
#         return Response(content=content, media_type="image/png")

#     except Exception as e:
#         print(f"\n!!! 错误: 处理瓦片 {z}/{x}/{y} 时发生异常 !!!")
#         print(f"错误类型: {type(e).__name__}")
#         print(f"错误信息: {str(e)}")
#         import traceback
#         traceback.print_exc()
#         return Response(content=TRANSPARENT_CONTENT, media_type="image/png")