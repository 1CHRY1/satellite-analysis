from fastapi import APIRouter, Query, Request, Response
from rio_tiler.io import COGReader
from rio_tiler.utils import render
from rio_tiler.colormap import cmap
from rio_tiler.profiles import img_profiles
import numpy as np
import os
import requests
import math
from config import minio_config, TRANSPARENT_CONTENT
import time

####### Helper ########################################################################################
# MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
MINIO_ENDPOINT = "http://" + minio_config['endpoint']

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

def calc_tile_bounds(x, y, z):
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


####### Router ########################################################################################
router = APIRouter()

@router.get("/{z}/{x}/{y}.png")
def get_tile(
    request: Request,
    z: int, x: int, y: int,
    sensorName: str = Query(...),
):
    start_time = time.time()

    try:
        t1 = time.time()
        
        start_time = time.time()

        # 计算tile的边界
        tile_bound = calc_tile_bounds(x, y, z)
        points = tile_bound['bbox']
        print(f"[⏱] 计算 tile 边界耗时: {time.time() - t1:.3f} 秒")

        # spring boot 接口
        t2 = time.time()
        url = "http://192.168.1.127:8999/api/v1/modeling/example/noCloud/createNoCloudConfig"  # 测试使用，记得修改~~~~~~~~~~~~~~~
        data = {
        "sensorName": sensorName,
        "points": points
        }
        json_response = requests.post(url, json=data).json()
        print(f"[⏱] SpringBoot 请求耗时: {time.time() - t2:.3f} 秒")

        t3 = time.time()
        # 获取bandMapper
        mapper = json_response.get('data', {}).get('bandMapper', {})
        # 获取scenesConfig数组
        json_data = json_response.get('data', {}).get('scenesConfig', [])
        
        # 按分辨率排序
        sorted_scene = sorted(json_data, key = lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)
        print(len(sorted_scene))

        # 处理bandMapper
        scene_band_paths = {}
        bandList = ['Red', 'Green', 'Blue']

        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"请求操作耗时：{elapsed_time:.6f} 秒")

        if request.is_disconnected():
            print("disconnected")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        for scene in sorted_scene:
            bands = {band: None for band in bandList}
            paths = scene.get('path', {})
            
            if sensorName == 'ZY1_AHSI':
                # ZY1_AHSI 特殊处理
                default_mapping = {'Red': '4', 'Green': '3', 'Blue': '2'}
                for band in bandList:
                    band_key = f"band_{default_mapping.get(band, '1')}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        available_bands = sorted([k for k in paths.keys() if k.startswith('band_')])
                        if available_bands:
                            bands[band] = paths[available_bands[0]]
            else:
                # 正常处理
                for band in bandList:
                    band_key = f"band_{mapper[band]}"
                    if band_key in paths:
                        bands[band] = paths[band_key]
                    else:
                        # 备选波段处理
                        if band == 'Red' and 'band_1' in paths:
                            bands[band] = paths['band_1']
                        elif band == 'Green' and 'band_2' in paths:
                            bands[band] = paths['band_2']
                        elif band == 'Blue' and 'band_3' in paths:
                            bands[band] = paths['band_3']
            
            scene_band_paths[scene['sceneId']] = bands


        print(f"[⏱] 处理 bandMapper 耗时: {time.time() - t3:.3f} 秒")

        t4 = time.time()

        # 格网 target_H, target_W 固定为256x256
        target_H, target_W = 256, 256
        img_r = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_g = np.full((target_H, target_W), 0, dtype=np.uint8)
        img_b = np.full((target_H, target_W), 0, dtype=np.uint8)
        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，待填充

        print(f"[⏱] 初始化图像数组耗时: {time.time() - t4:.3f} 秒")


        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"整理操作耗时：{elapsed_time:.6f} 秒")


        processed_scenes = 0
        filled_ratio = 0.0


        t5 = time.time()
        for scene in sorted_scene:
            scene_start = time.time()

        if request.is_disconnected():
            print("disconnected")
            return Response(content=TRANSPARENT_CONTENT, media_type="image/png")

        for scene in sorted_scene:
            if request.is_disconnected():
                print("disconnected")
                return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
            

            if 'SAR' in scene.get('sensorName'):
                continue

            nodata = scene.get('noData')

            try:
                if nodata is not None:
                    nodata_int = int(float(nodata))  # 先转float再转int
                else:
                    nodata_int = 0  # 默认值
            except (ValueError, TypeError):
                nodata_int = 0  # 转换失败时使用默认值
                print(f"警告: 无法转换nodata值 '{nodata}'，使用默认值0")

            scene_label = f"{scene.get('sensorName')}-{scene.get('sceneId')}-{scene.get('resolution')}"

            processed_scenes += 1

            cloud_band_path = scene.get('cloudPath')

            if not cloud_band_path:
                red_band_path = scene_band_paths[scene['sceneId']]['Red']
                if not red_band_path:
                    continue
                full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
                try:
                    with COGReader(full_red_path, options={'nodata': int(nodata_int)}) as reader:
                        temp_img_data = reader.tile(x, y, z, tilesize=256)
                        nodata_mask = temp_img_data.mask
                except Exception as e:
                    print(f"无法读取文件 {full_red_path}: {str(e)}")
                    continue
                
                valid_mask = (nodata_mask.astype(bool))
            else:
                red_band_path = scene_band_paths[scene['sceneId']]['Red']
                if not red_band_path:
                    continue
                full_red_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + red_band_path
                try:
                    with COGReader(full_red_path, options={'nodata': int(nodata_int)}) as reader:
        
                        # temp_img_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                        temp_img_data = reader.tile(x, y, z, tilesize=256)
                        nodata_mask = temp_img_data.mask
                except Exception as e:
                    print(f"无法读取文件 {full_red_path}: {str(e)}")
                    continue
                    
                # 读云波段获取 cloud_mask
                cloud_full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                try:
                    with COGReader(cloud_full_path, options={'nodata': int(nodata_int)}) as reader:
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
            print(f"[⏱] 处理场景 {scene.get('sceneId')} 耗时: {time.time() - scene_start:.3f} 秒")
            if np.any(fill_mask):
                
                def read_band(band_path):
                    full_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + band_path

                    try:
                        with COGReader(full_path, options={'nodata': int(nodata_int)}) as reader:
                            # band_data = reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                            band_data = reader.tile(x, y, z, tilesize=256)
                            original_data = band_data.data[0]
                            original_dtype = original_data.dtype

                            # 自动转换为uint8
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

        print(f"[⏱] 所有场景处理耗时: {time.time() - t5:.3f} 秒")
        
        t6 = time.time()

        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"计算操作耗时：{elapsed_time:.6f} 秒")

        img = np.stack([img_r, img_g, img_b])

        alpha_mask = (~need_fill_mask).astype(np.uint8) * 255

        content = render(img, mask=alpha_mask, img_format="png", **img_profiles.get("png"))

        print(f"[⏱] 图像渲染耗时: {time.time() - t6:.3f} 秒")

        print(f"[⏱] 总耗时: {time.time() - start_time:.3f} 秒")

        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"总操作耗时：{elapsed_time:.6f} 秒")

        
        return Response(content=content, media_type="image/png")


        

    except Exception as e:
        print(f"错误类型: {type(e).__name__}")
        print(f"错误信息: {str(e)}")
        import traceback
        traceback.print_exc()
        return Response(content=TRANSPARENT_CONTENT, media_type="image/png")
    