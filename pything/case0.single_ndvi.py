from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime

config_file_path = "config.json"
xfer.initialize(config_file_path)



##################################################
from osgeo import gdal
import numpy as np

def calc_ndvi(nir_path, red_path, output_path):
    # 打开输入文件
    nir_ds = gdal.Open(nir_path)
    red_ds = gdal.Open(red_path)

    # 获取输入文件的大小
    nir_xsize, nir_ysize = nir_ds.RasterXSize, nir_ds.RasterYSize
    red_xsize, red_ysize = red_ds.RasterXSize, red_ds.RasterYSize

    if nir_xsize != red_xsize or nir_ysize != red_ysize:
        raise ValueError("Input images do not have the same size")

    # 读取波段数据
    nir_band = nir_ds.GetRasterBand(1).ReadAsArray()
    red_band = red_ds.GetRasterBand(1).ReadAsArray()

    # 计算 NDVI
    ndvi = (nir_band.astype(float) - red_band.astype(float)) / (nir_band + red_band + 1e-6)
    ndvi = np.nan_to_num(ndvi, nan=-9999)

    # 创建输出文件
    driver = gdal.GetDriverByName('GTiff')
    ndvi_ds = driver.Create(output_path, nir_xsize, nir_ysize, 1, gdal.GDT_Float32)

    # 设置投影和地理变换信息
    ndvi_ds.SetProjection(nir_ds.GetProjection())
    ndvi_ds.SetGeoTransform(nir_ds.GetGeoTransform())

    # 写入 NDVI 数据
    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.FlushCache()  # 确保数据写入磁盘

    print("NDVI calculation completed successfully")



# 1. 检索影像 用ID检索
print('3333')
# scene = xfer.Scene().query(scene_name='LC08_L2SP_118038_20240320_20240402_02_T1')[0] # 直接通过ID检索
print('2222')

# if(scene.scene_id == None):
#     print("无法检索到影像, 请检查影像ID是否正确")
#     exit()
# print('111')
# print("检索到影像： ", scene.scene_name)
# print("波段数： ",scene.band_num)
# print("波段列表： ",scene.bands)

# 2. 取影像红光和近红外波段URL， 传入计算NDVI
# scene.get_band_image(5).pull('./data/nir.tif')
# scene.get_band_image(4).pull('./data/red.tif')
print('999')

output_path = xfer.URL.outputUrl('ndvibyremote.tif')

# start_time = datetime.now()
calc_ndvi('./data/nir.tif', './data/red.tif', output_path)
# end_time = datetime.datetime.now()
# print(f"云端资源计算NDVI总时间: {end_time - start_time}")
# print(' --------------------------------- ')
