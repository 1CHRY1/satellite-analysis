import os, shutil, glob, time, json
from functools import partial
from multiprocessing import Pool, cpu_count
import rasterio
from rasterio.merge import merge
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from concurrent.futures import ThreadPoolExecutor, as_completed

from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.gridUtil import GridHelper
from dataProcessing.model.task import Task
import dataProcessing.config as config

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"
INFINITY = 999999

def process_grid(grid, scenes, grid_helper, scene_band_paths, minio_endpoint, temp_dir_path):
    try:
        from rio_tiler.io import COGReader
        import numpy as np
        import rasterio
        from rasterio.transform import from_bounds
        from rasterio.enums import Resampling

        grid_x, grid_y = grid
        grid_lable = f'grid_{grid_x}_{grid_y}'
        INFINITY = 999999
        print('-' * 50)
        print(f" start { grid_lable }")

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
        
        ############### Prepare #########################
        # 按分辨率排序，格网的分辨率是以第一个景读到的像素为网格分辨率，所以先按分辨率排序
        sorted_scene = sorted(scenes, key=lambda obj: float(obj["resolution"].replace("m", "")), reverse=False)


        ############### Core ############################
        for scene in sorted_scene:
            
            nodata = scene.get('noData')
            scene_label = scene.get('sensorName') + '-' + scene.get('sceneId') + '-' + scene.get('resolution')
           
            print('Process', scene_label)
            
            ########### Check cover ######################
            if not is_grid_covered(scene):
                print(scene_label, ':: not cover, jump')
                continue
            
            print(scene.get('resolution'), grid_x, grid_y)

            ########### Check cloud ######################
            cloud_band_path = scene.get('cloudPath')
            
            if not cloud_band_path:
                # reading Red --> 确定格网 target_H, target_W
                if not first_shape_set: 

                    scene_id = scene['sceneId']
                    paths = scene_band_paths.get(scene_id)
                    full_path = minio_endpoint + "/" + scene['bucket'] + "/" + paths['red']
                    with COGReader(full_path, options={'nodata': int(nodata)}) as reader:
                        
                        temp_img_data = reader.part(bbox=bbox, indexes=[1]) # 不设置width/height的话不会重采样，分辨率与原始tif一致
                        nodata_mask = temp_img_data.mask
          
                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}")

                        img_R = np.full((target_H, target_W), 0, dtype=np.float32)
                        img_G = np.full((target_H, target_W), 0, dtype=np.float32)
                        img_B = np.full((target_H, target_W), 0, dtype=np.float32)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，待填充
                        first_shape_set = True


                # 默认全部无云，只考虑nodata
                valid_mask = (nodata_mask.astype(bool))
                        
            else:
                full_url = minio_endpoint + "/" + scene.get('bucket') + '/' + cloud_band_path
                # reading cloud_band --> 确定格网 target_H, target_W
                with COGReader(full_url, options={'nodata':int(nodata)}) as ctx:
                    
                    if not first_shape_set:
                        temp_img_data = ctx.part(bbox=bbox, indexes=[1])
                        target_H, target_W = temp_img_data.data[0].shape
                        print(f"{grid_lable}: H={target_H}, W={target_W}")

                        img_R = np.full((target_H, target_W), 0, dtype=np.float32)
                        img_G = np.full((target_H, target_W), 0, dtype=np.float32)
                        img_B = np.full((target_H, target_W), 0, dtype=np.float32)
                        need_fill_mask = np.ones((target_H, target_W), dtype=bool) # all true，全都待标记
                        first_shape_set = True
                    
                    # 这里指定宽度高度, 可能发生小的重采样，默认 Nearest
                    img_data = ctx.part(bbox=bbox, indexes=[1], height=target_H, width=target_W)
                    image_data = img_data.data[0]
                    nodata_mask = img_data.mask # true --> valid，false --> nodata
               
                sensorName = scene.get('sensorName')

                if "Landsat" in sensorName or "Landset" in sensorName:
                    cloud_mask = (image_data & (1 << 3)) > 0
                    
                elif "MODIS" in sensorName:
                    cloud_state = (image_data & 0b11)
                    cloud_mask = (cloud_state == 0) | (cloud_state == 1)
                    
                elif "GF" in sensorName:
                    cloud_mask = (image_data == 2)
                    
                else:
                    print("UNKNOWN :" , sensorName)
                    continue
                
                # print(f"这一瓦片有云的像素数：",np.count_nonzero(cloud_mask))
                # print(f"这一瓦片的非Nodata像素数: ",np.count_nonzero(nodata_mask.astype(bool)))

                # !!! valid_mask <--> 无云 且 非nodata 
                valid_mask = (~cloud_mask) & (nodata_mask.astype(bool))
            
            # 需要填充的区域 & 该景有效区域 <--> 该景可以填充格网的区域
            fill_mask = need_fill_mask & valid_mask
            
            # print(f"这一景这一瓦片可填充的像素数：",np.count_nonzero(fill_mask))

            if np.any(fill_mask): # 只要有任意一个是1 ，那就可以填充
                
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
                
                img_R[fill_mask] = R[fill_mask]
                img_G[fill_mask] = G[fill_mask] 
                img_B[fill_mask] = B[fill_mask]

                need_fill_mask[fill_mask] = False # False if pixel filled

                        
                filled_ratio = 1.0 - (np.count_nonzero(need_fill_mask) / need_fill_mask.size)
                
                print(f"grid fill progress: {filled_ratio * 100:.2f}%")

            if not np.any(need_fill_mask) or filled_ratio > 0.995:
                print("填充完毕")
                break

            print('ENDING ----- ')
            
        first_shape_set = False
        
        img = np.stack([img_R, img_G, img_B])
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
            transform=transform,
            BLOCKSIZE=512,
            COMPRESS='DEFLATE'
        ) as dst:
            dst.write(img)
            
        return tif_path, grid_x, grid_y
    
    except Exception as e:
        print(f"进程ERROR: {e}")
        return None

def upload_one(tif_path, grid_x, grid_y, task_id):
    minio_key = f"{task_id}/{grid_x}_{grid_y}.tif"
    uploadLocalFile(tif_path, config.MINIO_TEMP_FILES_BUCKET, minio_key)
    return {
        "grid": [grid_x, grid_y],
        "bucket": config.MINIO_TEMP_FILES_BUCKET,
        "tifPath": minio_key
    }


class calc_no_cloud_grid(Task):
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tile = self.args[0].get('tile', [0, 0]) # [x, y]
        self.resolution = self.args[0].get('resolution', 5) # km
        self.scenes = self.args[0].get('scenes', []) # scene object array
        

    def run(self):
        print("NoCloudGridTask run")

        ## Step 1 : Input Args #################################################
        grid_x, grid_y = self.tile
        gridResolution = self.resolution
        scenes = self.scenes

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
        
        start_time = time.time()

        process_func = partial(
            process_grid,
            scenes=scenes,
            grid_helper=grid_helper,
            scene_band_paths=scene_band_paths,
            minio_endpoint=MINIO_ENDPOINT,
            temp_dir_path=temp_dir_path
        )

        tif_path, grid_x, grid_y = process_func(grid_x, grid_y)
        
        end_time = time.time()

        print('end time ', end_time - start_time)

        ## Step 3 : Results Uploading #######################

        res = upload_one(tif_path, grid_x, grid_y, self.task_id)

        print('Task Cost: ', end_time - start_time)

        if os.path.exists(temp_dir_path):
            shutil.rmtree(temp_dir_path) 

        return res