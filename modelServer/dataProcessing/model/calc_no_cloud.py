import os, shutil
from multiprocessing import Pool, cpu_count
from dataProcessing.Utils.osUtils import uploadLocalFile
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataProcessing.model.task import Task
import dataProcessing.config as config
from dataProcessing.Utils.gridUtil import GridHelper
from functools import partial

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"
INFINITY = 999999

def process_grid(grid, scenes, grid_helper, scene_band_paths, minio_endpoint, temp_dir_path):
    from rio_tiler.io import COGReader
    import numpy as np
    import rasterio
    from rasterio.transform import from_bounds

    grid_x, grid_y = grid
    INFINITY = 999999

    def grid_bbox():
        bbox = grid_helper._get_grid_polygon(grid_x, grid_y)
        return bbox.bounds

    def is_grid_covered(scene):
        from shapely.geometry import shape, box
        bbox_wgs84 = grid_bbox()
        scene_geom = scene.get('bbox').get('geometry')
        polygon = shape(scene_geom)
        bbox_polygon = box(*bbox_wgs84)
        return polygon.contains(bbox_polygon)

    bbox = grid_bbox()
    target_H = None
    target_W = None

    img_R = None
    img_G = None
    img_B = None
    need_fill_mask = None
    first_shape_set = False
    
    ############### 预处理 ########################
    # 按分辨率排序，后续是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
    sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")))
    # sorted_scene = sorted(
    #     scenes,
    #     key=lambda obj: (
    #         0 if obj.get("cloudPath") else 1,  # 有 cloudPath 的排前面（0 < 1）
    #         float(obj["resolution"].replace("m", ""))  # 分辨率小的排前面（高分辨率）
    #     )
    # )
    
    
    
    
    ############### 景遍历 ######################## 
    for scene in sorted_scene:

        nodata = scene.get('noData')
        scene_label = scene.get('sensorName') + scene.get('sceneId')
        
        # Step 0 检验： 云波段检验
        if not is_grid_covered(scene):
            print(scene_label, ':: not cover, jump')
            continue

        # Step 0.0 云量
        cloud_band_path = scene.get('cloudPath')
        
        if not cloud_band_path:
            print(scene_label, ':: no cloud_band, 默认无云')

            if not first_shape_set:
                # print(f"{scene_label} 没有云波段，R通道推断 shape")

                scene_id = scene['sceneId']
                paths = scene_band_paths.get(scene_id)
                if not paths or not all(paths.values()):
                    print(f"{scene_label} 缺失 RGB 波段路径，跳过")
                    continue

                try:
                    full_path = minio_endpoint + "/" + scene['bucket'] + "/" + paths['red']
                    with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                        temp_img_data = reader.part(bbox=bbox, indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"[fallback] 通过红波段确定目标尺寸为: H={target_H}, W={target_W}")

                        img_R = np.full((target_H, target_W), 0, dtype=np.uint16)
                        img_G = np.full((target_H, target_W), 0, dtype=np.uint16)
                        img_B = np.full((target_H, target_W), 0, dtype=np.uint16)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool)
                        first_shape_set = True

                except Exception as e:
                    print(f"[fallback] 读取红波段失败，跳过：{e}")
                    continue

            # 默认认为全部有效（无云），只受 nodata 控制
            valid_mask = np.ones((target_H, target_W), dtype=bool)
                    
        else:

            full_url = minio_endpoint + "/" + scene.get('bucket') + '/' + cloud_band_path
            with COGReader(full_url, options={'nodata':int(nodata)}) as ctx:
                print('Process', scene_label)

                # Step 0.9 首次读取时，确定目标尺寸， 不然part的结果会出现一个像素的偏差，导致后续的mask计算错误
                if not first_shape_set:
                    # 尝试读取一次以获取默认的输出尺寸
                    temp_img_data = ctx.part(bbox=bbox, indexes=[1])
                    target_H, target_W = temp_img_data.data[0].shape
                    print(f"确定目标尺寸为: H={target_H}, W={target_W}")

                    img_R = np.full((target_H, target_W), 0, dtype=np.uint16)
                    img_G = np.full((target_H, target_W), 0, dtype=np.uint16)
                    img_B = np.full((target_H, target_W), 0, dtype=np.uint16)
                    need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，全都待标记
                    first_shape_set = True
                
                # Step 1 基于Width Height读取云波段，获取云掩膜
                try:
                    # 这里指定宽度高度，默认resampling_method Nearest，其实就做了重采样
                    img_data = ctx.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    image_data = img_data.data[0]
                    nodata_mask = img_data.mask # 这里，值为true的是有值，值为false的是无值， 这里是无效值掩膜
                except Exception as e:
                    print(f"读取 {scene.get('sceneId')} 云波段失败，尺寸不匹配或I/O错误: {e}")
                    continue # 跳过当前场景，继续下一个

            sensorName = scene.get('sensorName')

            if "Landsat" in sensorName or "Landset" in sensorName:
                cloud_mask = (image_data & (1 << 3)) > 0
                
            elif "MODIS" in sensorName:
                cloud_state = (image_data & 0b11)
                cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                
            elif "GF" in sensorName:
                cloud_mask = (image_data == 2)
                
            else:
                print("不支持的传感器：" , sensorName)
                continue
            
            print(f"这一瓦片有云的像素数：",np.count_nonzero(cloud_mask))
            print(f"这一瓦片的非Nodata像素数: ",np.count_nonzero(nodata_mask.astype(bool)))

            # !!! valid_mask <--> 无云 且 非nodata 
            valid_mask = (~cloud_mask) & (nodata_mask.astype(bool))
        

        if not first_shape_set:
            img_shape = image_data.shape
            H, W = img_shape
            img_R = np.full((H, W), 0, dtype=np.uint16) # 初始化 0
            img_G = np.full((H, W), 0, dtype=np.uint16) # 初始化 0
            img_B = np.full((H, W), 0, dtype=np.uint16) # 初始化 0
            need_fill_mask = np.ones((H, W), dtype=bool) # all true，全都待标记
            first_shape_set = True

        # !!! fill_mask <--> 需要填充的区域 & 该景有效区域 <--> 该景可以填充格网的区域
        fill_mask = need_fill_mask & valid_mask
        print(f"这一瓦片可填充的像素数：",np.count_nonzero(fill_mask))

        if np.any(fill_mask): # 只要有任意一个是1 ，那就可以填充
            # 读取 RGB 波段
            scene_id = scene['sceneId']
            paths = scene_band_paths.get(scene_id)
            if not paths:
                continue

            def read_band(band_path):
                full_path = minio_endpoint + "/" + scene['bucket'] + "/" + band_path
                with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                    return reader.part(bbox=bbox, indexes=[1], height=target_H, width=target_W).data[0]

            R = read_band(paths['red'])
            G = read_band(paths['green'])
            B = read_band(paths['blue'])
            

            img_R[fill_mask] = R[fill_mask] # numpy的批量赋值填充

            img_G[fill_mask] = G[fill_mask] 

            img_B[fill_mask] = B[fill_mask]

            need_fill_mask[fill_mask] = False # 填过了，标记False

                    
            filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
            print(f"当前grid填充进度：{filled_ratio * 100:.2f}%")

        if not np.any(need_fill_mask):
            print("填充完毕")
            break

        print('ENDING ----- ', scene.get('sceneId'))

    # 检查是否有填充结果
    if not first_shape_set or np.all(need_fill_mask):
        print("未被填满，可能空间上不包含，可能缺乏cloudpath，可能无云区域确实无法填满", grid_x, grid_y)
        return None  # 没有可用数据
        
    first_shape_set = False
    
    # 读取RGB并保存tif
    img = np.stack([img_R, img_G, img_B])
    
    grid_statistic = {
        "min_r": img_R.min(),
        "max_r": img_R.max(),
        "min_g": img_G.min(),
        "max_g": img_G.max(),
        "min_b": img_B.min(),
        "max_b": img_B.max()
    }

    tif_path = os.path.join(temp_dir_path, f"{grid_x}_{grid_y}.tif")
    transform = from_bounds(*bbox, img.shape[2], img.shape[1])
    with rasterio.open(
        tif_path, 'w',
        driver='COG',
        height=img.shape[1],
        width=img.shape[2],
        count=3,
        dtype=img.dtype,
        crs='EPSG:4326',
        transform=transform
    ) as dst:
        dst.write(img)

    return tif_path, grid_x, grid_y, grid_statistic


def upload_one(tif_path, grid_x, grid_y, task_id):
    minio_key = f"{task_id}/{grid_x}_{grid_y}.tif"
    uploadLocalFile(tif_path, config.MINIO_TEMP_FILES_BUCKET, minio_key)
    return {
        "grid": [grid_x, grid_y],
        "bucket": config.MINIO_TEMP_FILES_BUCKET,
        "tifPath": minio_key
    }


def do_statistic(results):
    """统计了所有格子的最值的平均，拉伸友好"""
    ave_statistic = {
        "min_r": 0,
        "max_r": 0,
        "min_g": 0,
        "max_g": 0,
        "min_b": 0,
        "max_b": 0
    }
    count = 0
    for result in results:
        if result is None:
            continue
        _, _, _, stats = result
        count = count + 1
        ave_statistic["min_r"] += stats["min_r"]
        ave_statistic["max_r"] += stats["max_r"]
        ave_statistic["min_g"] += stats["min_g"]
        ave_statistic["max_g"] += stats["max_g"]
        ave_statistic["min_b"] += stats["min_b"]
        ave_statistic["max_b"] += stats["max_b"]

    if count > 0:
        ave_statistic["min_r"] /= count
        ave_statistic["max_r"] /= count
        ave_statistic["min_g"] /= count
        ave_statistic["max_g"] /= count
        ave_statistic["min_b"] /= count
        ave_statistic["max_b"] /= count
    else:
        ave_statistic = None  # 或者填充默认值

    return ave_statistic


import os, time
import glob
import rasterio
from rasterio.merge import merge
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from contextlib import ExitStack

def merge_tifs(temp_dir_path: str, task_id: str) -> str:
    os.environ["GDAL_NUM_THREADS"] = "ALL_CPUS"
    os.environ["GDAL_CACHEMAX"] = "20480" # mb

    # 获取所有 tif 文件
    tif_files = glob.glob(os.path.join(temp_dir_path, "*.tif"))
    if not tif_files:
        raise ValueError("No .tif files found in the directory")

    # 安全管理多个文件句柄
    with ExitStack() as stack:
        src_files_to_mosaic = [stack.enter_context(rasterio.open(fp)) for fp in tif_files]

        # 合并影像
        mosaic, out_trans = merge(src_files_to_mosaic, mem_limit=20480, use_highest_res=False)

        # 基于第一个影像获取元信息
        out_meta = src_files_to_mosaic[0].meta.copy()
        out_meta.update({
            "driver": "GTiff",
            "height": mosaic.shape[1],
            "width": mosaic.shape[2],
            "transform": out_trans,
            "count": out_meta.get("count", 1),
            "crs": src_files_to_mosaic[0].crs,
            "tiled": True,
            "blockxsize": 512,
            "blockysize": 512   
        })

    profile = cog_profiles.get("zstd")  # 更快的压缩格式
    config = {
        "GDAL_NUM_THREADS": "ALL_CPUS",
        "GDAL_TIFF_OVR_BLOCKSIZE": "512"
    }


    # 路径设置
    temp_merge_path = os.path.join(temp_dir_path, f"{task_id}_temp_merge.tif")
    final_merge_path = os.path.join(temp_dir_path, f"{task_id}_merge_cog.tif")

    # 写入临时 GeoTIFF
    with rasterio.open(temp_merge_path, "w", **out_meta) as dest:
        dest.write(mosaic)

    # 转为 COG
    if cog_info(temp_merge_path)["COG"]:
        return temp_merge_path
    else:
        with rasterio.open(temp_merge_path) as src:
            cog_translate(
                src, final_merge_path, profile,
                in_memory=False,  # 改为磁盘处理
                overview_resampling="nearest",
                resampling="nearest",
                allow_intermediate_compression=True,
                config=config
            )
        print('合并结果：',temp_merge_path)
        # os.remove(temp_merge_path)
        return final_merge_path


# def merge_tifs(temp_dir_path: str, task_id: str) -> str:
#     # 获取所有 tif 文件
#     tif_files = glob.glob(os.path.join(temp_dir_path, "*.tif"))
#     if not tif_files:
#         raise ValueError("No .tif files found in the directory")

#     # 打开所有影像
#     src_files_to_mosaic = [rasterio.open(fp) for fp in tif_files]

#     # 合并影像
#     mosaic, out_trans = merge(src_files_to_mosaic, mem_limit= 20480, use_highest_res=True)

#     # 基于第一个影像获取元信息
#     out_meta = src_files_to_mosaic[0].meta.copy()
#     out_meta.update({
#         "driver": "GTiff",  # 中间文件是标准 GeoTIFF，后续再转为 COG
#         "height": mosaic.shape[1],
#         "width": mosaic.shape[2],
#         "transform": out_trans,
#         "count": out_meta.get("count", 1),  # 确保波段数正确
#         "crs": src_files_to_mosaic[0].crs  # 保证空间参考一致
#     })

#     # 路径设置
#     temp_merge_path = os.path.join(temp_dir_path, f"{task_id}_temp_merge.tif")
#     final_merge_path = os.path.join(temp_dir_path, f"{task_id}_merge_cog.tif")

#     # 写入临时 GeoTIFF
#     with rasterio.open(temp_merge_path, "w", **out_meta) as dest:
#         dest.write(mosaic)

#     # 转为 COG    
#     if cog_info(temp_merge_path)["COG"]:
#         return temp_merge_path
#     else:
#         with rasterio.open(temp_merge_path) as src:
#             profile = cog_profiles.get("deflate")
#             cog_translate(src, final_merge_path, profile, in_memory=True, overview_resampling="nearest", resampling="nearest", allow_intermediate_compression=False, temporary_compression="LZW", config={"GDAL_NUM_THREADS": "ALL_CPUS"})
#             os.remove(temp_merge_path)
#         return final_merge_path



class calc_no_cloud(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])

    def run(self):
        print("NoCloudGraphTask run")

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution
        scenes = self.scenes
        # cloud = data.get('cloud') 没啥用

        ## Step 2 : Multithread Processing 4 Grids ############################# 
        grid_helper = GridHelper(gridResolution)

        scene_band_paths = {} # as cache 
        for scene in scenes:
            mapper = scene['bandMapper']
            bands = { 'red': None, 'green': None, 'blue': None }
            for img in scene['images']:
                if img['band'] == mapper['Red']:
                    bands['red'] = img['tifPath']
                if img['band'] == mapper['Green']:
                    bands['green'] = img['tifPath']
                if img['band'] == mapper['Blue']:
                    bands['blue'] = img['tifPath']
            scene_band_paths[scene['sceneId']] = bands

        temp_dir_path = os.path.join(config.TEMP_OUTPUT_DIR, self.task_id)
        os.makedirs(temp_dir_path, exist_ok=True)
        
        print('start time ', time.time())

        process_func = partial(
            process_grid,
            scenes=scenes,
            grid_helper=grid_helper,
            scene_band_paths=scene_band_paths,
            minio_endpoint=MINIO_ENDPOINT,
            temp_dir_path=temp_dir_path
        )

        # process_func(grids[1])
        
        with Pool(processes=cpu_count()) as pool:
            results = pool.map(process_func, grids)


        ## Step 3 : Results Uploading and Statistic #######################
        # upload_results = []
        # stats = do_statistic(results)
        
        print('end time ', time.time())
        
        # with ThreadPoolExecutor(max_workers=8) as executor:
        #     futures = [
        #         executor.submit(upload_one, tif_path, grid_x, grid_y, self.task_id)
        #         for result in results if result is not None
        #         for tif_path, grid_x, grid_y, grid_statistic in [result]
        #     ]
        #     for future in as_completed(futures):
        #         upload_results.append(future.result())
    
        # upload_results.sort(key=lambda x: (x["grid"][0], x["grid"][1]))
        
        print('start merge ',time.time())
        result_path = merge_tifs(temp_dir_path, task_id=self.task_id)
        print('end merge ',time.time())
        minio_path = f"{self.task_id}/noCloud_merge.tif"
        uploadLocalFile(result_path, config.MINIO_TEMP_FILES_BUCKET, minio_path)
        print('end upload ',time.time())
        
        # # print(upload_results)
        print('=============ok=================')

        # if os.path.exists(temp_dir_path):
        #     shutil.rmtree(temp_dir_path) 

        # return {
        #     "grids": upload_results,
        #     "statistic": stats
        # }
        return {
            "bucket": config.MINIO_TEMP_FILES_BUCKET,
            "tifPath": minio_path
        }