from rio_tiler.io import COGReader, Reader
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG
import dataProcessing.Utils.cogUtils as cogUtils

# 使用函数获取MINIO_ENDPOINT
def get_minio_endpoint():
    try:
        return f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
    except:
        return "http://localhost:9000"  # 默认值

MINIO_ENDPOINT = None  # 初始化为None，将在需要时获取

class calc_raster_point(Task):
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.point = self.args[0].get('point', [])
        self.raster = self.args[0].get('raster', [])

    def run(self):
        print("CalRasterPointTask run")
        
        global MINIO_ENDPOINT
        if MINIO_ENDPOINT is None:
            MINIO_ENDPOINT = get_minio_endpoint()

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

        
            

