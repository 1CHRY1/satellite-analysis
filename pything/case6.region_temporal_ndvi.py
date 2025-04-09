from ogms_xfer import OGMS_Xfer as xfer
import json
from osgeo import gdal
from datetime import datetime
import numpy as np
import os

def merge_tifs_in_memory(input_files):
    # 动态生成内存中的VRT文件路径
    output_vrt_in_memory = "/vsimem/merged.vrt"
    
    # 动态生成内存中的TIF文件路径
    output_tif_in_memory = "/vsimem/merged" + datetime.now().strftime("%Y%m%d%H%M%S") + ".tif"

    # 创建VRT文件并保存在内存中
    vrt_options = gdal.BuildVRTOptions(separate=False)  # 将所有输入文件合并为一个波段
    vrt = gdal.BuildVRT(output_vrt_in_memory, input_files, options=vrt_options)
    if vrt is None:
        raise Exception("Failed to create VRT file in memory")

    # 将VRT文件转换为TIF文件，并保存在内存中
    translate_options = gdal.TranslateOptions(format="GTiff")
    gdal.Translate(output_tif_in_memory, vrt, options=translate_options)

    # 清理内存中的VRT文件
    gdal.Unlink(output_vrt_in_memory)

    print(f"瓦片合并完成，输出文件已保存到虚拟内存...")
    return output_tif_in_memory  # 返回内存中的TIF文件路径，供后续处理


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



def calculate_scene_ndvi(scene, region, ndvi_output_path):
    print('------------------------------------------')
    print(f"处理影像{scene.scene_id}...")
    nir_image = scene.get_band_image("5")
    red_image = scene.get_band_image("4")
    nir_region_tiles = nir_image.get_tiles_by_polygon(region)
    red_region_tiles = red_image.get_tiles_by_polygon(region)
    
    nir_tif_paths = [xfer.URL.resolve(tile.url) for tile in nir_region_tiles]
    red_tif_paths = [xfer.URL.resolve(tile.url) for tile in red_region_tiles]
    
    nir_merged = merge_tifs_in_memory(nir_tif_paths)
    red_merged = merge_tifs_in_memory(red_tif_paths)
    print(f"已完成区域红外和红光瓦片合并")
    
    calculate_ndvi(nir_merged, red_merged, ndvi_output_path)
   
    print(f"NDVI计算完毕....")
    return ndvi_output_path



if __name__ == "__main__":
    
    config_file_path = "config.json"
    xfer.initialize(config_file_path)
    
    ##### 区域影像获取 （1st）
    # 感兴趣区
    with open(xfer.URL.dataUrl('small.geojson'), "r") as f:
        region = json.load(f)
    
    # 目标产品
    product = xfer.Product().query(product_name="landset8_L2SP")[0]
    
    # 按条件筛选出系列影像
    scenes = xfer.Scene().query(
        product_id=product.product_id,
        polygon=region,
        time_range=(datetime(2021, 1, 1), datetime(2025, 1, 31)),
        cloud_range=(0, 10)
    )
    
    # 载入感兴趣区
    with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
        region = json.load(f)


    # 计算NDVI
    outputUrls = []
    
    start_time = datetime.now()
    for i, scene in enumerate(scenes):
        outputUrls.append(xfer.URL.outputUrl(f'ndvi_{i}.tif'))
        calculate_scene_ndvi(scene, region, outputUrls[i])
    
    end_time = datetime.now()
    print(f"区域时序NDVI计算完成，总计用时{end_time - start_time}")
    
    # 基于给定点采样生成历年ndvi曲线
    point = [119.295706, 31.603155]
    json_path = os.path.join(os.path.dirname(__file__), 'data.json')
    data = {
        'date':[],
        'value':[],
    }
    
    for i, tif_path in enumerate(outputUrls):
        time = datetime(2012 + i, 1, 1)
        data['date'].append(time.strftime('%Y/%m/%d'))
        data['value'].append(float(sample_raster(tif_path, point[0], point[1])))
    
    with open(json_path, 'w') as f:
        json.dump(data, f, indent=4)
    
    print("--------------------------------")