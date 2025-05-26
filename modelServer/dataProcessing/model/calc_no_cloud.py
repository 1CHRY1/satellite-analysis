import json, os, time, math, shutil
from multiprocessing import Pool, cpu_count
import numpy as np
from shapely.geometry import shape, box
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from rio_tiler.io import COGReader
from rasterio.transform import from_bounds
from rasterio.merge import merge
import rasterio
import glob
from dataProcessing.Utils.osUtils import uploadLocalFile
from osgeo import gdal

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

    def calc_grid_cloud(scene):
        if not is_grid_covered(scene):
            return {'cloud': INFINITY}

        cloud_band_path = scene.get('cloudPath')
        if cloud_band_path:
            full_url = minio_endpoint + "/" + scene.get('bucket') + '/' + cloud_band_path
            with COGReader(full_url) as ctx:
                bbox = grid_bbox()
                img_data = ctx.part(bbox=bbox, indexes=[1])
                image_data = img_data.data[0]
                nodata_mask = img_data.mask
                sensorName = scene.get('sensorName')

                if "Landsat" in sensorName or "Landset" in sensorName:
                    cloud_mask = (image_data & (1 << 3)) > 0
                elif "MODIS" in sensorName:
                    cloud_mask = (image_data & 1) > 0
                elif "GF" in sensorName:
                    cloud_mask = (image_data == 2)
                else:
                    return {'cloud': INFINITY}

                nodata = nodata_mask.astype(bool)
                cloud = cloud_mask.astype(bool)
                valid_mask = (~nodata) & (~cloud)
                cloud_percentage = np.sum(valid_mask) / np.sum(image_data) * 100 if np.sum(image_data) > 0 else INFINITY
                return {'cloud': cloud_percentage}
        else:
            return {'cloud': scene.get('cloud', INFINITY)}

    # 找最优场景
    min_cloud = INFINITY
    best_scene = None
    for scene in scenes:
        cloud_result = calc_grid_cloud(scene)
        if cloud_result['cloud'] < min_cloud:
            min_cloud = cloud_result['cloud']
            best_scene = scene
            if min_cloud == 0:
                break

    if best_scene is None:
        return None

    # 读取RGB并保存为临时tif
    bbox = grid_bbox()
    scene_id = best_scene['sceneId']
    paths = scene_band_paths[scene_id]

    def read_band(band_path):
        full_path = minio_endpoint + "/" + best_scene['bucket'] + "/" + band_path
        with COGReader(full_path) as reader:
            return reader.part(bbox=bbox, indexes=[1]).data[0]

    R = read_band(paths['red'])
    G = read_band(paths['green'])
    B = read_band(paths['blue'])
    img = np.stack([R, G, B])

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

    return tif_path

def merge_tifs(temp_dir_path: str, task_id: str) -> str:
    # 获取所有 tif 文件
    tif_files = glob.glob(os.path.join(temp_dir_path, "*.tif"))
    if not tif_files:
        raise ValueError("No .tif files found in the directory")

    # 打开所有影像
    src_files_to_mosaic = [rasterio.open(fp) for fp in tif_files]

    # 合并影像
    mosaic, out_trans = merge(src_files_to_mosaic, mem_limit= 20480, use_highest_res=True)

    # 基于第一个影像获取元信息
    out_meta = src_files_to_mosaic[0].meta.copy()
    out_meta.update({
        "driver": "GTiff",  # 中间文件是标准 GeoTIFF，后续再转为 COG
        "height": mosaic.shape[1],
        "width": mosaic.shape[2],
        "transform": out_trans,
        "count": out_meta.get("count", 1),  # 确保波段数正确
        "crs": src_files_to_mosaic[0].crs  # 保证空间参考一致
    })

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
            profile = cog_profiles.get("deflate")
            cog_translate(src, final_merge_path, profile, in_memory=True, overview_resampling="nearest", resampling="nearest", allow_intermediate_compression=False, temporary_compression="LZW", config={"GDAL_NUM_THREADS": "ALL_CPUS"})
            os.remove(temp_merge_path)
        return final_merge_path

# def merge_tifs(temp_dir_path: str, task_id: str) -> str:
#     # 获取所有 TIF 文件
#     tif_files = glob.glob(os.path.join(temp_dir_path, "*.tif"))
#     if not tif_files:
#         raise ValueError("No .tif files found in the directory")
    
#     # 使用 GDAL 生成 VRT
#     vrt_file = os.path.join(temp_dir_path, f"{task_id}.vrt")
#     gdal.BuildVRT(vrt_file, tif_files, options=gdal.BuildVRTOptions(resolution="highest"))

#     # 直接转为 COG 格式
#     cog_file = os.path.join(temp_dir_path, f"{task_id}_merge_cog.tif")
#     gdal.Translate(cog_file, vrt_file, creationOptions=[
#         "TILED=YES", "COMPRESS=DEFLATE", "NUM_THREADS=ALL_CPUS", "BIGTIFF=YES"
#     ])
    
#     # 删除 VRT 文件
#     os.remove(vrt_file)
#     return cog_file

class calc_no_cloud(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])

    def run(self):
        print("MinCloudGraphTask run")

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution
        scenes = self.scenes
        # cloud = data.get('cloud') 没啥用

        ## Step 2 : Grids  ##############################################
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
        
        with Pool(processes=cpu_count()) as pool:
            pool.map(process_func, grids)

        result_path = merge_tifs(temp_dir_path, task_id=self.task_id)

        minio_path = f"{self.task_id}/noCloud_merge.tif"
        uploadLocalFile(result_path, config.MINIO_TEMP_FILES_BUCKET, minio_path)

        if os.path.exists(temp_dir_path):
            shutil.rmtree(temp_dir_path) 

        return {
            "bucket": config.MINIO_TEMP_FILES_BUCKET,
            "tifPath": minio_path
        }