from ogms_xfer import OGMS_Xfer as xfer
import json
from osgeo import gdal
from datetime import datetime

def mtif(tif_paths, output_path):
    # --------- Merge tif using Translate --------------------------------------

    gdal.Translate(output_path, gdal.BuildVRT("", tif_paths),
                   options=gdal.TranslateOptions(
                       creationOptions=["COMPRESS=LZW"],
                       format="GTiff"
                   ))

    return output_path

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