import os, math, time, json, sys
from osgeo import gdal
import numpy as np

# 获取程序所在路径（对于打包的 .exe 文件）
# if getattr(sys, 'frozen', False):
#     # Nuitka 打包后的临时路径
#     base_path = os.path.dirname(sys.argv[0])
# else:
#     # 原始 Python 脚本路径
#     base_path = os.path.dirname(__file__)

# # proj.db存储在base_path下
# os.environ['PROJ_LIB'] = base_path

# print("os.environ['PROJ_LIB'] -- ", os.environ['PROJ_LIB'])

#
# Approximate radius of the earth in meters.
# Uses the WGS-84 approximation. The radius at the equator is ~6378137 and at the poles is ~6356752. https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84
# 6371008.8 is one published "average radius" see https://en.wikipedia.org/wiki/Earth_radius#Mean_radius, or ftp://athena.fsv.cvut.cz/ZFG/grs80-Moritz.pdf p.4
#
EarthRadius = 6371008.8

#
# The average circumference of the earth in meters.
#
EarthCircumference = 2 * math.pi * EarthRadius

def lnglat2grid(pos, world_grid_num):
    # Grid is arranged as follows:
    #
    # 0,0 -------------- maxX, 0
    #  +++++++++++++++++++++++++
    #  +++++++++++++++++++++++++
    # 0,maxY --------- maxX,maxY
    grid_x = math.floor((pos[0] + 180.0) / 360.0 * world_grid_num[0])
    grid_y = math.floor((90.0 - pos[1]) / 180.0 * world_grid_num[1])
    return grid_x, grid_y


def grid2lnglat(grid_x, grid_y, world_grid_num):
    """return the left-top geo position of the grid"""
    lng = grid_x / world_grid_num[0] * 360.0 - 180.0
    lat = 90.0 - grid_y / world_grid_num[1] * 180.0
    return lng, lat # left top (lng, lat)


def grid_size_in_degree(world_grid_num):
    size_x = 360.0 / world_grid_num[0]
    size_y = 180.0 / world_grid_num[1]
    return size_x, size_y


def rasterInfo(ds):

    x_size = ds.RasterXSize
    y_size = ds.RasterYSize
    origin_size = [x_size, y_size]
    info = ds.GetGeoTransform()
    left_top = [info[0], info[3]]
    right_bottom = [info[0] + x_size * info[1], info[3] + y_size * info[5]] # y is negative here
    resolution_per_pixel = [info[1], info[5] * (-1)]
    rotaion = [info[2], info[4]]

    return left_top, right_bottom, resolution_per_pixel, rotaion , origin_size, info


def geo_to_pixel(geo_x, geo_y, geo_transform):
    det = geo_transform[1] * geo_transform[5] - geo_transform[2] * geo_transform[4]
    if det == 0:
        raise ValueError("GeoTransform matrix is singular and cannot be inverted")

    inv_transform = [
        geo_transform[5] / det, -geo_transform[2] / det,
        -geo_transform[4] / det, geo_transform[1] / det
    ]

    pixel_x = inv_transform[0] * (geo_x - geo_transform[0]) + inv_transform[1] * (geo_y - geo_transform[3])
    pixel_y = inv_transform[2] * (geo_x - geo_transform[0]) + inv_transform[3] * (geo_y - geo_transform[3])

    return int(math.floor(pixel_x)), int(math.floor(pixel_y))  # 取整保证是像素索引

def tif2GeoCS(input_image, out_image):
    options = gdal.WarpOptions(dstSRS="EPSG:4326")  # 目标坐标系为 WGS 84
    gdal.Warp(out_image, input_image, options=options)



def process(input_image, output_dir, grid_resolution, clearNodata=True, convert_to_wgs84=False):
    # 初始化 GDAL 缓存
    # gdal.SetCacheMax(2**32)
    gdal.SetCacheMax(16 * 1024 * 1024 * 1024)  # 16GB
    
    print("开始读取影像...", time.strftime("%Y-%m-%d %H:%M:%S"))
    
    if convert_to_wgs84:
        tif_in_geoCS = "/vsimem/temp.tif"
        tif2GeoCS(input_image, tif_in_geoCS)
        ds = gdal.Open(tif_in_geoCS)
    else:
        ds = gdal.Open(input_image)
    
    if ds is None:
        print(f"Unable to open input image: {input_image}")
        return
    
    # 获取影像的波段数量
    band_count = ds.RasterCount
    
    no_data_values = {
        band_index: ds.GetRasterBand(band_index).GetNoDataValue() or 0
        for band_index in range(1, band_count + 1)
    }

    print("开始计算网格参数...", time.strftime("%Y-%m-%d %H:%M:%S"))
    # 计算网格参数
    grid_resolution_in_meter = grid_resolution * 1000
    world_grid_num_x = math.ceil(EarthCircumference / grid_resolution_in_meter)
    world_grid_num_y = math.ceil(EarthCircumference / 2.0 / grid_resolution_in_meter)
    world_grid_num = [world_grid_num_x, world_grid_num_y]

    # 获取影像信息
    lt, rb, degree_per_pixel, _, origin_size, geoTransform = rasterInfo(ds)

    # 计算网格大小
    grid_d_size_x, grid_d_size_y = grid_size_in_degree(world_grid_num)
    grid_p_size_x = math.ceil(grid_d_size_x / degree_per_pixel[0])
    grid_p_size_y = math.ceil(grid_d_size_y / degree_per_pixel[1])

    # 计算网格范围
    grid_lt_x, grid_lt_y = lnglat2grid(lt, world_grid_num)
    grid_rb_x, grid_rb_y = lnglat2grid(rb, world_grid_num)

    # 创建输出目录
    for band_index in range(1, band_count + 1):
        band_output_dir = os.path.join(output_dir, f"band_{band_index}")
        os.makedirs(band_output_dir, exist_ok=True)

    print("开始切片...", time.strftime("%Y-%m-%d %H:%M:%S"))
    # 遍历每个网格
    for i in range(grid_rb_x - grid_lt_x + 1):
        for j in range(grid_rb_y - grid_lt_y + 1):
            grid_id_x = grid_lt_x + i
            grid_id_y = grid_lt_y + j
            grid_lng, grid_lat = grid2lnglat(grid_id_x, grid_id_y, world_grid_num)
            x, y = geo_to_pixel(grid_lng, grid_lat, geoTransform)
            w = grid_p_size_x
            h = grid_p_size_y

            if clearNodata:
                if x < 0 or y < 0 or (x + w) > origin_size[0] or (y + h) > origin_size[1]:
                    continue

            for band_index in range(1, band_count + 1):
                band_output_dir = os.path.join(output_dir, f"band_{band_index}")
                tile_filename = os.path.join(band_output_dir, f"tile_{grid_id_x}_{grid_id_y}.tif")

                # 读取当前波段的数据
                band = ds.GetRasterBand(band_index)
                tile_data = band.ReadAsArray(x, y, w, h)

                # 检查是否包含 NoData 值
                no_data_value = no_data_values[band_index]
                if no_data_value is None:
                    no_data_value = 0

                if np.all(tile_data == no_data_value):
                    continue

                # 保存当前波段的瓦片
                gdal.Translate(tile_filename, ds, srcWin=[x, y, w, h], bandList=[band_index])
                
        # 打印进度
        progress = (i + 1) / (grid_rb_x - grid_lt_x + 1) * 100
        print(f"processing progress: {progress:.2f}%")
        
    print("切片完成！", time.strftime("%Y-%m-%d %H:%M:%S"))

    if convert_to_wgs84:
        gdal.Unlink(tif_in_geoCS)
    ds = None


###################################################################

if __name__ == "__main__":
    import argparse
    
    # 创建命令行参数解析器
    parser = argparse.ArgumentParser(description='Slice large TIF image into grid tiles.')
    parser.add_argument('input_image', help='Input TIF image path')
    parser.add_argument('output_dir', help='Output directory for tiles')
    parser.add_argument('grid_resolution', type=float, help='Grid resolution in kilometers')
    parser.add_argument('--clear-nodata', action='store_true', default=True, 
                        help='Clear tiles containing only nodata values (default: True)')
    parser.add_argument('--convert-wgs84', action='store_true', default=False,
                        help='Convert input to WGS84 projection before processing (default: False)')

    # 解析命令行参数
    args = parser.parse_args()

    start_time = time.time()

    # 执行处理
    process(args.input_image, args.output_dir, args.grid_resolution, 
           clearNodata=args.clear_nodata, convert_to_wgs84=args.convert_wgs84)

    end_time = time.time()
    print('Time cost:', end_time - start_time, 's')

# python bigTifSlicer.py input_image.tif output_directory 5