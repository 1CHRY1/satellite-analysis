from osgeo import gdal
from datetime import datetime
from .gridUtil import GridHelper

################## 瓦片和波端处理 ###########################
def merge_bands(input_files, output_path):
    # 创建VRT文件并保存在内存中
    vrt_options = gdal.BuildVRTOptions(separate=True)  # 每个tif一个波段
    vrt = gdal.BuildVRT(output_path, input_files, options=vrt_options)
    if vrt is None:
        raise Exception("Failed to create VRT file in memory")

    # 将VRT文件转换为TIF文件
    translate_options = gdal.TranslateOptions(format="GTiff")
    gdal.Translate(output_path, vrt, options=translate_options)

    gdal.Unlink(output_path)
    print(f"波段合并完成...")
    return output_path 


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

    gdal.Unlink(output_vrt_in_memory)
    print(f"瓦片合并完成...")
    return output_path


def merge_tiles_in_memory(input_files):
    # 动态生成内存中的VRT文件路径
    output_vrt_in_memory = "/vsimem/merged" + datetime.now().strftime("%Y%m%d%H%M%S") + ".vrt"
    output_tif_in_memory = "/vsimem/merged" + datetime.now().strftime("%Y%m%d%H%M%S") + ".tif"

    # 创建VRT文件并保存在内存中
    vrt_options = gdal.BuildVRTOptions(separate=False)  # 将所有输入文件合并为一个波段
    vrt = gdal.BuildVRT(output_vrt_in_memory, input_files, options=vrt_options)
    if vrt is None:
        raise Exception("Failed to create VRT file in memory")

    translate_options = gdal.TranslateOptions(format="GTiff")
    gdal.Translate(output_tif_in_memory, vrt, options=translate_options)

    gdal.Unlink(output_vrt_in_memory)
    print(f"瓦片合并完成，输出文件已保存到虚拟内存...")
    return output_tif_in_memory  # 返回内存中的TIF文件路径，供后续处理


def sample_raster(raster_path, longitude, latitude, band=1):
    """
    将WGS84经纬度坐标点匹配到栅格的像素上并进行采样, 默认tif也是wgs84坐标系, 采样第一个波段

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


################## 几何对象筛选格网 #########################

def polygon_to_grid_cells(polygon, tile_resolution = 1):
    grid_helper = GridHelper(tile_resolution)
    grid_cells = grid_helper.get_grid_cells(polygon)
    return grid_cells


def point_to_grid_cell(longitude, latitude, tile_resolution = 1):
    grid_helper = GridHelper(tile_resolution)
    grid_cell = grid_helper.get_grid_cell(longitude, latitude)
    return grid_cell

