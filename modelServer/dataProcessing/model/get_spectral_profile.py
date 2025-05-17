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

class get_spectral_profile(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.lnglat = self.args[0].get('point', [])
        self.lng = self.lnglat[0]
        self.lat = self.lnglat[1]
        self.images = self.args[0].get('images', [])

    def run(self):
        print("get_spectral_profile run")
        spectral_profile = list()
        print("获取数据epsg_code")
        epsg_code = get_tif_epsg(
            MINIO_ENDPOINT + "/" + self.images[0]['bucket'] + "/" + self.images[0]['tifPath'])
        print("开始转换坐标系")
        x, y = latlon_to_utm(self.lng, self.lat, epsg_code)
        print("开始获取光谱剖面")
        for image in self.images:
            path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
            value = int(get_pixel_value_at_utm(x, y, path))
            band = image['band']
            spectrum = {
                'band': band,
                'value': value
            }
            spectral_profile.append(spectrum)
        result = json.dumps({"spectral_profile": spectral_profile})
        print(result)
        return result

if __name__ == "__main__":
    data = {
    "point": [119.111, 25.222],
    "images": [
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_1.tif",
            "band": "1"
        },
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_2.tif",
            "band": "2"
        },
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_3.tif",
            "band": "3"
        },
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_4.tif",
            "band": "4"
        },
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_5.tif",
            "band": "5"
        },
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_6.tif",
            "band": "6"
        },
        {
            "bucket":"test-images",
            "path":"Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250217_20250226_02_T1/Band_7.tif",
            "band": "7"
        }
    ]
}




