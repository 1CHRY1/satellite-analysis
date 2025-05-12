import os
import tempfile
import uuid

import rasterio
from rio_cogeo.cogeo import cog_translate, cog_info
from rio_cogeo.profiles import cog_profiles
from PIL import Image
import numpy as np

########## DEPRECATED ##########
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

########## 创建预览图；DEPRECATED ##########
def create_preview_image(ds, output_image, scale_factor=1.0):
    """
    创建多波段影像的彩色合成预览图
    :param input_raster: 输入的多波段影像文件路径
    :param output_image: 输出的预览图文件路径
    :param scale_factor: 缩放比例，默认为0.1（即缩小到原图的10%）
    """

    # 获取影像的波段数量
    band_count = ds.RasterCount
    if band_count < 3:
        raise Exception("影像波段数不足，无法进行彩色合成")

    # 提取前三个波段（假设为RGB）
    band1 = ds.GetRasterBand(1).ReadAsArray()
    band2 = ds.GetRasterBand(2).ReadAsArray()
    band3 = ds.GetRasterBand(3).ReadAsArray()

    # 将波段数据缩放到0-255范围
    band1 = (band1 - np.min(band1)) / (np.max(band1) - np.min(band1)) * 255
    band2 = (band2 - np.min(band2)) / (np.max(band2) - np.min(band2)) * 255
    band3 = (band3 - np.min(band3)) / (np.max(band3) - np.min(band3)) * 255

    # 将波段数据转换为整型
    band1 = band1.astype(np.uint8)
    band2 = band2.astype(np.uint8)
    band3 = band3.astype(np.uint8)

    # 合成RGB图像
    rgb_image = np.stack([band1, band2, band3], axis=-1)

    # 缩放图像
    height, width, _ = rgb_image.shape
    new_width = int(width * scale_factor)
    new_height = int(height * scale_factor)
    preview_image = np.array(Image.fromarray(rgb_image).resize((new_width, new_height), Image.Resampling.LANCZOS))

    # 保存预览图
    img = Image.fromarray(preview_image)
    # 可选择设置图片的最大尺寸
    max_size = (1024, 1024)
    img.thumbnail(max_size, Image.Resampling.LANCZOS)
    # 保存图片
    img.save(output_image, format='PNG', optimize=True)
    return output_image


########## TIF 转 COG ##########
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

def convert_bbox_to_4326(dataset):
    """
    将GDAL数据集的边界框转换为EPSG:4326坐标系下的MySQL GEOMETRY格式
    包含对PROJ库配置问题的处理

    参数:
        dataset: GDAL数据集对象 (通过gdal.Open()获取)

    返回:
        str: MySQL GEOMETRY格式的WKT字符串，表示EPSG:4326坐标系下的边界框
    """
    from osgeo import gdal, osr
    import sys

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

########## QA 云量相关 ##########
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
