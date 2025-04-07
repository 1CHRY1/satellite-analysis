import os.path

import numpy as np
from osgeo import gdal
import config
from dataProcessing.Utils.tifUtils import convert_tif2cog

band_urls = {
    'nir': "http://223.2.34.7:9000/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20240320_20240402_02_T1/LC08_L2SP_118038_20240320_20240402_02_T1_SR_B1.TIF",
    'red': "http://223.2.34.7:9000/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20240320_20240402_02_T1/LC08_L2SP_118038_20240320_20240402_02_T1_SR_B2.TIF",
}


def calculate_ndvi(band_urls, output_path="ndvi.tif"):
    """从 HTTP 读取 NIR 和 RED 波段数据，计算 NDVI 并保存为 TIFF"""
    # 通过 GDAL 打开 HTTP 远程影像
    nir_ds = gdal.Open(band_urls["nir"])
    red_ds = gdal.Open(band_urls["red"])

    if not nir_ds or not red_ds:
        raise ValueError("无法打开影像数据，请检查 URL 是否有效")

    # 读取影像的第 1 个波段（假设单波段影像）
    nir_band = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)

    # 计算 NDVI，避免除零错误
    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-10)

    # 获取影像的地理信息和投影
    geo_transform = nir_ds.GetGeoTransform()
    projection = nir_ds.GetProjection()
    cols, rows = nir_band.shape  # 获取影像大小

    # 创建 NDVI 影像文件
    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(output_path, rows, cols, 1, gdal.GDT_Float32)

    # 设置地理信息和投影
    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)

    # 写入 NDVI 数据
    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(-9999)  # 设置无效值

    # 关闭数据集
    nir_ds, red_ds, ndvi_ds = None, None, None
    print(f"NDVI 计算完成，已保存到: {output_path}")
    return output_path  # 返回保存的文件路径

if __name__ == '__main__':
    ndvi_path = calculate_ndvi(band_urls, os.path.join(config.TEMP_OUTPUT_DIR, "ndvi.tif"))
    output_path = convert_tif2cog(ndvi_path)
    print(output_path)

