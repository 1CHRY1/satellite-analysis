import os, shutil, glob, time, json
from functools import partial
from multiprocessing import Pool, cpu_count
import rasterio
from rio_tiler.io import COGReader
import numpy as np
from rasterio.transform import from_bounds
from rasterio.warp import transform_bounds
from rasterio.enums import Resampling
from shapely.geometry import shape, box
from rasterio.merge import merge
from rio_cogeo.profiles import cog_profiles
from rio_cogeo.cogeo import cog_translate, cog_info
from concurrent.futures import ThreadPoolExecutor, as_completed
import ray
from dataProcessing.Utils.osUtils import uploadLocalFile, uploadMemFile
from dataProcessing.Utils.gridUtil import GridHelper
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG
import requests
# from pyproj import Transformer
from rasterio.shutil import delete as rio_delete
from rasterio.io import MemoryFile


MINIO_ENDPOINT = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
INFINITY = 999999

class calc_no_cloud(Task):

    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])
        self.bandList = self.args[0].get('bandList', ['Red', 'Green', 'Blue'])

        def run(self):
            print("NoCloudGraphTask run", flush=True)
            print('start time ', time.time())

            grids = self.tiles
            gridResolution = self.resolution
            scenes = self.scenes
            bandList = self.bandList

            grid_helper = GridHelper(gridResolution)

            scene_band_paths = {}
            for scene in scenes:
                mapper = scene['bandMapper']
                bands = {band: None for band in bandList}
                