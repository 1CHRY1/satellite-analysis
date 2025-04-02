from ogms_xfer import OGMS_Xfer as xfer
import json
from osgeo import gdal
from datetime import datetime
import numpy as np

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


if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    xfer.initialize(config_file_path)
    
    ##### 区域影像获取 （1st）
    scene = xfer.Scene("SC906772444")
    nir_image = scene.get_band_image("5")
    red_image = scene.get_band_image("4")
    
    with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
        region = json.load(f)
        
    nir_region_tiles = nir_image.get_tiles_by_polygon(region)
    red_region_tiles = red_image.get_tiles_by_polygon(region)
    
    nir_tif_paths = [xfer.URL.resolve(tile.url) for tile in nir_region_tiles]
    red_tif_paths = [xfer.URL.resolve(tile.url) for tile in red_region_tiles]
    print(f"已检索到{len(nir_region_tiles)}个瓦片")
    
    start_time = datetime.now()
    nir_region_tif_path = xfer.URL.outputUrl('nir_region.tif')
    red_region_tif_path = xfer.URL.outputUrl('red_region.tif')
    
    mtif(nir_tif_paths, nir_region_tif_path)
    mtif(red_tif_paths, red_region_tif_path)
    
    end_time = datetime.now()
    print(f"区域影像检索合并完成，用时{end_time - start_time}")
    print("--------------------------------")
    
    ##### NDVI计算（2nd）
    start_time = datetime.now()
    ndvi_output_path = xfer.URL.outputUrl('ndvi_region.tif')
    calculate_ndvi(nir_region_tif_path, red_region_tif_path, ndvi_output_path)
    
    end_time = datetime.now()
    print(f"NDVI计算完成，用时{end_time - start_time}")
    print("--------------------------------")