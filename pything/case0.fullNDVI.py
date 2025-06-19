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

    if not nir_ds or not red_ds:
        raise ValueError("无法打开影像数据，请检查 URL 是否有效")

    # 读取NoData值
    nir_nodata = nir_ds.GetRasterBand(1).GetNoDataValue()
    red_nodata = red_ds.GetRasterBand(1).GetNoDataValue()

    nir_band = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)

    # 构建掩膜
    mask = np.full(nir_band.shape, False)
    if nir_nodata is not None:
        mask |= (nir_band == nir_nodata)
    if red_nodata is not None:
        mask |= (red_band == red_nodata)

    # 计算NDVI
    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-10)
    ndvi_nodata = -9999
    ndvi = np.where(mask, ndvi_nodata, ndvi)  # 保证掩膜区为-9999

    geo_transform = nir_ds.GetGeoTransform()
    projection = nir_ds.GetProjection()
    cols, rows = nir_band.shape

    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(output_path, rows, cols, 1, gdal.GDT_Float32)

    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)

    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(ndvi_nodata)
    print("ndvi_ds created, start to translate to COG")
    
    gdal.Translate(
        output_path,
        ndvi_ds,
        format='COG',
        creationOptions=[
            'COMPRESS=LZW',
            'BLOCKSIZE=512'
        ]
    )
    print("COG NDVI calculation completed successfully")



# 1. 检索影像 用ID检索
scene = xfer.Scene().query(scene_name='LC08_L2SP_120035_20250217_20250226_02_T1')[0] # 直接通过ID检索
 
# 2. 直接用 vsicul http 资源
nir_path = xfer.URL.resolve(scene.get_band_image(5).url)
red_path = xfer.URL.resolve(scene.get_band_image(4).url)

# # 2. pull到本地后使用
# nir_path = xfer.URL.dataUrl('nir.tif')
# red_path = xfer.URL.dataUrl('red.tif')
# scene.get_band_image(5).pull(nir_path)
# scene.get_band_image(4).pull(red_path)

print(nir_path)
print(red_path)

output_path = xfer.URL.outputUrl('NDVI.tif')

start_time = datetime.now()
calc_ndvi(nir_path, red_path, output_path)
end_time = datetime.now()
print(f"云端资源计算NDVI总时间: {end_time - start_time}")
print(' --------------------------------- ')
