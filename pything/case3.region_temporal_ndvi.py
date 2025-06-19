from ogms_xfer import OGMS_Xfer as xfer
import datetime

config_file_path = "config.json"
xfer.initialize(config_file_path)

###################################################
import json
from osgeo import gdal
from datetime import datetime
import numpy as np
import os

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
    return output_path


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
    x_pixel = int((longitude - x_origin) / pixel_width + 0.5)
    y_pixel = int((latitude - y_origin) / pixel_height + 0.5)

    # 读取像素值
    band = dataset.GetRasterBand(band)  # 假设采样第一个波段
    data = band.ReadAsArray()
    
    if 0 <= x_pixel < data.shape[1] and 0 <= y_pixel < data.shape[0]:
        sample_value = data[y_pixel, x_pixel]
        print(f"采样值: {sample_value}")
    else:
        # raise Exception("坐标超出栅格范围")
        print(f"坐标超出栅格范围: {x_pixel}, {y_pixel}")
        sample_value = np.nan
        
    # 关闭数据集
    dataset = None

    return sample_value


def calculate_scene_ndvi(scene, feature, ndvi_output_path):
    print('------------------------------------------')
    print(f"处理影像{scene.scene_id}...")
    
    nir_path = xfer.URL.resolve(scene.get_band_image(5).url)
    red_path = xfer.URL.resolve(scene.get_band_image(4).url)
    
    nir_path = xfer.TileUtil.read_image_feature_save(nir_path, feature, xfer.URL.outputUrl(f'{scene.scene_id}_region_nir.tif'))
    red_path = xfer.TileUtil.read_image_feature_save(red_path, feature, xfer.URL.outputUrl(f'{scene.scene_id}_region_red.tif'))
    print(f"区域影像几何裁剪完成...")
    
    ndvi_output_path = calc_ndvi(nir_path, red_path, ndvi_output_path)
   
    print(f"NDVI计算完毕....")
    return ndvi_output_path


##### 区域影像获取 （1st）
# 感兴趣区
with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
    region = json.load(f)
    feature = region['features'][0]

# 目标产品
product = xfer.Product().query(product_name="L2SP")[0]

# 按条件筛选出系列影像
scenes = xfer.Scene().query(
    # product_id=product.product_id,
    polygon=region,
    # time_range=(datetime(2021, 1, 1), datetime(2025, 1, 31)),
    # cloud_range=(0, 10)
)

# 计算NDVI
outputUrls = []

start_time = datetime.now()
for i, scene in enumerate(scenes):
    outputUrls.append(xfer.URL.outputUrl(f'ndvi_{i}.tif'))
    calculate_scene_ndvi(scene, feature, outputUrls[i])

end_time = datetime.now()
print(f"区域时序NDVI计算完成，总计用时{end_time - start_time}")

# 基于给定点采样生成历年ndvi曲线
point = [
    120.4089252118747,
    30.5407566292287
]
# point = [119.295706, 31.603155]
json_path = xfer.URL.outputUrl('data.json')
data = {
    'date':[],
    'value':[],
}

for i, tif_path in enumerate(outputUrls):
    scene_time = scenes[i].scene_time
    time = scene_time.strftime('%Y/%m/%d')
    data['date'].append(time)
    data['value'].append(float(sample_raster(tif_path, point[0], point[1])))

with open(json_path, 'w') as f:
    json.dump(data, f, indent=4)

print("--------------------------------")
