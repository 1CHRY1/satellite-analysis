import os
import math
import numpy as np
from osgeo import gdal

# Constants
EarthRadius = 6371008.8
EarthCircumference = 2 * math.pi * EarthRadius

def lnglat2grid(pos, world_grid_num):
    grid_x = math.floor((pos[0] + 180.0) / 360.0 * world_grid_num[0])
    grid_y = math.floor((90.0 - pos[1]) / 180.0 * world_grid_num[1])
    return grid_x, grid_y

def grid2lnglat(grid_x, grid_y, world_grid_num):
    lng = grid_x / world_grid_num[0] * 360.0 - 180.0
    lat = 90.0 - grid_y / world_grid_num[1] * 180.0
    return lng, lat

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
    right_bottom = [info[0] + x_size * info[1], info[3] + y_size * info[5]]
    resolution_per_pixel = [info[1], -info[5]]
    rotaion = [info[2], info[4]]
    return left_top, right_bottom, resolution_per_pixel, rotaion, origin_size, info

def geo_to_pixel(geo_x, geo_y, geo_transform):
    det = geo_transform[1] * geo_transform[5] - geo_transform[2] * geo_transform[4]
    if det == 0:
        raise ValueError("GeoTransform matrix is singular")
    inv_transform = [
        geo_transform[5] / det, -geo_transform[2] / det,
        -geo_transform[4] / det, geo_transform[1] / det
    ]
    pixel_x = inv_transform[0] * (geo_x - geo_transform[0]) + inv_transform[1] * (geo_y - geo_transform[3])
    pixel_y = inv_transform[2] * (geo_x - geo_transform[0]) + inv_transform[3] * (geo_y - geo_transform[3])
    return int(math.floor(pixel_x)), int(math.floor(pixel_y))

def tif2GeoCS(input_image, out_image):
    options = gdal.WarpOptions(dstSRS="EPSG:4326")  # 目标坐标系为 WGS 84
    gdal.Warp(out_image, input_image, options=options)


import tempfile

# Updated WGS84 conversion to write to real temp file instead of /vsimem
def tif2GeoCS_to_disk(input_image):
    tmp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".tif")
    tmp_file.close()  # Close it so GDAL can write to it
    out_image = tmp_file.name
    options = gdal.WarpOptions(dstSRS="EPSG:4326")
    gdal.Warp(out_image, input_image, options=options)
    return out_image

from multiprocessing import Pool, cpu_count
import shutil

# Process pool version of tile processor
def process_tile_mp(args):
    ds_path, geoTransform, origin_size, world_grid_num, grid_id_x, grid_id_y, grid_p_size_x, grid_p_size_y, output_dir, band_count, clearNodata = args
    ds = gdal.Open(ds_path, gdal.GA_ReadOnly)
    if ds is None:
        return

    try:
        grid_lng, grid_lat = grid2lnglat(grid_id_x, grid_id_y, world_grid_num)
        x, y = geo_to_pixel(grid_lng, grid_lat, geoTransform)
        w, h = grid_p_size_x, grid_p_size_y

        if clearNodata:
            if x < 0 or y < 0 or (x + w) > origin_size[0] or (y + h) > origin_size[1]:
                return

        for band_index in range(1, band_count + 1):
            band_output_dir = os.path.join(output_dir, f"band_{band_index}")
            tile_filename = os.path.join(band_output_dir, f"tile_{grid_id_x}_{grid_id_y}.tif")
            band = ds.GetRasterBand(band_index)
            tile_data = band.ReadAsArray(x, y, w, h)
            no_data_value = band.GetNoDataValue()
            if no_data_value is None:
                no_data_value = 0
            if np.all(tile_data == no_data_value):
                continue
            gdal.Translate(tile_filename, ds, srcWin=[x, y, w, h], bandList=[band_index])
    finally:
        ds = None


import tempfile

# Replacing /vsimem/ usage with real disk temp file
def process_parallel_mp_fixed(input_image, output_dir, grid_resolution, clearNodata=True, convert_to_wgs84=False):
    gdal.SetCacheMax(2**27)

    in_image = input_image
    temp_file_created = None

    if convert_to_wgs84:
        in_image = tif2GeoCS_to_disk(input_image)
        temp_file_created = in_image

    ds = gdal.Open(in_image)
    if ds is None:
        print(f"Unable to open input image: {in_image}")
        return

    band_count = ds.RasterCount
    grid_resolution_in_meter = grid_resolution * 1000
    world_grid_num_x = math.ceil(EarthCircumference / grid_resolution_in_meter)
    world_grid_num_y = math.ceil(EarthCircumference / 2.0 / grid_resolution_in_meter)
    world_grid_num = [world_grid_num_x, world_grid_num_y]

    lt, rb, degree_per_pixel, _, origin_size, geoTransform = rasterInfo(ds)
    grid_d_size_x, grid_d_size_y = grid_size_in_degree(world_grid_num)
    grid_p_size_x = math.ceil(grid_d_size_x / degree_per_pixel[0])
    grid_p_size_y = math.ceil(grid_d_size_y / degree_per_pixel[1])
    grid_lt_x, grid_lt_y = lnglat2grid(lt, world_grid_num)
    grid_rb_x, grid_rb_y = lnglat2grid(rb, world_grid_num)

    for band_index in range(1, band_count + 1):
        band_output_dir = os.path.join(output_dir, f"band_{band_index}")
        os.makedirs(band_output_dir, exist_ok=True)

    tasks = []
    for i in range(grid_rb_x - grid_lt_x + 1):
        for j in range(grid_rb_y - grid_lt_y + 1):
            grid_id_x = grid_lt_x + i
            grid_id_y = grid_lt_y + j
            tasks.append((
                in_image, geoTransform, origin_size, world_grid_num,
                grid_id_x, grid_id_y, grid_p_size_x, grid_p_size_y,
                output_dir, band_count, clearNodata
            ))

    with Pool(processes=cpu_count()) as pool:
        pool.map(process_tile_mp, tasks)

    ds = None
    if temp_file_created:
        os.remove(temp_file_created)

    print("多进程切片完成（WGS84修复版）！")
