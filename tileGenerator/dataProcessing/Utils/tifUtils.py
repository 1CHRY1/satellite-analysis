import os
from shapely.ops import transform
from PIL import Image
import io
import numpy as np
from osgeo import gdal, ogr, osr
from functools import partial
import pyproj
from shapely.geometry import box

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

def generateColorfulTile(red_path, green_path, blue_path):
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

    driver = gdal.GetDriverByName('MEM')
    mem_ds = driver.Create(
        '',
        ds1.RasterXSize,
        ds1.RasterYSize,
        4,  # 3个波段对应RGB
        gdal.GDT_Byte
    )
    mem_ds.SetGeoTransform(ds1.GetGeoTransform())
    mem_ds.SetProjection(ds1.GetProjection())

    for i in range(3):
        out_band = mem_ds.GetRasterBand(i + 1)
        out_band.WriteArray(rgb[:, :, i])

    # 写入 Alpha 通道
    alpha_band = mem_ds.GetRasterBand(4)
    alpha_band.WriteArray(alpha)

    # 创建内存中的文件对象
    buffer = io.BytesIO()

    # 将内存数据集复制到GTiff格式并写入buffer
    vsi_path = '/vsimem/output.tif'
    tiff_driver = gdal.GetDriverByName('GTiff')
    tiff_ds = tiff_driver.CreateCopy(vsi_path, mem_ds, 0)
    tiff_ds.FlushCache()

    # 获取数据并写入buffer
    for i in range(3):
        band = tiff_ds.GetRasterBand(i + 1)
        data = band.ReadAsArray()
        buffer.write(data.tobytes())

    buffer.seek(0)
    data_length = len(buffer.getvalue())
    # 释放资源
    mem_ds = None
    tiff_ds = None

    return buffer, data_length

def tif2Png(tif_buffer, quality=10):
    try:
        img = Image.open(tif_buffer)
        buffer = io.BytesIO()
        img.save(buffer, 'PNG', optimize=True, quality=quality)
        buffer.seek(0)
        data_length = len(buffer.getvalue())
        return buffer, data_length
    except Exception as e:
        return None

def is_polygon_intersect_raster(raster_path, polygon):
    """
    判断投影转换后的 Polygon 是否超出栅格范围
    :param raster_path: 输入栅格文件路径
    :param polygon: Shapely Polygon 对象
    :return: True（未超出栅格范围）或 False（完全超出栅格范围）
    """
    # 打开栅格数据
    # 打开栅格数据
    raster_ds = gdal.Open(raster_path, gdal.GA_ReadOnly)
    if not raster_ds:
        raise Exception(f"无法打开栅格文件: {raster_path}")

    try:
        # 获取栅格的地理参考和投影
        geo_transform = raster_ds.GetGeoTransform()
        raster_srs = osr.SpatialReference(wkt=raster_ds.GetProjection())

        # 定义源投影（EPSG:4326）和目标投影（栅格的投影）
        source_srs = osr.SpatialReference()
        source_srs.ImportFromEPSG(4326)
        target_srs = osr.SpatialReference(wkt=raster_srs.ExportToWkt())

        # 创建投影转换函数
        proj_transform = partial(
            pyproj.transform,
            pyproj.Proj(source_srs.ExportToProj4()),  # 源投影
            pyproj.Proj(target_srs.ExportToProj4())  # 目标投影
        )

        # 将 Shapely 的 Polygon 转换到栅格的投影坐标系
        projected_polygon = transform(proj_transform, polygon)

        # 获取栅格的地理范围
        xmin_raster = geo_transform[0]
        ymax_raster = geo_transform[3]
        xmax_raster = xmin_raster + geo_transform[1] * raster_ds.RasterXSize
        ymin_raster = ymax_raster + geo_transform[5] * raster_ds.RasterYSize

        # 创建栅格范围的多边形
        raster_polygon = box(xmin_raster, ymin_raster, xmax_raster, ymax_raster)

        # 判断投影后的 Polygon 是否完全在栅格范围内
        return raster_polygon.contains(projected_polygon)

    finally:
        # 关闭数据源
        if raster_ds:
            raster_ds = None

def is_polygon_boundary_valid(raster_path, polygon):
    """
    判断 Polygon 的每个边界点所落地的栅格值是否都是空值
    :param raster_path: 输入栅格文件路径
    :param polygon: Shapely Polygon 对象
    :return: True（至少有一个边界点的栅格值不是空值）或 False（所有边界点的栅格值都是空值）
    """
    # 打开栅格数据
    raster_ds = gdal.Open(raster_path, gdal.GA_ReadOnly)
    if not raster_ds:
        raise Exception(f"无法打开栅格文件: {raster_path}")

    try:
        # 获取栅格的地理参考和投影
        geo_transform = raster_ds.GetGeoTransform()
        raster_srs = osr.SpatialReference(wkt=raster_ds.GetProjection())

        # 定义源投影（EPSG:4326）和目标投影（栅格的投影）
        source_srs = osr.SpatialReference()
        source_srs.ImportFromEPSG(4326)
        target_srs = osr.SpatialReference(wkt=raster_srs.ExportToWkt())

        # 创建投影转换函数
        proj_transform = partial(
            pyproj.transform,
            pyproj.Proj(source_srs.ExportToProj4()),  # 源投影
            pyproj.Proj(target_srs.ExportToProj4())  # 目标投影
        )

        # 将 Shapely 的 Polygon 转换到栅格的投影坐标系
        projected_polygon = transform(proj_transform, polygon)

        # 获取栅格的第一波段
        band = raster_ds.GetRasterBand(1)
        no_data_value = band.GetNoDataValue()

        # 获取 Polygon 的边界点
        boundary_points = list(projected_polygon.exterior.coords)

        # 遍历每个边界点
        for point in boundary_points:
            # 将地理坐标转换为像素坐标
            px = int((point[0] - geo_transform[0]) / geo_transform[1])
            py = int((point[1] - geo_transform[3]) / geo_transform[5])

            # 检查像素坐标是否在栅格范围内
            if 0 <= px < raster_ds.RasterXSize and 0 <= py < raster_ds.RasterYSize:
                # 读取栅格值
                value = band.ReadAsArray(px, py, 1, 1)[0][0]
                # 如果该值不是空值，返回 True
                if value != no_data_value:
                # if value != no_data_value and value != 0:
                    return True

        # 如果所有边界点的栅格值都是空值，返回 False
        return False

    finally:
        # 关闭数据源
        if raster_ds:
            raster_ds = None

def clip_raster_by_polygon(raster_path, polygon, output_path):
    """
    栅格裁剪函数（基于 Polygon）
    :param raster_path: 输入栅格文件路径
    :param polygon: Shapely Polygon 对象
    :param output_path: 输出裁剪后的栅格文件路径
    """

    # 打开栅格数据
    raster_ds = gdal.Open(raster_path, gdal.GA_ReadOnly)
    if not raster_ds:
        raise Exception(f"无法打开栅格文件: {raster_path}")

    try:
        # 获取栅格的地理参考和投影
        geo_transform = raster_ds.GetGeoTransform()
        raster_srs = osr.SpatialReference(wkt=raster_ds.GetProjection())

        # 定义源投影（EPSG:4326）和目标投影（栅格的投影）
        source_srs = osr.SpatialReference()
        source_srs.ImportFromEPSG(4326)
        target_srs = osr.SpatialReference(wkt=raster_srs.ExportToWkt())

        # 创建投影转换函数
        proj_transform = partial(
            pyproj.transform,
            pyproj.Proj(source_srs.ExportToProj4()),  # 源投影
            pyproj.Proj(target_srs.ExportToProj4())  # 目标投影
        )

        # 将 Shapely 的 Polygon 转换到栅格的投影坐标系
        projected_polygon = transform(proj_transform, polygon)

        # 获取裁剪边界的范围
        xmin, ymin, xmax, ymax = projected_polygon.bounds

        # 计算裁剪区域的像素范围
        x1 = max(0, int((xmin - geo_transform[0]) / geo_transform[1]))
        y1 = max(0, int((ymax - geo_transform[3]) / geo_transform[5]))
        x2 = min(raster_ds.RasterXSize, int((xmax - geo_transform[0]) / geo_transform[1]))
        y2 = min(raster_ds.RasterYSize, int((ymin - geo_transform[3]) / geo_transform[5]))

        # 获取栅格的波段数量
        num_bands = raster_ds.RasterCount

        # 创建输出栅格文件
        driver = gdal.GetDriverByName("GTiff")
        out_ds = driver.Create(output_path, x2 - x1, y2 - y1, num_bands, raster_ds.GetRasterBand(1).DataType)

        # 设置输出文件的地理参考和投影
        out_ds.SetGeoTransform((xmin, geo_transform[1], geo_transform[2], ymax, geo_transform[4], geo_transform[5]))
        out_ds.SetProjection(raster_srs.ExportToWkt())

        # 逐波段读取和写入数据
        for band_index in range(1, num_bands + 1):
            band = raster_ds.GetRasterBand(band_index)
            data = band.ReadAsArray(x1, y1, x2 - x1, y2 - y1)
            out_band = out_ds.GetRasterBand(band_index)
            out_band.WriteArray(data)
            out_band.SetNoDataValue(band.GetNoDataValue() or 0)
            out_band.FlushCache()

        print(f"裁剪完成，结果已保存到: {output_path}")
    finally:
        # 关闭数据源
        if raster_ds:
            raster_ds = None
        if out_ds:
            out_ds = None


