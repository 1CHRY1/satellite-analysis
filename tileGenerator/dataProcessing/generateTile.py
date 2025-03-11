from osgeo import gdal
import numpy as np

path = "D:\\1study\\Work\\2025_03_05_satellite\\tileGenerator"

band1 = "B1"
band2 = "B2"
band3 = "B3"
origin = "Origin"

zoom = '9-15'
tilesize = 256

def getTifPath(band):
    return f'{path}\\landset5\\LT51190382000261BJC00\\LT51190382000261BJC00_{band}.TIF'

def getTilePath(band):
    return f'{path}\\tiles\\{band}'

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

    # 合成RGB图像（注意波段顺序！）
    # 常规真彩色组合：B3(红), B2(绿), B1(蓝)
    rgb = np.dstack((red_stretched, green_stretched, blue_stretched))
    alpha = np.where(red_stretched == 0, 0, 255).astype(np.uint8)

    driver = gdal.GetDriverByName('GTiff')
    out_ds = driver.Create(
        output_path,
        ds1.RasterXSize,
        ds1.RasterYSize,
        4,  # 4个波段对应RGB+alpha
        gdal.GDT_Byte
    )
    out_ds.SetGeoTransform(ds1.GetGeoTransform())
    out_ds.SetProjection(ds1.GetProjection())

    for i in range(3):
        out_band = out_ds.GetRasterBand(i + 1)
        out_band.WriteArray(rgb[:, :, i])

    # 写入 Alpha 通道
    alpha_band = out_ds.GetRasterBand(4)
    alpha_band.WriteArray(alpha)

    out_ds.FlushCache()
    out_ds = None
    print("合成完成！输出文件:", output_path)

# def tif2tile(tifPath, tilePath, zoom, tileSize):
    # gdal2tiles.generate_tiles(tifPath, tilePath, zoom=zoom, tilesize=tileSize, srcnodata="0,0,0")

if __name__ == '__main__':
    generateColorfulTile(getTifPath(band3), getTifPath(band2), getTifPath(band1), getTifPath(origin))
    # tif2tile(getTifPath(origin), getTilePath(origin), "9-15", 256)

