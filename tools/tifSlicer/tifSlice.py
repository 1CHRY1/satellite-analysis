import os, math, time, json, sys
from osgeo import gdal
import numpy as np

# 获取程序所在路径（对于打包的 .exe 文件）
if getattr(sys, 'frozen', False):
    # Nuitka 打包后的临时路径
    base_path = os.path.dirname(sys.argv[0])
else:
    # 原始 Python 脚本路径
    base_path = os.path.dirname(__file__)

# proj.db存储在base_path下
os.environ['PROJ_LIB'] = base_path

print("os.environ['PROJ_LIB'] -- ", os.environ['PROJ_LIB'])

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


def tif2GeoCS(input_image, out_image):

    # if(not os.path.exists(os.path.dirname(out_image))):
    #     os.makedirs(os.path.dirname(out_image))

    # if(os.path.exists(out_image)):
    #     os.remove(out_image)

    # gdal.Warp(out_image, input_image, dstSRS="EPSG:4326")

    options = gdal.WarpOptions(dstSRS="EPSG:4326")  # 目标坐标系为 WGS 84
    gdal.Warp(out_image, input_image, options=options)


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


def grids2Geojson(gridIDlist, world_grid_num):
    result = {"type": "FeatureCollection", "features": []}

    for grid_x, grid_y, i, j in gridIDlist:
        left_lng, top_lat = grid2lnglat(grid_x, grid_y, world_grid_num)
        right_lng, bottom_lat = grid2lnglat(grid_x + 1, grid_y + 1, world_grid_num)
        grid_id = f"{grid_x}_{grid_y}"

        grid_polygon_feature = {
            "type": "Feature",
            "properties": {
                "id": grid_id,
                "id_in_image": {
                    "column": i,
                    "row": j
                },
                "id_in_globe": {
                    "column": grid_x,
                    "row": grid_y
                }
            },
            "geometry": {
                "type": "Polygon",
                "coordinates": [[
                    [left_lng, top_lat],
                    [right_lng, top_lat],
                    [right_lng, bottom_lat],
                    [left_lng, bottom_lat],
                    [left_lng, top_lat]
                ]]
            }
        }
        result["features"].append(grid_polygon_feature)

    return result


def process(input_image, output_dir, grid_resolution, clearNodata=True):

    tif_in_geoCS = "/vsimem/temp.tif" # GDAL 内存文件系统
    grid_resolution_in_meter = grid_resolution * 1000
    world_grid_num_x = math.ceil(EarthCircumference / grid_resolution_in_meter)
    world_grid_num_y = math.ceil(EarthCircumference / 2.0 / grid_resolution_in_meter)

    world_grid_num = [world_grid_num_x, world_grid_num_y]

    #--------- Project tif to geoCS ------------------------------------
    tif2GeoCS(input_image, tif_in_geoCS)

    ds = gdal.Open(tif_in_geoCS)
    if ds is None:
        print(f"Unable to open input image: {input_image}")
        return

    #--------- Calc pixelperDegree -------------------------------------
    lt, rb, degree_per_pixel, rotate ,origin_size, geoTransform = rasterInfo(ds)

    #--------- Calc grid_lt, grid_rb -----------------------------------
    grid_lt_x , grid_lt_y = lnglat2grid(lt, world_grid_num)
    grid_rb_x , grid_rb_y = lnglat2grid(rb, world_grid_num)

    grid_lt_lng, grid_lt_lat = grid2lnglat(grid_lt_x, grid_lt_y, world_grid_num)

    #--------- Calc grid size in degree space -------------------------
    grid_d_size_x, grid_d_size_y = grid_size_in_degree(world_grid_num)

    #--------- Calc grid size in image pixel space ---------------------
    grid_p_size_x = math.ceil(grid_d_size_x / degree_per_pixel[0])
    grid_p_size_y = math.ceil(grid_d_size_y / degree_per_pixel[1])

    #--------- Create a virtual raster pixel bound ---------------------
    grid_num_x = grid_rb_x - grid_lt_x + 1
    grid_num_y = grid_rb_y - grid_lt_y + 1

    pixel_offset_x = (lt[0] - grid_lt_lng) * (1 / degree_per_pixel[0])
    pixel_offset_y = (grid_lt_lat - lt[1]) * (1 / degree_per_pixel[1])

    pixel_offset_x = round(pixel_offset_x)
    pixel_offset_y = round(pixel_offset_y)

    #--------- Create Slice base on virtual raster pixel bound ---------

    no_data_value = ds.GetRasterBand(1).GetNoDataValue()
    if no_data_value is None:
        no_data_value = 0

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

            if(clearNodata):
                #--------- Skip out of padding grid ------------------------
                if(x < 0 or y < 0 or (x + w) > origin_size[0] or (y + h) > origin_size[1]):
                    continue

                #--------- Clean no data slice -----------------------------
                tile_data = ds.ReadAsArray(x, y, w, h)
                if np.all(tile_data == no_data_value):
                    continue

            grid_id_list.append([grid_id_x, grid_id_y])
            tile_filename = os.path.join(output_dir, f"tile_{i}_{j}.tif")
            gdal.Translate(tile_filename, ds, srcWin=[x, y, w, h])

    gdal.Unlink(tif_in_geoCS)


    #--------- Create grids geojson ------------------------------------
    geojson = grids2Geojson(grid_id_list, world_grid_num)
    with open(os.path.join(output_dir, "grid.geojson"), "w") as f:
        json.dump(geojson, f)


def debug(input_image, output_dir):

    ds = gdal.Open(input_image)
    if ds is None:
        print(f"Unable to open input image: {input_image}")
        return

    width = ds.RasterXSize
    height = ds.RasterYSize


    # x = i * tile_size
    # y = j * tile_size
    # w = min(tile_size, width - x)
    # h = min(tile_size, height - y)

    tile_filename = os.path.join(output_dir, f"debugging.tif")
    gdal.Translate(tile_filename, ds, srcWin=[-500, 100, 5000, 2000])


###################################################################
if __name__ == "__main__":

    #---------- External input parameters-------------
    input_image = "D:\\edgedownload\\LT51190382000261BJC00\\LT51190382000261BJC00_B1.TIF"
    output_dir = "C:\\Users\\19236\\Desktop\\test\\2"
    grid_resolution_in_kilometer = 5

    start_time = time.time()

    #---------- Core ---------------------------------
    process(input_image, output_dir, grid_resolution_in_kilometer, clearNodata = True)

    end_time = time.time()
    print('Time cost:', end_time - start_time, 's')
