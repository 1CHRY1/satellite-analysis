import numpy as np
import rasterio
from minio import Minio
from rasterio.io import MemoryFile
from rasterio.enums import Resampling
import random
from rio_tiler.io import COGReader
from rio_tiler.models import ImageData
from rasterio.merge import merge
from rio_tiler.colormap import cmap
import io
from rasterio.crs import CRS
from rio_cogeo.cogeo import cog_translate
from rio_cogeo.profiles import cog_profiles
from dataProcessing.config import current_config as CONFIG
from concurrent.futures import ThreadPoolExecutor, as_completed


class GridMosaic:
    def __init__(self, grid_bbox, scene_list, crs_id, z_level, task_id=None, per_grid_workers=10):
        self.grid_bbox = grid_bbox
        self.scene_list = scene_list
        self.final_image = None
        self.final_metadata = None
        self.minio_endpoint = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"
        self.crs_id = CRS.from_epsg(crs_id)
        self.target_res = self.resolution_from_zoom(z_level)
        self.per_grid_workers = per_grid_workers
        self.task_id = task_id

        # MinIO configuration - 使用统一配置
        self.minio_client = Minio(
            f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}",
            access_key=CONFIG.MINIO_ACCESS_KEY,
            secret_key=CONFIG.MINIO_SECRET_KEY,
            secure=CONFIG.MINIO_SECURE
        )
        self.minio_bucket = CONFIG.MINIO_TEMP_FILES_BUCKET
        # 与单线程版本保持一致的目录组织
        if self.task_id:
            self.minio_dir = f"national-mosaic/{self.task_id}/cog"
        else:
            self.minio_dir = "national-mosaicjson"

    def get_lowest_resolution_overview(self, scene) -> ImageData:
        """从 MinIO 中获取最低分辨率的概览数据"""
        max_size = 0
        all_bands_data = []
        info = None
        if len(scene.path) == 3:
            for band_path in scene.path:
                fp = f"{self.minio_endpoint}/{scene.bucket}/{band_path}"
                with COGReader(fp, options={'nodata': 0}) as reader:
                    if max_size == 0:
                        info = reader.info()
                        max_size = int(max(info.width, info.height) / info.overviews[-1])
                    img = reader.preview(indexes=1, max_size=max_size)
                    img_data = img.data
                    if not np.issubdtype(img_data.dtype, np.uint8):
                        band_min = np.nanmin(img_data)
                        band_max = np.nanmax(img_data)
                        if band_max == band_min:
                            img_data = np.zeros_like(img_data, dtype=np.uint8)
                        else:
                            img_data = ((img_data - band_min) / (band_max - band_min) * 255).astype(np.uint8)
                    all_bands_data.append(img_data)
            if all_bands_data:
                stacked_data = np.concatenate(all_bands_data, axis=0)
                data = np.ma.MaskedArray(data=stacked_data)
                image_data = ImageData(data, bounds=img.bounds, crs=img.crs)
                image_data = image_data.reproject(dst_crs=self.crs_id)
                return image_data

        elif len(scene.path) == 1:
            fp = f"{self.minio_endpoint}/{scene.bucket}/{scene.path[0]}"
            with COGReader(fp, options={'nodata': 0}) as reader:
                info = reader.info()
                max_size = int(max(info.width, info.height) / info.overviews[-1])
                img_single = reader.preview(indexes=1, max_size=max_size)
                cm = cmap.get("rdylgn")
                img_single = img_single.apply_colormap(cm)
                return img_single
        else:
            raise ValueError("Invalid scene path length")

    # --- 原有的入口函数，保持向后兼容 ---
    def create_mosaic(self):
        """
        合成多个场景的镶嵌影像, 并将结果上传到 MinIO.
        保持向后兼容，只返回路径
        """
        result = self.create_mosaic_with_metadata()
        if result:
            return result[0]  # 只返回路径
        return None

    # --- 新的入口函数，返回元数据 ---
    def create_mosaic_with_metadata(self):
        img_list = []
        if not self.scene_list:
            return None

        max_workers = max(1, int(self.per_grid_workers))
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = {executor.submit(self.get_lowest_resolution_overview, scene): scene for scene in self.scene_list}
            for fut in as_completed(futures):
                try:
                    imagedata = fut.result()
                    if imagedata:
                        img_list.append(imagedata)
                except Exception as e:
                    print(f"get_lowest_resolution_overview failed: {e}")
        if not img_list:
            print("No valid images to create a mosaic.")
            return None

        mosaic, out_meta = self.mosaic_by_rasterIO(img_list)
        print(out_meta)

        # 定义在MinIO中的存储路径
        # 与单线程版本保持一致: national-mosaic/{task_id}/cog/grid_{lon}_{lat}.tif
        grid_coords = self.grid_bbox[0]
        minio_object_name = f"{self.minio_dir}/grid_{grid_coords[0]:.6f}_{grid_coords[1]:.6f}.tif"
        
        # 调用上传函数
        success = self.upload_cog_to_minio(mosaic, out_meta, self.minio_bucket, minio_object_name)
        
        if success:
            # 从out_meta中提取边界信息
            bounds = self.extract_bounds_from_metadata(out_meta)
            crs_info = out_meta.get('crs', self.crs_id).to_string()
            
            print(f"✅ Successfully uploaded mosaic to minio://{self.minio_bucket}/{minio_object_name}")
            print(f"📍 Bounds: {bounds}")
            
            return minio_object_name, bounds, crs_info
        else:
            print(f"❌ Failed to upload mosaic")
            return None

    def extract_bounds_from_metadata(self, metadata):
        """
        从rasterio metadata中提取地理边界
        """
        transform = metadata.get('transform')
        width = metadata.get('width')
        height = metadata.get('height')
        
        if transform and width and height:
            # 计算四个角的坐标
            left, top = transform * (0, 0)
            right, bottom = transform * (width, height)
            
            # 返回 [west, south, east, north] 格式
            return [left, bottom, right, top]
        else:
            # 如果无法从metadata提取，则使用grid的边界
            return self.extract_bounds_from_grid(self.grid_bbox)

    def mosaic_by_rasterIO(self, img_list):
        rio_dataset_list = []
        out_meta = None
        for img in img_list:
            memory_file = io.BytesIO()
            # 必须写nodata，否则会多出一个alpha波段
            img.to_raster(memory_file, nodata=0)
            memory_file.seek(0)
            src = rasterio.open(memory_file)
            rio_dataset_list.append(src)
            if out_meta is None:
                out_meta = src.meta.copy()

        bounds = self.extract_bounds_from_grid(self.grid_bbox)
        mosaic, out_trans = merge(sources=rio_dataset_list, bounds=bounds, res=self.target_res, nodata=0, method="max")
        out_meta.update({
            "driver": "GTiff",
            "height": mosaic.shape[1],
            "width": mosaic.shape[2],
            "transform": out_trans,
            "crs": self.crs_id  # 确保CRS信息被保存
        })

        for src in rio_dataset_list:
            src.close()
        return mosaic, out_meta

    # --- 修改上传函数，返回成功/失败状态 ---
    def upload_cog_to_minio(self, mosaic_data, metadata, bucket_name, object_name, blocksize=256):
        """
        将镶嵌结果转换为COG格式, 并作为字节流直接上传到MinIO.
        返回是否成功
        """
        try:
            cog_profile = cog_profiles.get("deflate")
            cog_profile.update({
                "blockxsize": blocksize,
                "blockysize": blocksize,
                "compress": "deflate",
                "tiled": True,
                "nodata": 0,
                "driver": "GTiff"
            })

            # 步骤 1: 创建一个临时的内存Tiff文件
            with MemoryFile() as mem_tiff:
                with mem_tiff.open(**metadata) as dataset:
                    dataset.write(mosaic_data)
                    
                # 步骤 2: 将内存Tiff转换为内存COG
                with MemoryFile() as mem_cog:
                    cog_translate(
                        mem_tiff,
                        mem_cog.name, # cog_translate需要一个路径，这里使用内存文件的虚拟路径
                        dst_kwargs=cog_profile,
                        in_memory=True, # 关键参数：确保转换过程在内存中
                        quiet=True,
                    )
                    
                    # 步骤 3: 从内存COG中读取字节流并上传
                    cog_bytes = mem_cog.read()
                    
            # 检查存储桶是否存在，如果不存在则创建
            found = self.minio_client.bucket_exists(bucket_name)
            if not found:
                self.minio_client.make_bucket(bucket_name)
                print(f"Bucket '{bucket_name}' created.")

            # 将字节流上传到 MinIO
            self.minio_client.put_object(
                bucket_name=bucket_name,
                object_name=object_name,
                data=io.BytesIO(cog_bytes), # 将字节数据包装成BytesIO对象
                length=len(cog_bytes),
                content_type='image/tiff'
            )
            return True
            
        except Exception as e:
            print(f"❌ 上传COG失败: {e}")
            import traceback
            traceback.print_exc()
            return False

    def extract_bounds_from_grid(self, grid):
        longitudes = [x[0] for x in grid]
        latitudes = [x[1] for x in grid]
        return (min(longitudes), min(latitudes), max(longitudes), max(latitudes))

    def resolution_from_zoom(self, z: int, tile_size: int = 256) -> float:
        return 360.0 / (tile_size * (2 ** z))

    def get_image_size_from_grid(self, grid_coords: list, resolution: float, crs_is_wgs84: bool = True):
        lons = [pt[0] for pt in grid_coords]
        lats = [pt[1] for pt in grid_coords]
        xmin, xmax = min(lons), max(lons)
        ymin, ymax = min(lats), max(lats)
        x_res, y_res = resolution, resolution
        width = int(round((xmax - xmin) / x_res))
        height = int(round((ymax - ymin) / y_res))
        return width, height