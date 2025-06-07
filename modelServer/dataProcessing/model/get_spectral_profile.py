import os
import uuid
from datetime import datetime
import json
import requests
from osgeo import gdal
from rio_tiler.io import COGReader, Reader
from concurrent.futures import ThreadPoolExecutor

from dataProcessing.Utils.mySqlUtils import select_tile_by_column_and_row, select_tile_by_column_and_row_v2
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import latlon_to_utm, get_pixel_value_at_utm
from dataProcessing.Utils.tifUtils import calculate_cloud_coverage, get_tif_epsg, convert_bbox_to_utm, parse_time_in_scene
from dataProcessing.Utils.gridUtil import GridHelper, GridCell
from dataProcessing.model.task import Task
import dataProcessing.Utils.cogUtils as cogUtils
from dataProcessing.config import current_config as CONFIG

MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
MULTI_TASKS = os.cpu_count() - 3

class get_spectral_profile(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.lnglat = self.args[0].get('point', [])
        self.lng = self.lnglat[0]
        self.lat = self.lnglat[1]
        self.images = self.args[0].get('images', [])
        


    def run(self):
        print("get_spectral_profile run")
        
        def get_pixel_from_image(image, lng, lat):
            path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
            raster_context = COGReader(path)
            pointData = raster_context.point(lng, lat)
            value = pointData.data[0].tolist()
            raster_context.close()

            return {
                'band': image['band'],
                'value': value
            }

        spectral_profile = []
        with ThreadPoolExecutor(max_workers=MULTI_TASKS) as executor:
            futures = [executor.submit(get_pixel_from_image, image, self.lng, self.lat) for image in self.images]
            for future in futures:
                try:
                    spectral_profile.append(future.result())
                except Exception as e:
                    print(f"Error reading image: {e}")
                    
        spectral_profile.sort(key=lambda x: int(x['band']) if isinstance(x['band'], str) and x['band'].isdigit() else x['band'])
        
        result = json.dumps({"spectral_profile": spectral_profile})
        # print(result)
        return result

    # def run(self):
    #     print("get_spectral_profile run")
    #     spectral_profile = list()

    #     for image in self.images:
    #         path = MINIO_ENDPOINT + "/" + image['bucket'] + "/" + image["tifPath"]
    #         # value = int(get_pixel_value_at_utm(x, y, path))
    #         raster_context = COGReader(path)
    #         bounds = raster_context.dataset.bounds

    #         pointData = raster_context.point(self.lng, self.lat)
    #         value = pointData.data[0].tolist()
    #         print(' value is :: ', value)

    #         raster_context.close()
            
    #         band = image['band']
    #         spectrum = {
    #             'band': band,
    #             'value': value
    #         }
    #         spectral_profile.append(spectrum)
    #     result = json.dumps({"spectral_profile": spectral_profile})
    #     print(result)
    #     return result

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




