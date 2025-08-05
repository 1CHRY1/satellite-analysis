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

class GridMosaic:
    def __init__(self, grid_bbox, scene_list, crs_id, z_level):
        self.grid_bbox = grid_bbox
        self.scene_list = scene_list
        self.final_image = None
        self.final_metadata = None
        self.minio_endpoint = "http://223.2.34.8:30900"
        self.crs_id = CRS.from_epsg(crs_id)
        self.target_res = self.resolution_from_zoom(z_level)

        # MinIO configuration
        self.minio_client = Minio(
            "223.2.34.8:30900",
            access_key="minioadmin",
            secret_key="minioadmin",
            secure=False
        )
        self.minio_bucket = "temp-files"
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
        """
        合成多个场景的镶嵌影像, 并将结果上传到 MinIO.
        返回 (minio_path, bounds, crs_info) 元组
        """
        img_list = []
        for scene in self.scene_list:
            imagedata = self.get_lowest_resolution_overview(scene)
            if imagedata:
                img_list.append(imagedata)

        if not img_list:
            print("No valid images to create a mosaic.")
            return None

        mosaic, out_meta = self.mosaic_by_rasterIO(img_list)
        print(out_meta)

        # 定义在MinIO中的存储路径
        # 格式: national-mosaicjson/经度_纬度.tif
        minio_object_name = f"{self.minio_dir}/{self.grid_bbox[0][0]}_{self.grid_bbox[0][1]}.tif"
        
        # 调用上传函数
        success = self.upload_cog