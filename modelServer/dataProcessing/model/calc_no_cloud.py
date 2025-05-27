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
    for scene in scenes:

        nodata = scene.get('noData')
        
        print('处理景', scene.get('sceneId'))
        if not is_grid_covered(scene):
            continue

        # Step 0.0 云量
        cloud_band_path = scene.get('cloudPath')
        
        if not cloud_band_path:
            # 这里是否要操作一下呢
            print("缺乏Cloud_band")
            continue

        full_url = minio_endpoint + "/" + scene.get('bucket') + '/' + cloud_band_path
        with COGReader(full_url, options={'nodata':int(nodata)}) as ctx:
            # img_data = ctx.part(bbox=bbox, indexes=[1])
            # image_data = img_data.data[0]
            # nodata_mask = img_data.mask

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
                print(f"读取 {scene.get('sceneId')} 的grid区域成功")
            except Exception as e:
                print(f"读取 {scene.get('sceneId')} 云波段失败，尺寸不匹配或I/O错误: {e}")
                continue # 跳过当前场景，继续下一个

            sensorName = scene.get('sensorName')

            if "Landsat" in sensorName or "Landset" in sensorName:
                cloud_mask = (image_data & (1 << 3)) > 0
            elif "MODIS" in sensorName:
                cloud_mask = (image_data & 1) > 0
            elif "GF" in sensorName:
                cloud_mask = (image_data == 2)
            else:
                print("不支持的传感器：" , sensorName)
                continue

            # !!! valid_mask <--> 无云 且 非nodata 
            valid_mask = (~cloud_mask) & (nodata_mask.astype(bool))
            print(f"读取 {scene.get('sceneId')} 的最终有效区域mask成功")
            

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
                print(f"读取 {scene.get('sceneId')} 的原始RGB成功")
                

                img_R[fill_mask] = R[fill_mask] # numpy的批量赋值填充
                print(f" {scene.get('sceneId')}  R部分填充")

                img_G[fill_mask] = G[fill_mask] 
                print(f" {scene.get('sceneId')}  G部分填充")

                img_B[fill_mask] = B[fill_mask]
                print(f" {scene.get('sceneId')}  B部分填充")

                need_fill_mask[fill_mask] = False # 填过了，标记False

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
        upload_results = []
        stats = do_statistic(results)
        
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = [
                executor.submit(upload_one, tif_path, grid_x, grid_y, self.task_id)
                for result in results if result is not None
                for tif_path, grid_x, grid_y, grid_statistic in [result]
            ]
            for future in as_completed(futures):
                upload_results.append(future.result())
    
        upload_results.sort(key=lambda x: (x["grid"][0], x["grid"][1]))
        
        # print(upload_results)
        print('ok')

        # if os.path.exists(temp_dir_path):
        #     shutil.rmtree(temp_dir_path) 

        return {
            "grids": upload_results,
            "statistic": stats
        }