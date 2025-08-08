from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime
import json

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

    # 读取NoData值
    nir_nodata = nir_ds.GetRasterBand(1).GetNoDataValue()
    red_nodata = red_ds.GetRasterBand(1).GetNoDataValue()

    # 读取波段数据
    nir_band = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)

    # 构建掩膜
    mask = np.full(nir_band.shape, False)
    if nir_nodata is not None:
        mask |= (nir_band == nir_nodata)
    if red_nodata is not None:
        mask |= (red_band == red_nodata)

    # 计算 NDVI
    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-6)
    ndvi_nodata = -9999
    ndvi = np.where(mask, ndvi_nodata, ndvi)  # 保证掩膜区为-9999

    # 创建输出文件
    driver = gdal.GetDriverByName('MEM')
    ndvi_ds = driver.Create('', nir_xsize, nir_ysize, 1, gdal.GDT_Float32)
    ndvi_ds.SetProjection(nir_ds.GetProjection())
    ndvi_ds.SetGeoTransform(nir_ds.GetGeoTransform())
    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(ndvi_nodata)
    ndvi_ds.FlushCache()
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


# 目标产品和影像查询
scene = xfer.Scene().query(scene_name='LC08_L2SP_120035_20250217_20250226_02_T1')[0] # 直接通过ID检索

# 直接用 vsicul http 资源
nir_path = xfer.URL.resolve(scene.get_band_image(5).url)
red_path = xfer.URL.resolve(scene.get_band_image(4).url)

# 感兴趣区
with open(xfer.URL.dataUrl('region2.geojson'), "r") as f:
    region = json.load(f)
    feature = region['features'][0]
    
print("开始区域影像几何裁剪...")
nir_path = xfer.TileUtil.read_image_feature_save(nir_path, feature, xfer.URL.outputUrl('region_nir.tif'))
red_path = xfer.TileUtil.read_image_feature_save(red_path, feature, xfer.URL.outputUrl('region_red.tif'))
print("区域影像几何裁剪完成...")

output_path = xfer.URL.outputUrl('region_ndvi.tif')
start_time = datetime.now()
calc_ndvi(nir_path, red_path, output_path)