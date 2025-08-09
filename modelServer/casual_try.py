import ray
import time
import json
import io
from minio import Minio
from dataProcessing.Utils.mosaic.scene_fetcher import SceneFetcher
from dataProcessing.Utils.mosaic.grid_mosaic import GridMosaic
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG
import mercantile
from rasterio.crs import CRS

class LowLevelMosaicTask(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)

        self.config = args[0] if args else kwargs.get('config', {})

        self.grid_res = self.config.get('grid_res', CONFIG.MOSAIC_DEFAULT_GRID_RES)
        self.crs = self.config.get('crs', CONFIG.MOSAIC_DEFAULT_CRS)
        self.z_level = self.config.get('z_level', CONFIG.MOSAIC_DEFAULT_Z_LEVEL)
        self.start_time = self.config.get('start_time', CONFIG.MOSAIC_DEFAULT_START_TIME)
        self.end_time = self.config.get('end_time', CONFIG.MOSAIC_DEFAULT_END_TIME)
        self.region_id = self.config.get('region_id', CONFIG.MOSAIC_DEFAULT_REGION_ID)
        self.sensor_name = self.config.get('sensor_name', CONFIG.MOSAIC_DEFAULT_SENSOR_NAME)
        self.email = self.config.get('email')
        self.password = self.config.get('password')
        self.quadkey_zoom = self.config.get('quadkey_zoom', CONFIG.MOSAIC_DEFAULT_QUADKEY_ZOOM)

    def run(self):
        try:
            if not self.email or not self.password:
                return {
                    'success': False,
                    'message': '缺少必要的登录凭据',
                    'error': 'Missing email or password',
                    'task_id': self.task_id
                }
            
            if not ray.is_initialized():
                ray.init(
                    num_cpus=CONFIG.RAY_NUM_CPUS,
                    object_store_memory=CONFIG.RAY_OBJECT_STORE_MEMORY,
                    ignore_reinit_error=True
                )
            else:
                print(f"Ray已经初始化，跳过初始化")
            
        except Exception as e:
            import traceback
            traceback.print_exc()
            return {
                'success': False,
                'message': f'任务执行失败: {str(e)}',
                'mosaicjson_path': None,
                'error': str(e),
                'task_id': self.task_id
            }

    @staticmethod
    @ray.remote
    def _process_grid_remote(grid, sensor_name, fetcher, crs, z_level):
        try:
            scenes = fetcher.get_scenes_for_grid(sensor_name, grid['coordinates'][0])
            if len(scenes) > 0:
                grid_mosaic = GridMosaic(grid['coordinates'][0], scenes, crs_id=crs, z_level=z_level)
                result = grid_mosaic.create_mosaic_with_metadata()

                if result:
                    minio_path, bounds, crs_info = result
                    return {
                        'path': minio_path,
                        'bounds': bounds,
                        'crs': crs_info,
                        'grid_coords': grid['coordinates'][0]
                    }
                else:
                    return None
            else:
                return None
        except Exception as e:
            print(f"❌ Error in grid {grid['rowId']}-{grid['columnId']}: {e}")
            import traceback
            traceback.print_exc()
            return None

    
    def _bounds_to_tiles(self, bounds, zoom_level):
        """将地理边界转换为瓦片坐标列表"""
        west, south, east, north = bounds
        tiles = list(mercantile.tiles(west, south, east, north, zoom_level))
        return [(tile.x, tile.y, tile.z) for tile in tiles]

    def _tile_to_quadkey(self, x, y, z):
        """将瓦片坐标转换为quadkey格式"""
        quadkey = ""
        for i in range(z, 0, -1):
            digit = 0
            mask = 1 << (i - 1)
            if (x & mask) != 0:
                digit += 1
            if (y & mask) != 0:
                digit += 2
            quadkey += str(digit)
        return quadkey

    def _create_mosaicjson_from_metadata(self, cog_metadata_list, bucket_name, quadkey_zoom=8):
        minio_base_url = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"

        tiles_map = {}

        all_bounds = [item['bounds'] for item in cog_metadata_list if item.get('bounds')]
        if not all_bounds:
            raise ValueError("没有有效的边界信息")
        
        global_west = min(bounds[0] for bounds in all_bounds)
        global_south = min(bounds[1] for bounds in all_bounds)
        global_east = max(bounds[2] for bounds in all_bounds)
        global_north = max(bounds[3] for bounds in all_bounds)
        global_bounds = [global_west, global_south, global_east, global_north]

        for item in cog_metadata_list:
            if not item.get('bounds'):
                continue
                
            bounds = item['bounds']
            file_url == f"{minio_base_url}/{bucket_name}/{item['path']}"

            tile_coords = self._bounds_to_tiles(bounds, quadkey_zoom)

            for x, y, z in tile_coords:
                quadkey = self._tile_to_quadkey(x, y, z)
                if quadkey not in tiles_map:
                    tiles_map[quadkey] = []
                tiles_map[quadkey].append(file_url)

        mosaic_definition = {
            "mosaicjson": "0.0.3",
            "name": f"Mosaic_{self.task_id}",
            "description": f"Generated mosaic for task {self.task_id} using zzwConfig",
            "version": "1.0.0",
            "attribution": None,
            "minzoom": quadkey_zoom,
            "maxzoom": quadkey_zoom,
            "quadkey_zoom": quadkey_zoom,
            "bounds": global_bounds,
            "center": [
                (global_west + global_east) / 2,
                (global_south + global_north) / 2,
                quadkey_zoom
            ],
            "tiles": tiles_map,
            "tilematrixset": None,
            "asset_type": None,
            "asset_prefix": None,
            "data_type": None,
            "colormap": None,
            "layers": None
        }

        return mosaic_definition

    def _upload_mosaicjson(self, minio_client, bucket_name, mosaic_definition, output_object_name):
        try:
            mosaic_json_string = json.dumps(mosaic_definition, indent=4)
            mosaic_bytes = mosaic_json_string.encode('utf-8')

            found = minio_client.bucket_exists(bucket_name)
            if not found:
                minio_client.make_bucket(bucket_name)
                print(f"任务 {self.task_id}: 创建存储桶 '{bucket_name}'")

             minio_client.put_object(
                bucket_name=bucket_name,
                object_name=output_object_name,
                data=io.BytesIO(mosaic_bytes),
                length=len(mosaic_bytes),
                content_type='application/json'
            )

            print(f"任务 {self.task_id}: MosaicJSON 成功上传至: minio://{bucket_name}/{output_object_name}")
            return True

        except Exception as e:
            return False