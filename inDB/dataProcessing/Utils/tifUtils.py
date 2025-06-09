import os
import tempfile

import rasterio
from rio_cogeo.cogeo import cog_translate, cog_info
from rio_cogeo.profiles import cog_profiles

########## TIF 转 COG ##########
def convert_tif2cog(tif_path):
    if cog_info(tif_path)["COG"]:
        return tif_path
    else:
        temp_dir = tempfile.gettempdir()
        output_cog_tif = os.path.join(temp_dir, os.path.basename(tif_path))
        with rasterio.open(tif_path) as src:
            profile = cog_profiles.get("deflate")
            # cog_translate(src, output_cog_tif, profile, in_memory=False)
            cog_translate(src, output_cog_tif, profile, in_memory=True, overview_resampling="nearest", resampling="nearest", allow_intermediate_compression=False, temporary_compression="LZW", config={"GDAL_NUM_THREADS": "ALL_CPUS"})
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