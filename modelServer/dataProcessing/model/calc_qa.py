import uuid
from datetime import datetime
from osgeo import gdal

from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mband, convert_tif2cog
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
        print("calc_qa run")
        gridHelper = GridHelper(self.resolution) # 实例化
        # 循环，针对每个瓦片进行操作
        warp_file_list = []
        for index, tile in enumerate(self.tiles):
            gridCell = GridCell(tile[0], tile[1])
            bbox = gridHelper.get_grid_bbox(gridCell) # 计算bbox
            qas = list() # 用一个列表来保存每个景的云量
            for scene in self.scenes:
                qa = calculate_cloud_coverage(MINIO_ENDPOINT + "/" + scene['bucket'] + "/" + scene['cloudPath'], bbox)
                qas.append(qa)
            min_qa = min(qas)
            min_index = qas.index(min_qa) # 获取云量列表中最小值索引
            images = self.scenes[min_index]['images']
            tif_paths = list() # 记录需要融合的tif路径
            for image in images:
                tif_paths.append(MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image['tifPath'])
            # 需要将wgs 84 转成 utm，才能正常裁剪范围
            epsg_code = get_tif_epsg(tif_paths[0])
            bbox = convert_bbox_to_utm(bbox, epsg_code)
            output_file = mband(images, config.TEMP_OUTPUT_DIR, output_name='mband' + str(index) + '.tif')
            warp_file = config.TEMP_OUTPUT_DIR + '\\mtif' + str(index) + '.tif'
            gdal.Warp(
                warp_file,
                output_file,
                outputBounds=bbox
            )
            print(f'裁剪影像已保存至{warp_file}')
            warp_file_list.append(warp_file)
        result_file = config.TEMP_OUTPUT_DIR + '\\mtif.tif'
        gdal.Warp(
            result_file,
            warp_file_list,
        )
        print(f'影像镶嵌完毕，已保存至{result_file}')
        output_file_path = convert_tif2cog(result_file)
        object_name = f"{datetime.now().strftime('%Y-%m/%d')}/{uuid.uuid4()}.tif"
        uploadLocalFile(output_file_path, config.MINIO_TEMP_FILES_BUCKET, object_name)
        print(f'文件已上传至{config.MINIO_TEMP_FILES_BUCKET + '/' + object_name}')
        return config.MINIO_TEMP_FILES_BUCKET + '/' + object_name

if __name__ == "__main__":
    data_root_path = 'D:\\IdeaProjects\\test\\'
    data = {
        "resolution": 1,
        "cloud": 10,
        "tiles": [
            [33626,6508],
            [33626,6509]
        ],
        "scenes": [
            {
                "sceneId":"SC153032082",
                "cloudPath":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_QA_PIXEL.TIF",
                "images": [
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B1.TIF",
                        "band": "1"
                    },
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B2.TIF",
                        "band": "2"
                    },
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B2.TIF",
                        "band": "3"
                    }
                ]
            },
            {
                "sceneId":"SC155541969",
                "cloudPath":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_QA_PIXEL.TIF",
                "images": [
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B1.TIF",
                        "band": "1"
                    },
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B2.TIF",
                        "band": "2"
                    },
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B3.TIF",
                        "band": "3"
                    }
                ]
            }
        ]
    }
    calc_qa = calc_qa('1', data)
    result = calc_qa.run()
    print(result)

