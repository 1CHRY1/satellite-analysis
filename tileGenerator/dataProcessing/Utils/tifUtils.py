import os
import tempfile
import uuid

import rasterio
from rio_cogeo.cogeo import cog_translate, cog_info
from rio_cogeo.profiles import cog_profiles
from PIL import Image
import numpy as np


def percentile_stretch(array, lower=2, upper=98):
    """百分比截断拉伸增强"""
    # 排除无效值（假设NoData值为0）
    array = np.ma.masked_equal(array, 0)
    # 计算百分比范围
    lower_percentile = np.percentile(array.compressed(), lower)
    upper_percentile = np.percentile(array.compressed(), upper)
    # 应用拉伸
    stretched = np.clip(array, lower_percentile, upper_percentile)
    stretched = (stretched - lower_percentile) / (upper_percentile - lower_percentile) * 255
    return stretched.filled(0).astype(np.uint8)  # 填充无效值并转换为8位


def create_preview_image(ds, output_image, scale_factor=1.0):
    """
    创建多波段影像的彩色合成预览图
    :param input_raster: 输入的多波段影像文件路径
    :param output_image: 输出的预览图文件路径
    :param scale_factor: 缩放比例，默认为0.1（即缩小到原图的10%）
    """

    # 获取影像的波段数量
    band_count = ds.RasterCount
    if band_count < 3:
        raise Exception("影像波段数不足，无法进行彩色合成")

    # 提取前三个波段（假设为RGB）
    band1 = ds.GetRasterBand(1).ReadAsArray()
    band2 = ds.GetRasterBand(2).ReadAsArray()
    band3 = ds.GetRasterBand(3).ReadAsArray()

    # 将波段数据缩放到0-255范围
    band1 = (band1 - np.min(band1)) / (np.max(band1) - np.min(band1)) * 255
    band2 = (band2 - np.min(band2)) / (np.max(band2) - np.min(band2)) * 255
    band3 = (band3 - np.min(band3)) / (np.max(band3) - np.min(band3)) * 255

    # 将波段数据转换为整型
    band1 = band1.astype(np.uint8)
    band2 = band2.astype(np.uint8)
    band3 = band3.astype(np.uint8)

    # 合成RGB图像
    rgb_image = np.stack([band1, band2, band3], axis=-1)

    # 缩放图像
    height, width, _ = rgb_image.shape
    new_width = int(width * scale_factor)
    new_height = int(height * scale_factor)
    preview_image = np.array(Image.fromarray(rgb_image).resize((new_width, new_height), Image.Resampling.LANCZOS))

    # 保存预览图
    img = Image.fromarray(preview_image)
    # 可选择设置图片的最大尺寸
    max_size = (1024, 1024)
    img.thumbnail(max_size, Image.Resampling.LANCZOS)
    # 保存图片
    img.save(output_image, format='PNG', optimize=True)
    return output_image



def convert_tif2cog(tif_path):
    if cog_info(tif_path)["COG"]:
        return tif_path
    else:
        temp_dir = tempfile.gettempdir()
        output_cog_tif = os.path.join(temp_dir, os.path.basename(tif_path))
        with rasterio.open(tif_path) as src:
            profile = cog_profiles.get("deflate")
            cog_translate(src, output_cog_tif, profile, in_memory=False)
        return output_cog_tif



## QA 云量相关 #########################################################################################
def read_qa_pixel(tiff_path, scene_name):
    # --------- Read QA_PIXEL band ---------------------------------
    directory = os.path.dirname(tiff_path)
    qa_pixel_path = os.path.join(directory, f"{scene_name}_QA_PIXEL.TIF")
    with rasterio.open(qa_pixel_path) as src:
        qa_pixel_data = src.read(1)  # 读取第一波段数据

    # --------- Get no data value ----------------------------------
    with rasterio.open(tiff_path) as src:
        no_data_value = src.nodata
    if no_data_value is None:
        no_data_value = 0

    return qa_pixel_data, no_data_value


def get_cloud_coverage4grid(qa_pixel_data, x, y, w, h, no_data_value):
    # --------- Get cloud coverage for every grid ----------------------------

    # --------- Get QA_PIXEL data in grid ----------------------------
    row_min, row_max = max(0, y), min(qa_pixel_data.shape[0], y + h)
    col_min, col_max = max(0, x), min(qa_pixel_data.shape[1], x + w)
    grid_data = qa_pixel_data[row_min:row_max, col_min:col_max]

    # --------- Get Bit3(thick) and Bit2(thin) mask ------------------
    thick_cloud_mask = (grid_data & (1 << 3)) > 0
    thin_cloud_mask = (grid_data & (1 << 2)) > 0
    cloud_mask = thick_cloud_mask | thin_cloud_mask
    no_data_mask = grid_data == no_data_value  # 由外部提供 NoData 值
    valid_pixels = ~no_data_mask  # 有效像素区域

    # --------- Calculate cloud coverage -----------------------------
    total_valid_pixels = np.sum(valid_pixels)
    if total_valid_pixels == 0:
        return 0.0  # 如果网格内全是 NoData，则云覆盖率设为 0

    cloud_percentage = np.sum(cloud_mask & valid_pixels) / total_valid_pixels * 100

    return cloud_percentage


def get_cloud_coverage4image(qa_pixel_data, no_data_value):
    # --------- Get cloud coverage for every image ----------------------------

    # --------- Get Bit3(thick) and Bit2(thin) mask ------------------
    thick_cloud_mask = (qa_pixel_data & (1 << 3)) > 0
    thin_cloud_mask = (qa_pixel_data & (1 << 2)) > 0
    cloud_mask = thick_cloud_mask | thin_cloud_mask
    no_data_mask = qa_pixel_data == no_data_value  # 由外部提供 NoData 值
    valid_pixels = ~no_data_mask  # 有效像素区域

    # --------- Calculate cloud coverage -----------------------------
    cloud_percentage = np.sum(cloud_mask & valid_pixels) / np.sum(valid_pixels) * 100

    return cloud_percentage
