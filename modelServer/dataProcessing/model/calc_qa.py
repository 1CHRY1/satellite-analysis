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


class calc_qa(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])


    def run(self):
        # 5个瓦片，158秒
        print("calc_qa run")
        gridHelper = GridHelper(self.resolution) # 实例化
        # 循环，针对每个瓦片进行操作
        warp_file_list = []
        for index, tile in enumerate(self.tiles):
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
            images = [image for image in images if image['band'] in ("2", "3", "4")]
            sceneId = self.scenes[min_index]['sceneId']
            print("需要融合波段图像：", images)
            # 需要将wgs 84 转成 utm，才能正常裁剪范围
            epsg_code = get_tif_epsg(MINIO_ENDPOINT + "/" + self.scenes[min_index]['bucket'] + "/" + images[0]["tifPath"])
            bbox = convert_bbox_to_utm(bbox, epsg_code)
            print("准备融合多波段影像")
            output_name = 'mband_' + sceneId + '.tif'   # 融合景的多波段生成的文件名
            output_file = os.path.join(config.TEMP_OUTPUT_DIR, output_name)  # 组成文件存储路径
            if os.path.exists(output_file):
                print("已有融合后影像，直接调用")
                mband_output_file = output_file
            else:
                print("开始融合多波段影像")
                mband_output_file = mband(images, config.TEMP_OUTPUT_DIR, output_name=output_name)
            warp_file = config.TEMP_OUTPUT_DIR + '\\mtif' + str(index) + '.tif'
            print("开始按瓦片范围裁剪:", bbox)
            gdal.Warp(
                warp_file,
                mband_output_file,
                outputBounds=bbox
            )
            print(f'裁剪影像已保存至{warp_file}')
            warp_file_list.append(warp_file)
        result_file = config.TEMP_OUTPUT_DIR + '\\mtif.tif'
        print(f"开始影像镶嵌，共{index+1}个瓦片")
        gdal.Warp(
            result_file,
            warp_file_list,
        )
        print(f'影像镶嵌完毕，已保存至{result_file}')
        output_file_path = convert_tif2cog(result_file)
        object_name = f"{datetime.now().strftime('%Y-%m/%d')}/{uuid.uuid4()}.tif"
        uploadLocalFile(output_file_path, config.MINIO_TEMP_FILES_BUCKET, object_name)
        print(f"文件已上传至{config.MINIO_TEMP_FILES_BUCKET + '/' + object_name}")
        print(json.dumps({"bucket": config.MINIO_TEMP_FILES_BUCKET, "tifPath": object_name}))
        return json.dumps({"bucket": config.MINIO_TEMP_FILES_BUCKET, "tifPath": object_name})

# if __name__ == "__main__":
#     data_root_path = 'D:\\IdeaProjects\\test\\'
#     data = {
#         "resolution": 1,
#         "cloud": 10,
#         "tiles": [
#             [33626,6508],
#             [33626,6509]
#         ],
#         "scenes": [
#             {
#                 "sceneId":"SC153032082",
#                 "cloudPath":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_QA_PIXEL.TIF",
#                 "images": [
#                     {
#                         "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B1.TIF",
#                         "band": "1"
#                     },
#                     {
#                         "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B2.TIF",
#                         "band": "2"
#                     },
#                     {
#                         "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B2.TIF",
#                         "band": "3"
#                     }
#                 ]
#             },
#             {
#                 "sceneId":"SC155541969",
#                 "cloudPath":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_QA_PIXEL.TIF",
#                 "images": [
#                     {
#                         "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B1.TIF",
#                         "band": "1"
#                     },
#                     {
#                         "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B2.TIF",
#                         "band": "2"
#                     },
#                     {
#                         "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B3.TIF",
#                         "band": "3"
#                     }
#                 ]
#             }
#         ]
#     }
#     calc_qa = calc_qa('1', data)
#     result = calc_qa.run()
#     print(result)

