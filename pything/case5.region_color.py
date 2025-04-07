from ogms_xfer import OGMS_Xfer as xfer
from osgeo import gdal
import numpy as np
from PIL import Image
import json


def read_band(path):
    """读取单波段栅格数据"""
    ds = gdal.Open(path)
    if ds is None:
        raise ValueError(f"无法打开文件: {path}")
    band = ds.GetRasterBand(1)
    data = band.ReadAsArray()
    return data, ds


def percentile_stretch(array, lower=2, upper=98):
    """百分比截断拉伸增强"""
    # 排除无效值（假设NoData值为0）
    array = np.ma.masked_equal(array, 0)
    # 计算百分比范围
    lower_percentile = np.percentile(array.compressed(), lower)
    upper_percentile = np.percentile(array.compressed(), upper)
    # 应用拉伸
    stretched = np.clip(array, lower_percentile, upper_percentile)
    stretched = (stretched - lower_percentile) / (upper_percentile - lower_percentile) * 255
    return stretched.filled(0).astype(np.uint8)  # 填充无效值并转换为8位


def generateColorfulTile(red_path, green_path, blue_path, output_path):
    red_data, ds1 = read_band(red_path)
    green_data, ds2 = read_band(green_path)
    blue_data, ds3 = read_band(blue_path)
    if (ds1.GetProjection() != ds2.GetProjection()) or (ds1.GetProjection() != ds3.GetProjection()):
        raise ValueError("投影信息不一致")
    if (ds1.RasterXSize != ds2.RasterXSize) or (ds1.RasterYSize != ds2.RasterYSize) or \
            (ds1.RasterXSize != ds3.RasterXSize) or (ds1.RasterYSize != ds3.RasterYSize):
        raise ValueError("影像尺寸不一致")
    if (ds1.GetGeoTransform() != ds2.GetGeoTransform()) or (ds1.GetGeoTransform() != ds3.GetGeoTransform()):
        raise ValueError("地理变换参数不一致")

    # 对每个波段进行增强处理
    red_stretched = percentile_stretch(red_data)
    green_stretched = percentile_stretch(green_data)
    blue_stretched = percentile_stretch(blue_data)

    rgb_image = np.stack((red_stretched, green_stretched, blue_stretched), axis=-1)

    # 使用PIL将NumPy数组转换为图像
    img = Image.fromarray(rgb_image)
    img.save(output_path, format='TIFF')
    return output_path


def merge_tif(tif_paths, output_path):
    # --------- Merge tif --------------------------------------
    merge_options = gdal.WarpOptions(
        format="GTiff",
        cutlineDSName=None,
        srcSRS=None,  # 自动识别输入投影
        dstSRS=None,  # 保持输入投影
        width=0,  # 自动计算输出尺寸
        height=0,
        resampleAlg="near",  # 重采样算法（near/bilinear等）
        creationOptions=["COMPRESS=LZW"]
    )
    gdal.Warp(
        output_path,
        tif_paths,
        options=merge_options
    )





if __name__ == '__main__':
    
    config_file_path = "config.json"
    xfer.initialize(config_file_path)
    
    scene = xfer.Scene("SC906772444")
    image_red = scene.get_band_image("4")
    image_green = scene.get_band_image("3")
    image_blue = scene.get_band_image("2")
    
    with open(xfer.URL.dataUrl('区域.geojson'), "r") as f:
        region = json.load(f)
    
    red_region_tiles = image_red.get_tiles_by_polygon(region)
    green_region_tiles = image_green.get_tiles_by_polygon(region)
    blue_region_tiles = image_blue.get_tiles_by_polygon(region)
    
    red_tif_paths = [xfer.URL.resolve(tile.url) for tile in red_region_tiles]
    green_tif_paths = [xfer.URL.resolve(tile.url) for tile in green_region_tiles]
    blue_tif_paths = [xfer.URL.resolve(tile.url) for tile in blue_region_tiles]
    
    print(f"检索到{len(red_tif_paths)}个红光波段瓦片...")
    print(f"检索到{len(green_tif_paths)}个绿光波段瓦片...")
    print(f"检索到{len(blue_tif_paths)}个蓝光波段瓦片...")
    
    ref_tif_path = xfer.URL.outputUrl('red.tif')
    green_tif_path = xfer.URL.outputUrl('green.tif')
    blue_tif_path = xfer.URL.outputUrl('blue.tif')
    color_tif_path = xfer.URL.outputUrl('color.tif')
    
    merge_tif(red_tif_paths, ref_tif_path)
    print(f"红光波段影像合并完成 {ref_tif_path}")
    merge_tif(green_tif_paths, green_tif_path)
    print(f"绿光波段影像合并完成 {green_tif_path}")
    merge_tif(blue_tif_paths, blue_tif_path)
    print(f"蓝光波段影像合并完成 {blue_tif_path}")
    
    generateColorfulTile(ref_tif_path, green_tif_path, blue_tif_path, color_tif_path)
    print(f"真彩色合成已完成!")

