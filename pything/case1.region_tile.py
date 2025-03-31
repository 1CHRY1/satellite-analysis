from ogms_xfer import OGMS_Xfer as xfer
import json
from osgeo import gdal
from datetime import datetime

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

if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    xfer.initialize(config_file_path)
    
    scene = xfer.Scene("SC906772444")
    image = scene.get_band_image("1")
    
    with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
        region = json.load(f)
        
    region_tiles = image.get_tiles_by_polygon(region)
    
    tif_paths = [xfer.URL.resolve(tile.url) for tile in region_tiles]
    print(f"已检索到{len(region_tiles)}个瓦片")
    
    start_time = datetime.now()
    merged_tif_path = xfer.URL.outputUrl('region_merged_tile.tif')
    
    mtif(tif_paths, merged_tif_path)
    end_time = datetime.now()
    print(f"区域影像检索合并完成，用时{end_time - start_time}")
    print("--------------------------------")