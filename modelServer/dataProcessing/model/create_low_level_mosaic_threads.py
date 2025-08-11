# dataProcessing/model/create_low_level_mosaic.py

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


class create_low_level_mosaic(Task):
    """低层级镶嵌任务类"""
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        # 从参数中提取配置
        self.config = args[0] if args else kwargs.get('config', {})
        
        # 使用系统配置的默认值，允许请求参数覆盖
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
        """执行镶嵌任务"""
        print(f"[LowLevelMosaicTask] 任务 {self.task_id} 开始执行")
        print(f"[LowLevelMosaicTask] 参数: sensor_name={self.sensor_name}, grid_res={self.grid_res}, crs={self.crs}")
        try:
            # 验证必要参数
            if not self.email or not self.password:
                return {
                    'success': False,
                    'message': '缺少必要的登录凭据',
                    'error': 'Missing email or password',
                    'task_id': self.task_id
                }
            
            # 使用配置文件中的Ray设置初始化
            if not ray.is_initialized():
                print(f"[LowLevelMosaicTask] 任务 {self.task_id}: Ray未初始化，正在初始化...")
                ray.init(
                    num_cpus=CONFIG.RAY_NUM_CPUS,
                    object_store_memory=CONFIG.RAY_OBJECT_STORE_MEMORY,
                    ignore_reinit_error=True
                )
                print(f"[LowLevelMosaicTask] 任务 {self.task_id}: Ray已初始化，CPU数: {CONFIG.RAY_NUM_CPUS}")
            else:
                print(f"[LowLevelMosaicTask] 任务 {self.task_id}: Ray已经初始化，跳过初始化")
            
            # 创建场景获取器并登录
            fetcher = SceneFetcher(email=self.email, password=self.password)
            fetcher.login()
            
            # 获取格网数据
            grids_data = fetcher.get_grids(region_id=self.region_id, resolution=self.grid_res)
            print(f"任务 {self.task_id}: 网格总数：{len(grids_data)}")

            # 提交查询
            fetcher.submit_query(
                start_time=self.start_time, 
                end_time=self.end_time, 
                region_id=self.region_id, 
                resolution=self.grid_res
            )
            
            # 并行处理所有格网
            start = time.time()
            futures = [self._process_grid_remote.remote(
                grid, self.sensor_name, fetcher, self.crs, self.z_level
            ) for grid in grids_data]
            
            results = ray.get(futures)
            processing_time = time.time() - start
            print(f"任务 {self.task_id}: 所有格网处理完成，耗时: {processing_time:.2f} 秒")
            
            # 过滤出成功的结果
            successful_results = [result for result in results if result is not None]
            
            print(f"任务 {self.task_id}: 成功处理的COG文件: {len(successful_results)} 个")
            
            if not successful_results:
                return {
                    'success': False,
                    'message': '没有成功生成的COG文件',
                    'mosaicjson_path': None,
                    'processing_time': processing_time,
                    'cog_count': 0,
                    'task_id': self.task_id
                }

            # 创建和上传MosaicJSON - 使用配置文件中的MinIO设置
            print(f"任务 {self.task_id}: 正在生成 MosaicJSON")
            
            minio_client = Minio(
                f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}",
                access_key=CONFIG.MINIO_ACCESS_KEY,
                secret_key=CONFIG.MINIO_SECRET_KEY,
                secure=CONFIG.MINIO_SECURE
            )
            
            bucket = CONFIG.MINIO_TEMP_FILES_BUCKET
            minio_dir = "national-mosaicjson"
            mosaic_output_path = f"{minio_dir}/mosaic_{self.task_id}.json"
            
            # 使用元数据直接创建MosaicJSON
            start_mosaic = time.time()
            mosaic_definition = self._create_mosaicjson_from_metadata(
                successful_results, 
                bucket,
                quadkey_zoom=self.quadkey_zoom
            )
            mosaic_creation_time = time.time() - start_mosaic
            print(f"任务 {self.task_id}: MosaicJSON创建耗时: {mosaic_creation_time:.2f} 秒")
            
            # 上传MosaicJSON
            upload_success = self._upload_mosaicjson(
                minio_client, bucket, mosaic_definition, mosaic_output_path
            )
            
            if upload_success:
                full_mosaicjson_path = f"minio://{bucket}/{mosaic_output_path}"
                mosaicjson_url = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{bucket}/{mosaic_output_path}"
                print(f"任务 {self.task_id}: MosaicJSON 路径: {full_mosaicjson_path}")
                
                return {
                    # 'success': True,
                    # 'message': '镶嵌任务完成',
                    'mosaicjson_path': full_mosaicjson_path,
                    'mosaicjson_url': mosaicjson_url,
                    'processing_time': processing_time,
                    'mosaic_creation_time': mosaic_creation_time,
                    'cog_count': len(successful_results),
                    'tile_count': len(mosaic_definition['tiles']),
                    'bounds': mosaic_definition['bounds'],
                    'task_id': self.task_id,
                    # 'config_profile': 'zzw'  # 标识使用的配置
                }
            else:
                return {
                    # 'success': False,
                    # 'message': 'MosaicJSON上传失败',
                    'mosaicjson_path': None,
                    'processing_time': processing_time,
                    'cog_count': len(successful_results),
                    'task_id': self.task_id
                }
                
        except Exception as e:
            print(f"任务 {self.task_id} 执行失败: {e}")
            import traceback
            traceback.print_exc()
            return {
                # 'success': False,
                'message': f'任务执行失败: {str(e)}',
                'mosaicjson_path': None,
                'error': str(e),
                'task_id': self.task_id
            }

    @staticmethod
    @ray.remote
    def _process_grid_remote(grid, sensor_name, fetcher, crs, z_level):
        """远程处理单个格网的函数"""
        try:
            scenes = fetcher.get_scenes_for_grid(sensor_name, grid['coordinates'][0])
            if len(scenes) > 0:
                print(f"处理中... Grid {grid['rowId']}-{grid['columnId']} 包含 {len(scenes)} 个场景")
                grid_mosaic = GridMosaic(
                    grid['coordinates'][0],
                    scenes,
                    crs_id=crs,
                    z_level=z_level,
                    per_grid_workers=10  # 或从配置读取
                )
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
                    print(f"❌ Grid {grid['rowId']}-{grid['columnId']} 镶嵌失败")
                    return None
            else:
                print(f"⚠️ Grid {grid['rowId']}-{grid['columnId']} 没有找到场景")
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
        """根据COG元数据列表直接创建MosaicJSON"""
        minio_base_url = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
        
        # 初始化瓦片映射
        tiles_map = {}
        
        # 计算全局边界
        all_bounds = [item['bounds'] for item in cog_metadata_list if item.get('bounds')]
        if not all_bounds:
            raise ValueError("没有有效的边界信息")
        
        global_west = min(bounds[0] for bounds in all_bounds)
        global_south = min(bounds[1] for bounds in all_bounds)
        global_east = max(bounds[2] for bounds in all_bounds)
        global_north = max(bounds[3] for bounds in all_bounds)
        global_bounds = [global_west, global_south, global_east, global_north]
        
        print(f"任务 {self.task_id}: 全局边界: {global_bounds}")
        print(f"任务 {self.task_id}: 使用 quadkey_zoom 级别: {quadkey_zoom}")
        
        # 使用指定的quadkey_zoom级别创建瓦片映射
        for item in cog_metadata_list:
            if not item.get('bounds'):
                continue
                
            bounds = item['bounds']
            file_url = f"{minio_base_url}/{bucket_name}/{item['path']}"
            
            # 获取该边界在quadkey_zoom级别下覆盖的瓦片
            tile_coords = self._bounds_to_tiles(bounds, quadkey_zoom)
            
            for x, y, z in tile_coords:
                # 转换为quadkey格式
                quadkey = self._tile_to_quadkey(x, y, z)
                if quadkey not in tiles_map:
                    tiles_map[quadkey] = []
                tiles_map[quadkey].append(file_url)
        
        # 创建标准的MosaicJSON结构
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
        """将MosaicJSON上传到MinIO"""
        try:
            mosaic_json_string = json.dumps(mosaic_definition, indent=4)
            mosaic_bytes = mosaic_json_string.encode('utf-8')
            
            # 检查存储桶是否存在
            found = minio_client.bucket_exists(bucket_name)
            if not found:
                minio_client.make_bucket(bucket_name)
                print(f"任务 {self.task_id}: 创建存储桶 '{bucket_name}'")
            
            # 上传到 MinIO
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
            print(f"任务 {self.task_id}: 上传 MosaicJSON 失败: {e}")
            import traceback
            traceback.print_exc()
            return False