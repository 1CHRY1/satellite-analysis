import json, os, time, math

import numpy as np

from shapely.geometry import shape, box

from rio_tiler.io import COGReader
from rasterio.transform import from_bounds
import rasterio

from dataProcessing.model.task import Task
import dataProcessing.config as config

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"

class CalRasterPointTask(Task):
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.point = self.args[0].get('point', [])
        self.resolution = self.args[0].get('rester', [])

    def run(self):
        print("CalRasterPointTask run")

        [lon, lat] = self.point
        raster = self.raster

        url = MINIO_ENDPOINT + "/" + raster.get('bucket') + raster.get('tifPath')
        raster_context = COGReader(url)

        pointValue = raster_context.point(lon, lat)

        return {
            "value": pointValue
        }

        
            

