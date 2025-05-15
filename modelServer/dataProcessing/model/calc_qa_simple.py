import uuid
from datetime import datetime
from osgeo import gdal
import json
import os

from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mband, convert_tif2cog, check_intersection
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
import dataProcessing.config as config


MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"


class calc_qa_simple(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])


    def run(self):
        # 6个瓦片，81秒
        print(datetime.now())
        print("calc_qa_simple run")
        gridHelper = GridHelper(self.resolution) # 实例化
        # 循环，针对每个瓦片进行操作
        tiles_list = []
        for index, tile in enumerate(self.tiles):
            # tile[0]为col，tile[1]为row
            gridCell = GridCell(tile[0], tile[1])
            bbox = gridHelper.get_grid_bbox(gridCell) # 计算bbox
            qas = list() # 用一个列表来保存每个景的云量
            print("开始检查相交状态")
            for scene in self.scenes:
                cloud_path = MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath']
                if check_intersection(cloud_path, bbox):
                    print("开始计算云量")
                    qa = calculate_cloud_coverage(cloud_path, bbox)
                    print("云量:", qa)
                    qas.append(qa)
                else:
                    qa = 9999
                    qas.append(qa)
            min_qa = min(qas)
            print("最小云量值为：", min_qa)
            min_index = qas.index(min_qa) # 获取云量列表中最小值索引
            print("最小云量的索引为：", min_index)
            images = self.scenes[min_index]['images']
            # 取出band为1的tifpath和bucket
            band1_tifpath = next((image['tifPath'] for image in images if image.get('band') == '1'), None)
            band1_bucket = next((image['bucket'] for image in images if image.get('band') == '1'), None)

            tiles_list.append({'colId':tile[0], 'rowId':tile[1], 'tifPath':band1_tifpath, 'bucket':band1_bucket})
        result = json.dumps({'noCloud':{'tiles':tiles_list}})
        print(result)
        print(datetime.now())
        return result