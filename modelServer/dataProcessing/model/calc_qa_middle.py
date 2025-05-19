import uuid
from osgeo import gdal
import json
import os
import requests
import concurrent.futures
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mband, convert_tif2cog, check_intersection, check_full_coverage_v2
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
import dataProcessing.config as config
import time

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"
MULTI_TASKS = os.cpu_count() - 3

class calc_qa_middle(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])

    # 预先异步加载所有 scene 数据
    def get_scene_dataSets(self, scenes):
        preloaded_scene_data = {}

        def load_scene(scene):
            scene_id = scene["sceneId"]
            scene_border_path = MINIO_ENDPOINT + "/" + scene['images'][0]['bucket'] + "/" + scene['images'][0]['tifPath']
            dataset = gdal.Open(scene_border_path)
            if dataset is None:
                raise Exception(f"无法打开数据集: {scene_border_path}")
            return scene_id, dataset

        with concurrent.futures.ThreadPoolExecutor() as executor:
            futures = [executor.submit(load_scene, scene) for scene in scenes]
            for future in concurrent.futures.as_completed(futures):
                scene_id, dataset = future.result()
                preloaded_scene_data[scene_id] = dataset

        return preloaded_scene_data

    def run(self):
        def get_first_element_or_empty(lst):
            return lst[0] if lst else ''

        print("calc_qa_middle run")
        gridHelper = GridHelper(self.resolution)  # 实例化
        tiles_list = [None] * len(self.tiles)  # 初始化 tiles_list，确保长度与 tiles 一致

        def process_scene(scene, bbox):
            if 'cloudPath' in scene:
                cloud_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                print("qa波段存在，开始计算云量")
                qa = calculate_cloud_coverage(cloud_path, scene['sensorName'], bbox)
            else:
                print("qa波段不存在，采用全景云量均值")
                qa = float(scene['cloud'])
            print("云量:", qa)
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
                print("最小云量值为：", min_qa)
                min_index = qas.index(min_qa)  # 获取云量列表中最小值索引
                print("最小云量的索引为：", min_index)
                images = self.scenes[min_index]['images']
                bandMapper = self.scenes[min_index]['bandMapper']
                bucket = images[0]['bucket']
                red_paths = [image["tifPath"] for image in images if image["band"] == bandMapper['Red']]
                green_paths = [image["tifPath"] for image in images if image["band"] == bandMapper['Green']]
                blue_paths = [image["tifPath"] for image in images if image["band"] == bandMapper['Blue']]
                red_path = get_first_element_or_empty(red_paths)
                green_path = get_first_element_or_empty(green_paths)
                blue_path = get_first_element_or_empty(blue_paths)
                print("三原色波段提取完成")

            return tile_index, {'colId': tile[0], 'rowId': tile[1], 'redPath': red_path, 'greenPath': green_path, 'bluePath': blue_path, 'bucket': bucket}

        with concurrent.futures.ThreadPoolExecutor(max_workers = MULTI_TASKS) as executor:
            futures = [executor.submit(process_tile, index) for index in range(len(self.tiles))]
            for future in concurrent.futures.as_completed(futures):
                tile_index, result = future.result()
                tiles_list[tile_index] = result  # 按原始顺序填充结果

        result = json.dumps({'noCloud': {'tiles': tiles_list}})
        print(result)
        return result

