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
from rasterio.enums import Resampling
from rio_cogeo.profiles import cog_profiles
class GridMosaic:
    def __init__(self, grid_bbox, scene_list, crs_id, z_level):
        self.grid_bbox = grid_bbox  # 格网的 bounding box
        self.scene_list = scene_list  # 格网相关的场景列表
        self.final_image = None  # 最终拼接的图像
        self.final_metadata = None  # 最终图像的元数据
        self.minio_endpoint = "http://172.31.13.21:9000"
        self.crs_id = CRS.from_epsg(crs_id)
        self.target_res = self.resolution_from_zoom(z_level)

    def get_lowest_resolution_overview(self, scene)->ImageData:
        """从 MinIO 中获取最低分辨率的概览数据"""
        max_size = 0
        all_bands_data = [] 
        all_bands_masks = []
        info = None
        if len(scene.path) == 3:
            for band_path in scene.path:
                fp = f"{self.minio_endpoint}/{scene.bucket}/{band_path}"

                with COGReader(fp, options={'nodata': 0}) as reader:
                    if max_size == 0:
                        info = reader.info()
                        # 取width 和height的大值
                        max_size = int(max(info.width, info.height)/info.overviews[-1])
                    img = reader.preview(indexes=1, max_size=max_size)
                    img_data = img.data
                    if not np.issubdtype(img_data.dtype, np.uint8):  #转换为 uint8
                        band_min = np.nanmin(img_data)
                        band_max = np.nanmax(img_data)
                        if band_max == band_min: # 避免除以0，全为0
                            # img_data = np.zeros_like(band, dtype=np.uint8)    
                            img_data = np.zeros_like(img_data, dtype=np.uint8)
                        else:
                            img_data = ((img_data - band_min) / (band_max - band_min) * 255).astype(np.uint8)
                    all_bands_data.append(img_data) 
                    # all_bands_masks.append(img.mask) # no needs right now
            if all_bands_data:
                # 将所有波段的数据合并成一个多波段数组
                stacked_data = np.concatenate(all_bands_data, axis=0)  # shape: (band_count, height, width)
                # 合并所有波段的掩码
                # stacked_mask = np.concatenate(all_bands_masks, axis=0)  # shape: (band_count, height, width)
                data = np.ma.MaskedArray(data = stacked_data)
                image_data = ImageData(data, bounds = img.bounds, crs = img.crs)
                image_data = image_data.reproject(dst_crs=self.crs_id)
                return image_data

        elif len(scene.path) == 1:
            fp = f"{self.minio_endpoint}/{scene.bucket}/{scene.path[0]}"
            with COGReader(fp, options={'nodata': 0}) as reader:
                info = reader.info()
                # 取width 和height的大值
                max_size = int(max(info.width, info.height)/info.overviews[-1])
                img_single = reader.preview(indexes=1, max_size=max_size)
                cm = cmap.get("rdylgn")
                img_single = img_single.apply_colormap(cm) # to rgba
                return img_single
        else:
            raise ValueError("Invalid scene path length")
# 入口函数
    def create_mosaic(self, prefix_minio):
        """合成多个场景的镶嵌影像"""
        img_list = []
        for scene in self.scene_list:
            imagedata = self.get_lowest_resolution_overview(scene)
            img_list.append(imagedata)
        mosaic, out_meta = self.mosaic_by_rasterIO(img_list) 
        output_path = f"{prefix_minio}/{self.grid_bbox[0]}_{self.grid_bbox[1]}.tif"
        self.save_mosaic_as_cog(mosaic, out_meta, output_path)
        return output_path

    def mosaic_by_rasterIO(self, img_list):
        rio_dataset_list = []
        out_meta = None
        for img in img_list:
            memory_file = io.BytesIO()
            img.to_raster(memory_file,nodata=0)# 必须写nodata，否则会多出一个波段
            memory_file.seek(0)
            src = rasterio.open(memory_file)   
            rio_dataset_list.append(src)
            if out_meta is None:
                out_meta = src.meta.copy()
        
        bounds = self.extract_bounds_from_grid(self.grid_bbox)
        mosaic, out_trans = merge(sources= rio_dataset_list,bounds=bounds, res=self.target_res, nodata=0, method="max")
        out_meta.update({"driver": "GTiff","height": mosaic.shape[1],"width": mosaic.shape[2],"transform": out_trans})
        # close src
        for src in rio_dataset_list:
            src.close()
        return mosaic, out_meta

    def save_mosaic_as_cog(self, mosaic, out_meta, output_path, blocksize=256):
        """
        将 rasterio.merge 结果保存为高效的 COG 文件
        """
        profile = cog_profiles.get("deflate")  # 可选 "lzw", "zstd" 等压缩方式
        profile.update({
            "blocksize": blocksize,
            "compress": "deflate",
            "tiled": True,
            "nodata": 0,
            "driver": "GTiff"
        })

        # 用内存文件临时保存
        with MemoryFile() as memfile:
            with memfile.open(**out_meta) as dataset:
                dataset.write(mosaic)
            # 生成 COG 文件
            cog_translate(
                memfile,
                output_path,
                dst_kwargs=profile,
                in_memory=True,
                quiet=True,
            )

    def extract_bounds_from_grid(self, grid):
        # 提取坐标并计算边界框
        longitudes = [x[0] for x in grid]
        latitudes = [x[1] for x in grid]
        
        # 返回左、下、右、上的边界值
        return (min(longitudes), min(latitudes), max(longitudes), max(latitudes))

    def resolution_from_zoom(self, z: int, tile_size: int = 256) -> float:
        """
        输入 zoom 级别，返回每像素对应地面上的距离（单位：度）
        """
        return 360.0 / (tile_size * (2 ** z))

    # 没用 right now
    def get_image_size_from_grid(self,
        grid_coords: list,
        resolution: float,  # (x_res, y_res)
        crs_is_wgs84: bool = True
    ):
        """
        根据格网坐标点和分辨率计算输出影像的宽度和高度（像素个数）

        参数:
            grid_coords: 格网四个点（[lon, lat]），闭合，如 [[xmin,ymin], [xmin,ymax], [xmax,ymax], [xmax,ymin], [xmin,ymin]]
            resolution: 分辨率 (x_res, y_res)，单位为：度/像素（WGS84）或米/像素（投影坐标）
            crs_is_wgs84: 是否为 WGS84 投影，如果为 False，说明单位是米

        返回:
            (width, height): 输出影像的像素尺寸
        """
        lons = [pt[0] for pt in grid_coords]
        lats = [pt[1] for pt in grid_coords]

        xmin, xmax = min(lons), max(lons)
        ymin, ymax = min(lats), max(lats)

        x_res, y_res = resolution

        width = int(round((xmax - xmin) / x_res))
        height = int(round((ymax - ymin) / y_res))

        return width, height