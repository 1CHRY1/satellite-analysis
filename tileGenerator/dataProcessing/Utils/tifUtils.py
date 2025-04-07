import copy
import json
import math
import os
import tempfile
import rasterio
from rio_cogeo.cogeo import cog_validate, cog_translate, cog_info
from types import SimpleNamespace
from rio_cogeo.profiles import cog_profiles
import rasterio
from shapely.ops import transform
from PIL import Image
import io
import numpy as np
from osgeo import gdal, ogr, osr
from functools import partial
import pyproj
from shapely.geometry import box
import subprocess
from collections import defaultdict
from dataProcessing.Utils.filenameUtils import extract_landsat7_info
from dataProcessing.Utils.osUtils import uploadFileToMinio, uploadLocalFile, uploadLocalDirectory
from dataProcessing.tifSlicer.tifSlice import tif2GeoCS, rasterInfo, lnglat2grid, grid2lnglat, grid_size_in_degree, \
    geo_to_pixel, grids2Geojson
import dataProcessing.config as config


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

    rgb_image = np.stack((red_stretched, green_stretched, blue_stretched), axis=-1)

    # 使用PIL将NumPy数组转换为图像
    img = Image.fromarray(rgb_image)

    # 将生成的图像保存为TIF格式
    buffer = io.BytesIO()
    img.save(buffer, format='TIFF')
    buffer.seek(0)

    # 获取数据长度
    data_length = len(buffer.getvalue())

    return buffer, data_length


def tif2Png(tif_buffer, quality=10):
    try:
        tif_data = tif_buffer.getvalue()  # 获取 TIF 数据
        mem_driver = gdal.GetDriverByName('MEM')  # 使用内存驱动创建虚拟文件
        gdal.FileFromMemBuffer('/vsimem/temp.tif', tif_data)  # 把 TIF 数据加载到 GDAL 虚拟文件系统
        ds = gdal.Open('/vsimem/temp.tif')  # 读取虚拟文件
        band = ds.GetRasterBand(1)  # 取第一个波段
        no_data_value = band.GetNoDataValue()  # 获取 NoData 值
        if no_data_value is None:
            no_data_value = 0  # 默认 NoData 值

        img = Image.open(io.BytesIO(tif_data)).convert('RGBA')  # 转换为 RGBA 模式
        img_data = np.array(img)  # 转 NumPy 数组

        # 设置透明：如果像素值等于 NoData 值，则让 Alpha 通道透明
        mask = (img_data[:, :, 0] == no_data_value) & \
               (img_data[:, :, 1] == no_data_value) & \
               (img_data[:, :, 2] == no_data_value)
        img_data[mask, 3] = 0  # 设置 Alpha 通道为 0（透明）

        img = Image.fromarray(img_data)  # 重新转换为图片

        buffer = io.BytesIO()
        max_size = (2000, 2000)
        img.thumbnail(max_size, Image.ANTIALIAS)
        img.save(buffer, 'PNG', optimize=True, quality=quality)  # 以 PNG 格式保存
        buffer.seek(0)
        data_length = len(buffer.getvalue())

        gdal.Unlink('/vsimem/temp.tif')  # 清理虚拟文件

        return buffer, data_length
    except Exception as e:
        return None


def convert_bbox_to_4326(dataset):
    """
    将GDAL数据集的边界框转换为EPSG:4326坐标系下的MySQL GEOMETRY格式
    包含对PROJ库配置问题的处理

    参数:
        dataset: GDAL数据集对象 (通过gdal.Open()获取)

    返回:
        str: MySQL GEOMETRY格式的WKT字符串，表示EPSG:4326坐标系下的边界框
    """
    import numpy as np
    from osgeo import gdal, osr
    import sys
    import os

    # 获取数据集的地理变换参数
    gt = dataset.GetGeoTransform()

    # 获取数据集的尺寸
    width = dataset.RasterXSize
    height = dataset.RasterYSize

    # 计算边界框的四个角点 (像素坐标)
    # (左上，右上，右下，左下)
    pixel_corners = [
        (0, 0),
        (width, 0),
        (width, height),
        (0, height)
    ]

    # 将像素坐标转换为地理坐标
    geo_corners = []
    for px, py in pixel_corners:
        x = gt[0] + px * gt[1] + py * gt[2]
        y = gt[3] + px * gt[4] + py * gt[5]
        geo_corners.append((x, y))

    # 获取数据集的空间参考
    src_srs = osr.SpatialReference()
    src_wkt = dataset.GetProjection()

    # 如果没有投影信息，尝试从元数据获取EPSG代码
    if not src_wkt:
        epsg = None
        metadata = dataset.GetMetadata()
        for key in metadata:
            if "EPSG" in key.upper():
                try:
                    epsg = int(metadata[key].replace("EPSG:", ""))
                    break
                except:
                    pass

        if epsg:
            try:
                src_srs.ImportFromEPSG(epsg)
            except:
                print("警告: 无法从EPSG代码导入空间参考")
                return None
        else:
            print("警告: 数据集没有定义投影信息")
            return None
    else:
        src_srs.ImportFromWkt(src_wkt)

    # 创建目标空间参考 (EPSG:4326 - WGS84)
    dst_srs = osr.SpatialReference()

    try:
        # 尝试导入EPSG:4326
        dst_srs.ImportFromEPSG(4326)
    except Exception as e:
        print(f"警告: 无法导入EPSG:4326 - {str(e)}")
        print("可能是PROJ库配置问题")

        # 尝试使用WKT定义WGS84
        wgs84_wkt = """GEOGCS["WGS 84",
            DATUM["WGS_1984",
                SPHEROID["WGS 84",6378137,298.257223563,
                    AUTHORITY["EPSG","7030"]],
                AUTHORITY["EPSG","6326"]],
            PRIMEM["Greenwich",0,
                AUTHORITY["EPSG","8901"]],
            UNIT["degree",0.0174532925199433,
                AUTHORITY["EPSG","9122"]],
            AUTHORITY["EPSG","4326"]]"""

        try:
            dst_srs.ImportFromWkt(wgs84_wkt)
        except Exception as e2:
            print(f"警告: 无法使用WKT定义WGS84 - {str(e2)}")
            print("正在检查PROJ库配置...")

            # 打印PROJ相关环境变量
            print(f"PROJ_LIB环境变量: {os.environ.get('PROJ_LIB', '未设置')}")

            # 尝试查找PROJ库文件
            proj_paths = [
                "/usr/share/proj",
                "/usr/local/share/proj",
                "/opt/share/proj",
                "C:\\OSGeo4W\\share\\proj",
                "C:\\Program Files\\GDAL\\projlib"
            ]

            for path in proj_paths:
                if os.path.exists(path):
                    print(f"找到可能的PROJ数据目录: {path}")
                    print(f"尝试设置PROJ_LIB环境变量到: {path}")
                    os.environ["PROJ_LIB"] = path

                    try:
                        dst_srs = osr.SpatialReference()
                        dst_srs.ImportFromEPSG(4326)
                        print("成功导入EPSG:4326！")
                        break
                    except:
                        print(f"设置环境变量后仍无法导入EPSG:4326")

            if not dst_srs.IsGeographic():
                print("警告: 无法配置PROJ库。尝试手动构建坐标转换...")
                return None

    # 创建坐标转换
    try:
        # 为避免旧版本问题，先设置坐标轴顺序为传统顺序
        # GDAL 3.x默认使用PROJ 6以上新的坐标轴顺序约定
        try:
            src_srs.SetAxisMappingStrategy(osr.OAMS_TRADITIONAL_GIS_ORDER)
            dst_srs.SetAxisMappingStrategy(osr.OAMS_TRADITIONAL_GIS_ORDER)
        except:
            # 旧版GDAL可能没有这个方法
            pass

        transform = osr.CoordinateTransformation(src_srs, dst_srs)

        # 测试转换是否工作
        try:
            test_point = transform.TransformPoint(geo_corners[0][0], geo_corners[0][1], 0)
            print("坐标转换测试成功!")
        except Exception as e:
            print(f"坐标转换测试失败: {str(e)}")
            print("尝试使用pyproj进行转换...")

            try:
                import pyproj

                # 从WKT获取PROJ4字符串
                src_proj4 = src_srs.ExportToProj4()

                # 定义转换
                transformer = pyproj.Transformer.from_crs(
                    src_proj4,
                    "epsg:4326",
                    always_xy=True
                )

                # 转换函数
                def transform_with_pyproj(x, y):
                    lon, lat = transformer.transform(x, y)
                    return (lon, lat, 0)

                # 替换transform.TransformPoint
                transform.TransformPoint = transform_with_pyproj
                print("已使用pyproj替代GDAL的坐标转换功能")
            except ImportError:
                print("pyproj库未安装，无法进行坐标转换")
                return None
            except Exception as e:
                print(f"使用pyproj转换时出错: {str(e)}")
                return None
    except Exception as e:
        print(f"创建坐标转换时出错: {str(e)}")
        print("PROJ库可能未正确配置")
        return None

    # 将地理坐标转换为EPSG:4326坐标系
    wgs84_corners = []
    try:
        for x, y in geo_corners:
            try:
                # 尝试使用不同的TransformPoint调用方式
                try:
                    # 方式1：传入数组
                    xyz = [0, 0, 0]
                    transform.TransformPoint(xyz, x, y, 0)
                    point = (xyz[0], xyz[1], xyz[2])
                except:
                    try:
                        # 方式2：单独坐标
                        point = transform.TransformPoint(x, y, 0)
                    except:
                        # 方式3：如果之前用pyproj替换了函数
                        point = transform.TransformPoint(x, y)

                wgs84_corners.append((point[0], point[1]))
            except Exception as e:
                print(f"转换点({x}, {y})时出错: {str(e)}")
                return None
    except Exception as e:
        print(f"坐标转换过程中出错: {str(e)}")
        return None

    if not wgs84_corners or len(wgs84_corners) != 4:
        print("坐标转换失败，无法生成有效的边界框")
        return None

    # 添加起始点以闭合多边形（需要与第一个点相同）
    wgs84_corners.append(wgs84_corners[0])

    # 创建WKT格式的POLYGON字符串
    coords_str = ", ".join([f"{x} {y}" for x, y in wgs84_corners])
    wkt = f"POLYGON(({coords_str}))"

    return wkt


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


def process_and_upload(files_dict: dict, bucket_name: str, object_prefix: str):
    """
    处理每个影像ID及其对应的波段路径，生成真彩色合成的PNG，并上传至MinIO。
    :param files_dict: 包含影像ID和对应波段路径列表的字典
    :param bucket_name: MinIO存储桶名称
    :param object_prefix: 上传文件时的对象名称前缀（例如，路径）
    """
    scene_info_list = []

    #--------- process band files according to scene_name -----------------------------------
    for scene_name, band_paths in files_dict.items():
        scene_info = SimpleNamespace(image_info_list=[], bands=[], scene_name="")
        # Make sure band_paths have B1, B2, B3, QA_PIXEL
        b1_path = None
        b2_path = None
        b3_path = None
        qa_data = None

        #--------- process a band file one by one ------------------------------------------
        for path in band_paths:
            # Upload TIF
            band_name = os.path.basename(path)
            scene_info.scene_name = scene_name
            object_name = f"{object_prefix}/tif/{scene_info.scene_name}/{band_name}"
            uploadLocalFile(convert_tif2cog(path), bucket_name, object_name)
            # Calculate the cloud coverage(for the whole image)
            if qa_data is None:
                qa_data, no_data_value = read_qa_pixel(path, scene_info.scene_name)
                scene_info.cloud = get_cloud_coverage4image(qa_data, no_data_value)
            # Get b1/b2/b3 path
            if 'B1' in os.path.basename(path):
                b1_path = path
            elif 'B2' in os.path.basename(path):
                b2_path = path
            elif 'B3' in os.path.basename(path):
                b3_path = path
            # Process tiles and get tile info list
            with tempfile.TemporaryDirectory() as temp_dir:
                tile_info_list = process_tiles(path, scene_info.scene_name, temp_dir, config.GRID_RESOLUTION,
                                               config.MINIO_TILES_BUCKET,
                                               config.MINIO_GRID_BUCKET, object_prefix)
            # Prep image info
            image_info = extract_landsat7_info(path)
            image_info.tif_path = object_name
            image_info.cloud = scene_info.cloud
            image_info.png_path = None
            image_info.tile_info_list = tile_info_list
            # Prep scene info
            scene_info.png_path = None
            scene_info.bands.append(image_info.band)
            # 将一些景的公共属性复制到scene_info中
            vars(scene_info).update({key: getattr(image_info, key) for key in vars(image_info) if key in {
                "image_time", "crs", "bbox", "tile_level_num", "tile_levels", "resolution", "period"
            }})
            scene_info.image_info_list.append(image_info)
        # Calculate band num
        scene_info.band_num = len(scene_info.bands)

        #--------- Image true color synthesis and upload -----------------------------------
        if b1_path and b2_path and b3_path:
            tif_buffer, tif_dataLen = generateColorfulTile(b3_path, b2_path, b1_path)
            png_buffer, png_dataLen = tif2Png(tif_buffer, 10)
            object_name = f"{object_prefix}/png/{scene_name}.png"  # 可以根据需要调整路径和文件名
            for image_info in scene_info.image_info_list:
                image_info.png_path = object_name
                scene_info.png_path = object_name
            uploadFileToMinio(png_buffer, png_dataLen, bucket_name, object_name)
        else:
            print(f"Skipping {scene_name} as one or more required bands (B1, B2, B3) are missing.")

        scene_info_list.append(scene_info)
    return scene_info_list


# based on tifSlice.py(process)
def process_tiles(tiff_path, scene_name, output_dir, grid_resolution, tile_bucket, grid_bucket,
                  object_prefix, clearNodata=True):
    # --------- Prep required variables ---------------------------------
    tiff_file_name = os.path.basename(tiff_path)
    tiff_name = os.path.splitext(tiff_file_name)[0]  # 去除扩展名
    tile_info_list = []

    tif_in_geoCS = "/vsimem/temp.tif"  # GDAL 内存文件系统
    grid_resolution_in_meter = grid_resolution * 1000
    world_grid_num_x = math.ceil(config.EARTH_CIRCUMFERENCE / grid_resolution_in_meter)
    world_grid_num_y = math.ceil(config.EARTH_CIRCUMFERENCE / 2.0 / grid_resolution_in_meter)

    world_grid_num = [world_grid_num_x, world_grid_num_y]

    # --------- Project tif to geoCS ------------------------------------
    tif2GeoCS(tiff_path, tif_in_geoCS)

    ds = gdal.Open(tif_in_geoCS)
    if ds is None:
        print(f"Unable to open input image: {tiff_path}")
        return

    # --------- Calc pixelperDegree -------------------------------------
    lt, rb, degree_per_pixel, rotate, origin_size, geoTransform = rasterInfo(ds)

    # --------- Calc grid_lt, grid_rb -----------------------------------
    grid_lt_x, grid_lt_y = lnglat2grid(lt, world_grid_num)
    grid_rb_x, grid_rb_y = lnglat2grid(rb, world_grid_num)

    grid_lt_lng, grid_lt_lat = grid2lnglat(grid_lt_x, grid_lt_y, world_grid_num)

    # --------- Calc grid size in degree space -------------------------
    grid_d_size_x, grid_d_size_y = grid_size_in_degree(world_grid_num)

    # --------- Calc grid size in image pixel space ---------------------
    grid_p_size_x = math.ceil(grid_d_size_x / degree_per_pixel[0])
    grid_p_size_y = math.ceil(grid_d_size_y / degree_per_pixel[1])

    # --------- Create a virtual raster pixel bound ---------------------
    grid_num_x = grid_rb_x - grid_lt_x + 1
    grid_num_y = grid_rb_y - grid_lt_y + 1

    pixel_offset_x = (lt[0] - grid_lt_lng) * (1 / degree_per_pixel[0])
    pixel_offset_y = (grid_lt_lat - lt[1]) * (1 / degree_per_pixel[1])

    pixel_offset_x = round(pixel_offset_x)
    pixel_offset_y = round(pixel_offset_y)

    # --------- Get QA_PIXEL Data to calculate the cloud coverage for grid/tile ---------
    qa_data, no_data_value = read_qa_pixel(tiff_path, scene_name)

    # --------- Create Slice base on virtual raster pixel bound ---------

    grid_id_list = []

    for i in range(grid_num_x):
        for j in range(grid_num_y):
            # x = i * grid_p_size_x - pixel_offset_x
            # y = j * grid_p_size_y - pixel_offset_y
            grid_id_x = grid_lt_x + i
            grid_id_y = grid_lt_y + j
            grid_lng, grid_lat = grid2lnglat(grid_id_x, grid_id_y, world_grid_num)
            x, y = geo_to_pixel(grid_lng, grid_lat, geoTransform)
            w = grid_p_size_x
            h = grid_p_size_y

            if (clearNodata):
                # --------- Skip out of padding grid ------------------------
                if (x < 0 or y < 0 or (x + w) > origin_size[0] or (y + h) > origin_size[1]):
                    continue

                # --------- Clean no data slice -----------------------------
                tile_data = ds.ReadAsArray(x, y, w, h)
                if np.all(tile_data == no_data_value):
                    continue

            grid_id_list.append([grid_id_x, grid_id_y, i, j])
            cloud = get_cloud_coverage4grid(qa_data, x, y, w, h, no_data_value)
            tile_filename = os.path.join(output_dir, f"tile_{i}_{j}-{cloud:.2f}.tif")
            gdal.Translate(tile_filename, ds, srcWin=[x, y, w, h])

            # --------- Prep tile_info_list ----------------------------------------------#
            tile_info = {}
            left_lng, top_lat = grid2lnglat(grid_id_x, grid_id_y, world_grid_num)
            right_lng, bottom_lat = grid2lnglat(grid_id_x + 1, grid_id_y + 1, world_grid_num)
            wgs84_corners = [(left_lng, top_lat), (right_lng, top_lat), (right_lng, bottom_lat), (left_lng, bottom_lat),
                             (left_lng, top_lat)]
            coords_str = ", ".join([f"{x} {y}" for x, y in wgs84_corners])
            tile_info['bbox'] = f"POLYGON(({coords_str}))"
            tile_info['bucket'] = tile_bucket
            tile_info['cloud'] = cloud
            tile_info['path'] = f"{object_prefix}/{scene_name}/{tiff_name}/tile_{i}_{j}-{tile_info['cloud']:.2f}.tif"
            tile_info['column_id'] = grid_id_x
            tile_info['row_id'] = grid_id_y
            print(f"{i}, {j}")
            tile_info_list.append(tile_info)

    # --------- Upload tiles ------------------------------------------------------
    cmd = ["mc", "mirror", output_dir, f"myminio/{tile_bucket}/{object_prefix}/{scene_name}/{tiff_name}"]
    with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1,
                          universal_newlines=True) as process:
        for line in process.stdout:
            print(line, end="")  # 逐行输出，不额外换行
        for err in process.stderr:
            print("ERROR:", err, end="")  # 逐行输出错误信息
    process.wait()

    # --------- Create and upload grids geojson ------------------------------------
    geojson = grids2Geojson(grid_id_list, world_grid_num)
    with open(os.path.join(output_dir, "grid.geojson"), "w") as f:
        json.dump(geojson, f)
    uploadLocalFile(os.path.join(output_dir, "grid.geojson"), grid_bucket,
                    f"{object_prefix}/{scene_name}/{tiff_name}/grid.geojson")

    gdal.Unlink(tif_in_geoCS)
    ds = None
    return tile_info_list


def convert_tif2cog(tif_path):
    if cog_info(tif_path)["COG"]:
        return tif_path
    else:
        temp_dir = tempfile.gettempdir()
        output_cog_tif = os.path.join(temp_dir, os.path.basename(tif_path))
        with rasterio.open(tif_path) as src:
            profile = cog_profiles.get("deflate")
            cog_translate(src, output_cog_tif, profile, in_memory=False)
        return output_cog_tif


def validate_inputs(tif_paths):
    crs_list = [gdal.Open(path).GetProjection() for path in tif_paths]
    res_list = [gdal.Open(path).GetGeoTransform()[1] for path in tif_paths]
    if len(set(crs_list)) > 1:
        raise ValueError("坐标系不一致")
    if len(set(res_list)) > 1:
        raise ValueError("分辨率不一致")


def mtif(tif_paths, output_path):
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


def mband(merged_tif_list, output_path, output_name="merged.tif"):
    # --------- Merge tif and band ---------------------------------
    grouped_by_band = defaultdict(list)
    for entry in merged_tif_list:
        band = entry["band"]
        path = entry["path"]
        grouped_by_band[band].append(path)

    os.makedirs(output_path, exist_ok=True)
    output_file = os.path.join(output_path, output_name)

    # --------- Create output ds according to first tif -------------
    sample_ds = gdal.Open(merged_tif_list[0]["path"])
    geotransform = sample_ds.GetGeoTransform()
    projection = sample_ds.GetProjection()
    cols = sample_ds.RasterXSize
    rows = sample_ds.RasterYSize
    bands_num = len(grouped_by_band)
    driver = gdal.GetDriverByName('GTiff')
    out_ds = driver.Create(output_file, cols, rows, bands_num, gdal.GDT_Float32,
                           options=["TILED=YES", "COMPRESS=LZW"])  # 使用 LZW 压缩
    out_ds.SetProjection(projection)
    out_ds.SetGeoTransform(geotransform)

    # --------- Write data into output ds band by band --------------
    for i, (band, paths) in enumerate(sorted(grouped_by_band.items()), start=1):
        print(f"写入第 {i} 个波段...")
        band_data = []
        for path in paths:
            dataset = gdal.Open(path)
            band_data.append(dataset.GetRasterBand(1).ReadAsArray())
        merged_data = np.stack(band_data, axis=0)[0]
        out_band = out_ds.GetRasterBand(i)
        out_band.WriteArray(merged_data)

    # --------- Other steps after writing ---------------------------
    out_ds.FlushCache()
    out_ds.BuildOverviews(overviewlist=[1, 2, 4, 8, 16])
    out_ds = None  # 释放资源
    print(f"多波段 TIFF 影像已保存至 {output_file}")
    return output_file


def read_qa_pixel(tiff_path, scene_name):
    # --------- Read QA_PIXEL band ---------------------------------
    directory = os.path.dirname(tiff_path)
    qa_pixel_path = os.path.join(directory, f"{scene_name}_QA_PIXEL.TIF")
    with rasterio.open(qa_pixel_path) as src:
        qa_pixel_data = src.read(1)  # 读取第一波段数据

    # --------- Get no data value ----------------------------------
    with rasterio.open(tiff_path) as src:
        no_data_value = src.nodata
    if no_data_value is None:
        no_data_value = 0

    return qa_pixel_data, no_data_value


def get_cloud_coverage4grid(qa_pixel_data, x, y, w, h, no_data_value):
    # --------- Get cloud coverage for every grid ----------------------------

    # --------- Get QA_PIXEL data in grid ----------------------------
    row_min, row_max = max(0, y), min(qa_pixel_data.shape[0], y + h)
    col_min, col_max = max(0, x), min(qa_pixel_data.shape[1], x + w)
    grid_data = qa_pixel_data[row_min:row_max, col_min:col_max]

    # --------- Get Bit3(thick) and Bit2(thin) mask ------------------
    thick_cloud_mask = (grid_data & (1 << 3)) > 0
    thin_cloud_mask = (grid_data & (1 << 2)) > 0
    cloud_mask = thick_cloud_mask | thin_cloud_mask
    no_data_mask = grid_data == no_data_value  # 由外部提供 NoData 值
    valid_pixels = ~no_data_mask  # 有效像素区域

    # --------- Calculate cloud coverage -----------------------------
    total_valid_pixels = np.sum(valid_pixels)
    if total_valid_pixels == 0:
        return 0.0  # 如果网格内全是 NoData，则云覆盖率设为 0

    cloud_percentage = np.sum(cloud_mask & valid_pixels) / total_valid_pixels * 100

    return cloud_percentage


def get_cloud_coverage4image(qa_pixel_data, no_data_value):
    # --------- Get cloud coverage for every image ----------------------------

    # --------- Get Bit3(thick) and Bit2(thin) mask ------------------
    thick_cloud_mask = (qa_pixel_data & (1 << 3)) > 0
    thin_cloud_mask = (qa_pixel_data & (1 << 2)) > 0
    cloud_mask = thick_cloud_mask | thin_cloud_mask
    no_data_mask = qa_pixel_data == no_data_value  # 由外部提供 NoData 值
    valid_pixels = ~no_data_mask  # 有效像素区域

    # --------- Calculate cloud coverage -----------------------------
    cloud_percentage = np.sum(cloud_mask & valid_pixels) / np.sum(valid_pixels) * 100

    return cloud_percentage
