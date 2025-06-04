from rio_tiler.io import COGReader, Reader
from dataProcessing.model.task import Task
import dataProcessing.config as config
import dataProcessing.Utils.cogUtils as cogUtils

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"

class calc_raster_point(Task):
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.point = self.args[0].get('point', [])
        self.raster = self.args[0].get('raster', [])

    def run(self):
        print("CalRasterPointTask run")

        [lon, lat] = self.point
        raster = self.raster

        url = MINIO_ENDPOINT + "/" + raster.get('bucket') + "/" + raster.get('tifPath')
        raster_context = COGReader(url)
        bounds = raster_context.dataset.bounds
        if cogUtils.ifPointContained(lon, lat, bounds):
            pointData = raster_context.point(lon, lat)
            pointValue = pointData.data[0].tolist()
        else:
            pointValue = None
        raster_context.close()
        return {
            "value": pointValue
        }

        
            

