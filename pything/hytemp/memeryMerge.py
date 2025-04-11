from osgeo import gdal
import numpy as np
from datetime import datetime
import os, json

# vrt_options = gdal.BuildVRTOptions(separate=True)  # 波段合并，每个tif一个波段

# Utils
def merge_bands(input_files, output_path):
    """
    合并多个TIF文件的波段，并返回合并后的TIF文件路径。

    :param input_files: 输入TIF文件列表
    :return: 合并后的TIF文件路径
    """

    # 创建VRT文件并保存在内存中
    vrt_options = gdal.BuildVRTOptions(separate=True)  # 每个tif一个波段
    vrt = gdal.BuildVRT(output_path, input_files, options=vrt_options)
    if vrt is None:
        raise Exception("Failed to create VRT file in memory")

    # 将VRT文件转换为TIF文件
    translate_options = gdal.TranslateOptions(format="GTiff")
    gdal.Translate(output_path, vrt, options=translate_options)

    # 清理内存中的VRT文件
    gdal.Unlink(output_path)
    
    print(f"波段合并完成...")
    return output_tif_in_memory 


def merge_tiles(input_files, output_path):
    # 动态生成内存中的VRT文件路径
    output_vrt_in_memory = "/vsimem/merged.vrt"
    
    # 创建VRT文件并保存在内存中
    vrt_options = gdal.BuildVRTOptions(separate=False)  # 将所有输入文件合并为一个波段
    vrt = gdal.BuildVRT(output_vrt_in_memory, input_files, options=vrt_options)
    if vrt is None:
        raise Exception("Failed to create VRT file in memory")

    # 将VRT文件转换为TIF文件，并保存在内存中
    translate_options = gdal.TranslateOptions(format="GTiff")
    gdal.Translate(output_path, vrt, options=translate_options)

    # 清理内存中的VRT文件
    gdal.Unlink(output_vrt_in_memory)

    print(f"瓦片合并完成...")
    return output_path


def merge_tiles_in_memory(input_files):
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




def calculate_hh(input_tif, output_path, factor):
    print('------------------------------------------')
    
    ds = gdal.Open(input_tif)
    imageData = ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    
    # 核心计算
    hh = imageData * factor
    
    geo_transform = ds.GetGeoTransform()
    projection = ds.GetProjection()
    cols, rows = imageData.shape
    
    driver = gdal.GetDriverByName("GTiff")
    hh_ds = driver.Create(output_path, rows, cols, 1, gdal.GDT_Float32)
    
    hh_ds.SetGeoTransform(geo_transform)
    hh_ds.SetProjection(projection)
    
    hh_ds.GetRasterBand(1).WriteArray(hh)
    hh_ds.GetRasterBand(1).SetNoDataValue(-9999)
    print(f"HH计算完毕....")
    
    # 清理内存
    ds, hh_ds = None, None
    return output_path


def get_tif_paths(directory):
    """获取指定目录下的所有tif文件路径"""
    import os
    tif_paths = []
    for file in os.listdir(directory):
        if file.endswith('.tif'):
            tif_paths.append(os.path.join(directory, file))
    return tif_paths


if __name__ == "__main__":
    tif_paths = get_tif_paths("D:\\edgedownload\\LT51190382000261BJC00\\82个")
    out_path_1 = os.path.join(os.path.dirname(__file__), 'hh.tif')
    out_path_2 = os.path.join(os.path.dirname(__file__), 'hh_2.tif')
    out_path_3 = os.path.join(os.path.dirname(__file__), 'hh_3.tif')
    out_path_4 = os.path.join(os.path.dirname(__file__), 'hh_4.tif')
    json_path = os.path.join(os.path.dirname(__file__), 'data.json')
    
    start_time = datetime.now()
    output_tif_in_memory = merge_tiles_in_memory(tif_paths)
    print(f"合并完成，用时{datetime.now() - start_time}")
    
    # 模拟计算历年ndvi
    calculate_hh(output_tif_in_memory, out_path_1, -1)
    calculate_hh(output_tif_in_memory, out_path_2, -0.5)
    calculate_hh(output_tif_in_memory, out_path_3, -0.25)
    calculate_hh(output_tif_in_memory, out_path_4, -0.1)
    
    # 基于给定点采样生成历年ndvi曲线
    point = [119.295706, 31.603155]
    # point2 = [119.303756, 31.627172]
    # point3 = [119.3875531, 31.0419943]
    
    data = {
        'date':[],
        'value':[],
    }
    for i, tif_path in enumerate([out_path_1, out_path_2, out_path_3, out_path_4]):
        time = datetime(2012 + i, 1, 1)
        data['date'].append(time.strftime('%Y/%m/%d'))
        data['value'].append(float(sample_raster(tif_path, point[0], point[1])))
    
    with open(json_path, 'w') as f:
        json.dump(data, f, indent=4)
