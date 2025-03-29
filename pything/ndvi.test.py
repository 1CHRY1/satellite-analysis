from ogms_xfer import OGMS_Xfer as xfer
import numpy as np
from osgeo import gdal

# TODO 加个URL TOOL
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
    
    # 两种计算方式 ，pull到个人资产， 或采用gdal根据url读取文件
    
    # 影像检索筛选
    scene = xfer.Scene("SC906772444")
    print(scene.band_num)
    print(scene.bands)
    
    nir_url = scene.get_band_image(5).url
    red_url = scene.get_band_image(4).url
    
    prefix = "http://223.2.34.7:9000"
    nir_url = f"{prefix}{nir_url}"
    red_url = f"{prefix}{red_url}"
    

    # 1. 假定已经有URL  TODO 后面也需要检索后，从Image对象上获取
    calculate_ndvi(nir_url, red_url, './output/ndvi.tif')
    print(' --------------------------------- ')
    
 
    # 2. 影像检索， pull到本地，然后计算

