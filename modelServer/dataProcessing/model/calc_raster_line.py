from multiprocessing import Pool, cpu_count

from rio_tiler.io import COGReader
from rasterio.transform import from_bounds

from dataProcessing.model.task import Task
import dataProcessing.config as config
import dataProcessing.Utils.cogUtils as cogUtils

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"

class calc_raster_line(Task):
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.points = self.args[0].get('points', [])
        self.rasters = self.args[0].get('rasters', [])

    @staticmethod
    def process_point(lon, lat, raster_urls):
        for raster_url in raster_urls:
            try:
                raster_cog = COGReader(raster_url)
                bounds = raster_cog.dataset.bounds
                if cogUtils.ifPointContained(lon, lat, bounds):
                    pointData = raster_cog.point(lon, lat)
                    pointValue = pointData['values'][0] if pointData and 'values' in pointData else None
                    return pointValue
            except Exception as e:
                print(e)
            return None


    def run(self):

        print("CalRasterLineTask run")

        points = self.points
        rasters = self.rasters

        result = []
        raster_urls = [
            f"{MINIO_ENDPOINT}/{raster.get('bucket')}/{raster.get('tifPath')}"
            for raster in rasters
        ]

        tasks = [(lon, lat, raster_urls) for [lon, lat] in points]

        with Pool(processes=cpu_count()) as pool:
            result = pool.map(self.process_point, tasks)

        return result

        
            

