import os
import tempfile
from rio_cogeo.cogeo import cog_validate, cog_translate, cog_info
from rio_cogeo.profiles import cog_profiles
import rasterio
from PIL import Image
import io
import numpy as np
from osgeo import gdal
from collections import defaultdict



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
    # --------- Merge tif Using warp ----------------------------------
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

def mtif_v2(tif_paths, output_path):
    # --------- Merge tif using Translate --------------------------------------
    from osgeo import gdal

    # 执行合并操作
    gdal.Translate(output_path, gdal.BuildVRT("", tif_paths),
                   options=gdal.TranslateOptions(
                       creationOptions=["COMPRESS=LZW"],
                       format="GTiff"
                   ))

    return output_path


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

