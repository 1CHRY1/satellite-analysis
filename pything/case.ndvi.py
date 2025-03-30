from ogms_xfer import OGMS_Xfer as xfer
import numpy as np
from osgeo import gdal
import datetime

def calculate_ndvi(nir_path, red_path, output_path):
    nir_ds = gdal.Open(nir_path)
    red_ds = gdal.Open(red_path)

    if not nir_ds or not red_ds:
        raise ValueError("无法打开影像数据，请检查 URL 是否有效")

    nir_band = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)

    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-10)

    geo_transform = nir_ds.GetGeoTransform()
    projection = nir_ds.GetProjection()
    cols, rows = nir_band.shape

    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(output_path, rows, cols, 1, gdal.GDT_Float32)

    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)

    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(-9999)

    nir_ds, red_ds, ndvi_ds = None, None, None
    print(f"NDVI 计算完成，已保存到: {output_path}")
    return output_path


if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    xfer.initialize(config_file_path)

    # 1. 检索影像
    scene = xfer.Scene("SC906772444")
    print(scene.band_num)
    print(scene.bands)
 
    # 2.a. 将云端数据下载到容器后计算
    start_time = datetime.datetime.now()
    nir_url = xfer.URL.resolve(scene.get_band_image(5).url)
    red_url = xfer.URL.resolve(scene.get_band_image(4).url)
    calculate_ndvi(nir_url, red_url, './output/ndvibyremote.tif')
    end_time = datetime.datetime.now()
    print(f"云端资源计算NDVI总时间: {end_time - start_time}")
    print(' --------------------------------- ')
    
    # 2.b. 直接使用GDAL读取云端数据计算
    start_time = datetime.datetime.now()
    nir_band_image = scene.get_band_image(5)
    red_band_image = scene.get_band_image(4)
    local_nir_path = "./data/nir.tif"
    local_red_path = "./data/red.tif"
    
    nir_band_image.pull(local_nir_path)
    red_band_image.pull(local_red_path)
    
    calculate_ndvi(local_nir_path, local_red_path, './output/ndvibylocal.tif')
    end_time = datetime.datetime.now()
    print(f"本地资源计算NDVI总时间: {end_time - start_time}")
    print(' --------------------------------- ')