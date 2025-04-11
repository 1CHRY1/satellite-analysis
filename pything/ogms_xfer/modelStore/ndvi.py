from osgeo import gdal
import numpy as np

def ndvi(nir_path, red_path, output_path):
    """
    计算NDVI
    """
    nir_ds = gdal.Open(nir_path)
    red_ds = gdal.Open(red_path)


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