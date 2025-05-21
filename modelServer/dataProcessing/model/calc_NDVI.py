import os
import uuid
from datetime import datetime
import json
import requests
from osgeo import gdal
import concurrent.futures

from dataProcessing.Utils.mySqlUtils import select_tile_by_column_and_row, select_tile_by_column_and_row_v2
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import latlon_to_utm, get_pixel_value_at_utm
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm, parse_time_in_scene
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
import dataProcessing.config as config

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"
MULTI_TASKS = os.cpu_count() - 3

class calc_NDVI(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.lnglat = self.args[0].get('point', [])
        self.lng = self.lnglat[0]
        self.lat = self.lnglat[1]
        self.scenes = self.args[0].get('scenes', [])

    # def run(self):
    #     print("calc_NDVI run")
    #     # 按sceneTime排序
    #     scenes = sorted(self.scenes, key=parse_time_in_scene)
    #     # scenes = sorted(self.scenes, key=lambda scene: scene["sceneTime"])
    #     sceneTime_list = []
    #     NDVI_list = []
    #     data = []
    #     for scene in scenes:
    #         epsg_code = get_tif_epsg(MINIO_ENDPOINT + "/" + scene['images'][0]['bucket'] + "/" + scene['images'][0]['tifPath'])
    #         x, y = latlon_to_utm(self.lng, self.lat, epsg_code)

    #         bandMapper = scene['bandMapper']
    #         Red_path = ''
    #         NIR_path = ''
    #         for image in scene["images"]:
    #             if image["band"] == bandMapper['Red']:
    #                 Red_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
    #                 break
    #         for image in scene["images"]:
    #             if image["band"] == bandMapper['NIR']:
    #                 NIR_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
    #                 break
    #         # 只要有1个路径不存在，NDVI被置为Nan
    #         if not Red_path or not NIR_path:
    #             # NDVI = 'Nan'
    #             NDIV = 0
    #         else:
    #             Red = int(get_pixel_value_at_utm(x, y, Red_path))
    #             NIR = int(get_pixel_value_at_utm(x, y, NIR_path))
    #             if NIR == 0 and Red == 0:
    #                 # NDVI = 'Nan'
    #                 NDIV = 0
    #             else:
    #                 NDVI = (NIR - Red) / (NIR + Red)
    #         sceneTime_list.append(scene['sceneTime'])
    #         NDVI_list.append(NDVI)
    #         data = [{"sceneTime": sceneTime, "value": value} for sceneTime, value in zip(sceneTime_list, NDVI_list)]
    #     NDVI_result = json.dumps({"NDVI": data}, indent=4)
    #     print(NDVI_result)
    #     return NDVI_result

    def run(self):

        def process_scene(scene, lng, lat, MINIO_ENDPOINT): 
            try:
                epsg_code = get_tif_epsg(MINIO_ENDPOINT + "/" + scene['images'][0]['bucket'] + "/" + scene['images'][0]['tifPath'])
                x, y = latlon_to_utm(lng, lat, epsg_code)

                bandMapper = scene['bandMapper']
                Red_path = None
                NIR_path = None

                NDVI = 0.0

                # 查找Red和NIR波段的路径
                for image in scene["images"]:
                    if image["band"] == bandMapper['Red']:
                        Red_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
                    elif image["band"] == bandMapper['NIR']:
                        NIR_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]

                # 如果缺少Red或NIR路径，返回None
                if not Red_path or not NIR_path:
                    print(f"Skipping scene {scene['sceneTime']} due to missing Red or NIR path.")
                    return {"sceneTime": scene['sceneTime'], "value": NDVI}

                # 获取像素值
                Red = get_pixel_value_at_utm(x, y, Red_path)
                NIR = get_pixel_value_at_utm(x, y, NIR_path)

                # 确保Red和NIR都是有效值
                if Red is None or NIR is None or Red == 0 and NIR == 0:
                    print(f"Skipping scene {scene['sceneTime']} due to invalid Red or NIR values.")
                    return {"sceneTime": scene['sceneTime'], "value": NDVI}

                # 计算NDVI
                try:
                    NDVI = (NIR - Red) / (NIR + Red)
                except ZeroDivisionError:
                    print(f"Skipping scene {scene['sceneTime']} due to division by zero.")
                    return {"sceneTime": scene['sceneTime'], "value": NDVI}

                # 返回有效结果
                return {"sceneTime": scene['sceneTime'], "value": NDVI}

            except Exception as e:
                print(f"Error processing scene {scene['sceneTime']}: {e}")
                return 0.0

        print("calc_NDVI run")
        # 按sceneTime排序
        scenes = sorted(self.scenes, key=parse_time_in_scene)

        # 使用ThreadPoolExecutor并发处理每个场景
        with concurrent.futures.ThreadPoolExecutor(max_workers = MULTI_TASKS) as executor:
            # 提交任务到线程池
            futures = [executor.submit(process_scene, scene, self.lng, self.lat, MINIO_ENDPOINT) for scene in scenes]

            # 收集结果
            results = []
            for future in concurrent.futures.as_completed(futures):
                result = future.result()
                if result is not None:
                    results.append(result)

        # 如果所有场景都被跳过，返回空结果
        if not results:
            print("No valid NDVI data available.")
            return json.dumps({"NDVI": []}, indent=4)

        # 构建最终结果
        NDVI_result = json.dumps({"NDVI": results}, indent=4)
        print(NDVI_result)
        return NDVI_result



if __name__ == "__main__":
    data_root_path = 'D:\\IdeaProjects\\test\\'
    # lng, lat = [122.4, 31.5]
    data = {
        "point":[122.4, 31.5],
        "scenes": [
            {
                "sceneId":"SC153032082",
                "sceneTime":2,
                "cloudPath":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_QA_PIXEL.TIF",
                "images": [
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B4.TIF",
                        "band": "4"
                    },
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240320_20240402_02_T1_SR_B5.TIF",
                        "band": "5"
                    }
                ]
            },
            {
                "sceneId":"SC155541969",
                "sceneTime": 1,
                "cloudPath":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_QA_PIXEL.TIF",
                "images": [
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B4.TIF",
                        "band": "4"
                    },
                    {
                        "path":"D:\\IdeaProjects\\test\\LC08_L2SP_118038_20240928_20241005_02_T1_SR_B5.TIF",
                        "band": "5"
                    }
                ]
            }
        ]
    }
    calc_NDVI = calc_NDVI('1', data)
    result = calc_NDVI.run()
    print(result)
