import os
import uuid
from datetime import datetime
import json
import requests
from osgeo import gdal

from dataProcessing.Utils.mySqlUtils import select_tile_by_column_and_row, select_tile_by_column_and_row_v2
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import latlon_to_utm, get_pixel_value_at_utm
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm, parse_time_in_scene
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
import dataProcessing.config as config

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"


class calc_NDVI(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.lnglat = self.args[0].get('point', [])
        self.lng = self.lnglat[0]
        self.lat = self.lnglat[1]
        self.scenes = self.args[0].get('scenes', [])

    def run(self):
        print("calc_NDVI run")
        # 按sceneTime排序
        scenes = sorted(self.scenes, key=parse_time_in_scene)
        # scenes = sorted(self.scenes, key=lambda scene: scene["sceneTime"])
        NDVI_list = []
        for scene in scenes:
            epsg_code = get_tif_epsg(MINIO_ENDPOINT + "/" + scene['images'][0]['bucket'] + "/" + scene['images'][0]['tifPath'])
            x, y = latlon_to_utm(self.lng, self.lat, epsg_code)
            # 以landsat8为例，波段4为红光，波段5为近红外
            band_4_path = ''
            band_5_path = ''
            for image in scene["images"]:
                if image["band"] == "4":
                    band_4_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
                    break
            for image in scene["images"]:
                if image["band"] == "5":
                    band_5_path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
                    break
            Red = int(get_pixel_value_at_utm(x, y, band_4_path))
            NIR = int(get_pixel_value_at_utm(x, y, band_5_path))
            if NIR == 0 and Red == 0:
                NDVI = 'Nan'
            else:
                NDVI = (NIR - Red) / (NIR + Red)
            NDVI_list.append(NDVI)
        print({"NDVI": NDVI_list})
        return json.dumps({"NDVI": NDVI_list})



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
