from ogms_xfer import OGMS_Xfer as xfer
import json
from osgeo import gdal
from datetime import datetime
import numpy as np

def merge_tiles_to_memory(tif_paths):
    # --------- Merge tif to memory using VRT and Translate -----------------
    
    # 创建VRT
    vrt = gdal.BuildVRT("", tif_paths)
    
    # 使用内存驱动
    mem_driver = gdal.GetDriverByName('MEM')
    
    # 获取VRT的元数据
    vrt_ds = gdal.Open(vrt)
    cols = vrt_ds.RasterXSize
    rows = vrt_ds.RasterYSize
    bands = vrt_ds.RasterCount
    geo_transform = vrt_ds.GetGeoTransform()
    projection = vrt_ds.GetProjection()
    
    # 创建内存数据集
    mem_ds = mem_driver.Create('', cols, rows, bands, gdal.GDT_Float32)
    mem_ds.SetGeoTransform(geo_transform)
    mem_ds.SetProjection(projection)
    
    # 将VRT数据复制到内存数据集
    gdal.Translate(mem_ds, vrt)
    
    return mem_ds


def calculate_ndvi(nir_path, red_path, output_path):
    nir_ds = gdal.Open(nir_path)
    red_ds = gdal.Open(red_path)

    if not nir_ds or not red_ds:
        raise ValueError("无法打开影像数据，请检查 URL 是否有效")

    nir_band = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)

    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-10)

    geo_transform = nir_ds.GetGeoTransform()
    projection = nir_ds.GetProjection()
    cols, rows = nir_band.shape

    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(output_path, rows, cols, 1, gdal.GDT_Float32)

    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)

    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(-9999)

    nir_ds, red_ds, ndvi_ds = None, None, None
    return output_path


def calculate_scene_ndvi(scene, region, ndvi_output_path):
    print('------------------------------------------')
    print(f"处理影像{scene.scene_id}...")
    nir_image = scene.get_band_image("5")
    red_image = scene.get_band_image("4")
    nir_region_tiles = nir_image.get_tiles_by_polygon(region)
    red_region_tiles = red_image.get_tiles_by_polygon(region)
    
    nir_tif_paths = [xfer.URL.resolve(tile.url) for tile in nir_region_tiles]
    red_tif_paths = [xfer.URL.resolve(tile.url) for tile in red_region_tiles]
    
    # 使用内存中的瓦片合并结果
    nir_mem_ds = merge_tiles_to_memory(nir_tif_paths)
    red_mem_ds = merge_tiles_to_memory(red_tif_paths)
    print(f"已完成区域红外和红光瓦片合并")
    
    
    # 从内存数据集计算NDVI
    nir_band = nir_mem_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_mem_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    
    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-10)
    
    geo_transform = nir_mem_ds.GetGeoTransform()
    projection = nir_mem_ds.GetProjection()
    cols, rows = nir_band.shape
    
    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(ndvi_output_path, rows, cols, 1, gdal.GDT_Float32)
    
    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)
    
    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(-9999)
    print(f"NDVI计算完毕....")
    
    # 清理内存
    nir_mem_ds, red_mem_ds, ndvi_ds = None, None, None
    return ndvi_output_path


def calculate_grid_ndvi(scene, grid_cells, ndvi_output_path):
    print('------------------------------------------')
    print(f"处理影像{scene.scene_id}...")
    nir_image = scene.get_band_image("5")
    red_image = scene.get_band_image("4")
    # nir_region_tiles = nir_image.get_tiles_by_polygon(region)
    # red_region_tiles = red_image.get_tiles_by_polygon(region)
    
    nir_region_tiles = nir_image.get_tiles_by_grid_cells(grid_cells)
    red_region_tiles = red_image.get_tiles_by_grid_cells(grid_cells)
    
    nir_tif_paths = [xfer.URL.resolve(tile.url) for tile in nir_region_tiles]
    red_tif_paths = [xfer.URL.resolve(tile.url) for tile in red_region_tiles]
    
    # 使用内存中的瓦片合并结果
    nir_mem_ds = merge_tiles_to_memory(nir_tif_paths)
    red_mem_ds = merge_tiles_to_memory(red_tif_paths)
    print(f"已完成区域红外和红光瓦片合并")
    
    
    # 从内存数据集计算NDVI
    nir_band = nir_mem_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_band = red_mem_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    
    ndvi = (nir_band - red_band) / (nir_band + red_band + 1e-10)
    
    geo_transform = nir_mem_ds.GetGeoTransform()
    projection = nir_mem_ds.GetProjection()
    cols, rows = nir_band.shape
    
    driver = gdal.GetDriverByName("GTiff")
    ndvi_ds = driver.Create(ndvi_output_path, rows, cols, 1, gdal.GDT_Float32)
    
    ndvi_ds.SetGeoTransform(geo_transform)
    ndvi_ds.SetProjection(projection)
    
    ndvi_ds.GetRasterBand(1).WriteArray(ndvi)
    ndvi_ds.GetRasterBand(1).SetNoDataValue(-9999)
    print(f"NDVI计算完毕....")
    
    # 清理内存
    nir_mem_ds, red_mem_ds, ndvi_ds = None, None, None
    return ndvi_output_path
    
    
    


if __name__ == "__main__":
    
    config_file_path = "config.json"
    xfer.initialize(config_file_path)
    
    ##### 区域影像获取 （1st）
    # 感兴趣区
    with open(xfer.URL.dataUrl('small.geojson'), "r") as f:
        region = json.load(f)
    
    # 目标产品
    product = xfer.Product().query(product_name="landset8_L2SP")[0]
    
    # 按条件筛选出系列影像
    scenes = xfer.Scene().query(
        product_id=product.product_id,
        polygon=region,
        time_range=(datetime(2021, 1, 1), datetime(2025, 1, 31)),
        cloud_range=(0, 10)
    )
    
    # 载入感兴趣区
    with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
        region = json.load(f)

    # 计算格网
    grid_helper = xfer.GridHelper(1) # 1km 格网
    grid_cells = grid_helper.get_grid_cells(region)

    # 计算NDVI
    start_time = datetime.now()
    for i, scene in enumerate(scenes):
        calculate_scene_ndvi(scene, region, xfer.URL.outputUrl(f'ndvi_{i}.tif'))
    
    end_time = datetime.now()
    print(f"区域时序NDVI计算完成，总计用时{end_time - start_time}")
    print("--------------------------------")