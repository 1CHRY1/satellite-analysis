import uuid
from datetime import datetime
from osgeo import gdal
import json
import os
import requests

from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mband, convert_tif2cog, check_intersection
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
import dataProcessing.config as config


MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"


class calc_qa_middle(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])


    def run(self):
        print(datetime.now())
        print("calc_qa_middle run")
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
            bandMapper = self.scenes[min_index]['bandMapper']
            # # 挑选波段为1,3,4  适用MODIS
            # images = [image for image in images if image['band'] in (bandMapper['Red'], bandMapper['Green'], bandMapper['Blue'])]
            # sceneId = self.scenes[min_index]['sceneId']
            # print("需要融合波段图像：", images)
            # print("准备融合多波段影像")
            # output_name = 'mband_' + sceneId + '.tif'  # 融合景的多波段生成的文件名，上传后也叫这个名字
            # output_file = os.path.join(config.TEMP_OUTPUT_DIR, output_name)  # 组成文件存储路径
            # # if os.path.exists(output_file):
            # response = requests.head(MINIO_ENDPOINT + '/' + config.MINIO_TEMP_FILES_BUCKET + '/' + output_name, timeout=5)  # 判断该景是否已经融合
            # if response.status_code == 200:
            #     print("已有融合后影像，直接调用")
            #     object_name = output_name
            # else:
            #     print("开始融合多波段影像")
            #     mband_output_file = mband(images, config.TEMP_OUTPUT_DIR, output_name=output_name)
            #     output_file_path = convert_tif2cog(mband_output_file)
            #     object_name = output_name
            #     print("多波段影像融合完成")
            #     uploadLocalFile(output_file_path, config.MINIO_TEMP_FILES_BUCKET, object_name)
            # tiles_list.append({'colId':tile[0], 'rowId':tile[1], 'tifPath':object_name, 'bucket':config.MINIO_TEMP_FILES_BUCKET})

            # 不需要融合，返回RGB波段即可
            red_path = [image["tifPath"] for image in images if image["band"] == bandMapper['Red']]
            green_path = [image["tifPath"] for image in images if image["band"] == bandMapper['Green']]
            blue_path = [image["tifPath"] for image in images if image["band"] == bandMapper['Blue']]
            if len(images) < 3:
                print(f"三原色波段缺失，只有{len(images)}个波段")
                continue
            print("三原色波段提取完成")
            tiles_list.append({'colId': tile[0], 'rowId': tile[1], 'redPath': red_path, 'greenPath': green_path, 'bluePath': blue_path,
                               'bucket': images[0]['bucket']})
        result = json.dumps({'noCloud':{'tiles':tiles_list}})
        print(result)
        print(datetime.now())
        return result
