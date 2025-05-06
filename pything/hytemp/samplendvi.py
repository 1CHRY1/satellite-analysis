from osgeo import gdal
import os
import numpy as np
from datetime import datetime

def sample_raster(raster_path, longitude, latitude, band=1):
    """
    将WGS84经纬度坐标点匹配到栅格的像素上并进行采样, 默认采样第一个波段

    :param raster_path: 栅格文件路径
    :param longitude: 经度
    :param latitude: 纬度
    :return: 采样值
    """
    # 打开栅格文件
    dataset = gdal.Open(raster_path)
    if dataset is None:
        raise Exception(f"无法打开栅格文件：{raster_path}")

    # 获取地理变换信息
    geotransform = dataset.GetGeoTransform()
    x_origin, pixel_width, _, y_origin, _, pixel_height = geotransform

    # 将经纬度坐标转换为像素坐标
    x_pixel = int((longitude - x_origin) / pixel_width)
    y_pixel = int((latitude - y_origin) / pixel_height)

    # 读取像素值
    band = dataset.GetRasterBand(band)  # 假设采样第一个波段
    data = band.ReadAsArray()
    if 0 <= x_pixel < data.shape[1] and 0 <= y_pixel < data.shape[0]:
        sample_value = data[y_pixel, x_pixel]
    else:
        raise Exception("坐标超出栅格范围")

    # 关闭数据集
    dataset = None

    return sample_value

def calculate_ndvi(nir_path, red_path, output_path):
    nir_ds = gdal.Open(nir_path)
    red_ds = gdal.Open(red_path)

    if not nir_ds or not red_ds:
        raise ValueError("无法打开影像数据，请检查 URL 是否有效")

    if nir_ds.RasterCount != 1 or red_ds.RasterCount != 1:
        raise ValueError("输入影像必须是单波段数据")

    nir_band = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)

    # 将NoData值设置为np.nan
    nir_band[nir_band == nir_ds.GetRasterBand(1).GetNoDataValue()] = np.nan
    red_band[red_band == red_ds.GetRasterBand(1).GetNoDataValue()] = np.nan

    # 计算NDVI，避免分母为零
    ndvi = np.where(nir_band + red_band != 0, (nir_band - red_band) / (nir_band + red_band), np.nan)

    geo_transform = nir_ds.GetGeoTransform()
    projection = nir_ds.GetProjection()
    cols, rows = nir_band.shape

    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(output_path, cols, rows, 1, gdal.GDT_Float32)

    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)

    ndvi_ds.GetRasterBand(1).WriteArray(np.nan_to_num(ndvi, nan=-9999))
    ndvi_ds.GetRasterBand(1).SetNoDataValue(-9999)

    del nir_ds, red_ds, ndvi_ds
    return output_path

if __name__ == "__main__":
    
    nir_path = "D:\\edgedownload\\multi_output\\LT51190382000261BJC00_B5\\tile_43_29.tif"
    red_path = "D:\\edgedownload\\multi_output\\LT51190382000261BJC00_B4\\tile_43_29.tif"
    ndvi_path = os.path.join(os.path.dirname(__file__), "ndvi.tif")
    
    start_time = datetime.now()
    calculate_ndvi(nir_path, red_path, ndvi_path)
    end_time = datetime.now()
    print(f"计算NDVI时间: {end_time - start_time}")

    print(sample_raster(ndvi_path, 121.0192, 31.437)) # -29
    print(sample_raster(ndvi_path, 121.0292, 31.417)) # -113
    print(sample_raster(ndvi_path, 121.0332, 31.427)) # -113