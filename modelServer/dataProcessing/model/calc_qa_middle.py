import uuid
from osgeo import gdal
import json
import os
import requests
import concurrent.futures
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mband, convert_tif2cog, check_intersection, check_full_coverage_v2, check_full_coverage
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG
import time

MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
MULTI_TASKS = os.cpu_count() - 3

class calc_qa_middle(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])

    # 预先异步加载所有 cloud 数据
    def get_cloud_dataSets(self, scenes):
        preloaded_cloud_data = {}

        def load_scene(scene):
            scene_id = scene["sceneId"]
            if "cloudPath" not in scene:
                return None
            cloud_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
            dataset = gdal.Open(cloud_path)
            if dataset is None:
                return None
            return scene_id, dataset
        
        with concurrent.futures.ThreadPoolExecutor(max_workers = MULTI_TASKS) as executor:
            futures = [executor.submit(load_scene, scene) for scene in scenes]
            for future in concurrent.futures.as_completed(futures):
                result = future.result()
                if result is not None:
                    scene_id, dataset = result
                    preloaded_cloud_data[scene_id] = dataset

        return preloaded_cloud_data

    def get_bound_dataSets(self, scenes):
        preloaded_bound_data = {}

        def load_scene(scene):
            scene_id = scene["sceneId"]
            if "images" not in scene:
                return None
            image = scene['images'][0]
            tif_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image['tifPath']
            dataset = gdal.Open(tif_path)
            if dataset is None:
                return None
            return scene_id, dataset
        
        with concurrent.futures.ThreadPoolExecutor(max_workers = MULTI_TASKS) as executor:
            futures = [executor.submit(load_scene, scene) for scene in scenes]
            for future in concurrent.futures.as_completed(futures):
                result = future.result()
                if result is not None:
                    scene_id, dataset = result
                    preloaded_bound_data[scene_id] = dataset

        return preloaded_bound_data

    def run(self):
        def get_first_element_or_empty(lst):
            return lst[0] if lst else ''
        gridHelper = GridHelper(self.resolution)  # 实例化
        tiles_list = [None] * len(self.tiles)  # 初始化 tiles_list，确保长度与 tiles 一致

        preloaded_cloud_data = self.get_cloud_dataSets(self.scenes)
        # preloaded_bound_data = self.get_bound_dataSets(self.scenes)

        def process_scene(scene, bbox):
            image = scene['images'][0]
            tif_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image['tifPath']
            if check_full_coverage(tif_path, bbox) == False:
            # if check_full_coverage_v2(scene['bbox'], bbox) == False:
                qa = 9999
            if 'cloudPath' not in scene:
                qa = float(scene['cloud'])
            else:
                cloud_dataSet = preloaded_cloud_data[scene['sceneId']]
                qa = -1
                # qa = calculate_cloud_coverage(cloud_dataSet, scene['sensorName'], bbox)
            return qa

        def process_tile(tile_index):
            tile = self.tiles[tile_index]
            gridCell = GridCell(tile[0], tile[1])
            bbox = gridHelper.get_grid_bbox(gridCell)  # 计算bbox
            qas = []  # 用一个列表来保存每个景的云量

            if self.scenes:
                with concurrent.futures.ThreadPoolExecutor(max_workers = MULTI_TASKS) as executor:
                    futures = [executor.submit(process_scene, scene, bbox) for scene in self.scenes]
                    for future in concurrent.futures.as_completed(futures):
                        qas.append(future.result())  # 收集每个 scene 的云量

            min_qa = min(qas) if qas else 9999
            if min_qa == 9999:
                red_path = ''
                green_path = ''
                blue_path = ''
                bucket = ''
            else:
                min_index = qas.index(min_qa)  # 获取云量列表中最小值索引
                images = self.scenes[min_index]['images']
                bandMapper = self.scenes[min_index]['bandMapper']
                bucket = images[0]['bucket']
                red_paths = [image["tifPath"] for image in images if image["band"] == bandMapper['Red']]
                green_paths = [image["tifPath"] for image in images if image["band"] == bandMapper['Green']]
                blue_paths = [image["tifPath"] for image in images if image["band"] == bandMapper['Blue']]
                red_path = get_first_element_or_empty(red_paths)
                green_path = get_first_element_or_empty(green_paths)
                blue_path = get_first_element_or_empty(blue_paths)

            return tile_index, {'colId': tile[0], 'rowId': tile[1], 'redPath': red_path, 'greenPath': green_path, 'bluePath': blue_path, 'bucket': bucket}

        with concurrent.futures.ThreadPoolExecutor(max_workers = MULTI_TASKS) as executor:
            futures = [executor.submit(process_tile, index) for index in range(len(self.tiles))]
            for future in concurrent.futures.as_completed(futures):
                tile_index, result = future.result()
                tiles_list[tile_index] = result  # 按原始顺序填充结果

        result = json.dumps({'noCloud': {'tiles': tiles_list}})
        print(result)
        return result

