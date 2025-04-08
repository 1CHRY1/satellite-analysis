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
    
    ###### 1. 检索一系列影像
    
    # 感兴趣区
    with open(xfer.URL.dataUrl('small.geojson'), "r") as f:
        region = json.load(f)
    
    # 目标产品
    product = xfer.Product().query(product_name="landset8_L2SP")[0]
    
    # 系列影像
    scenes = xfer.Scene().query(
        product_id=product.product_id,
        polygon=region,
        time_range=(datetime(2021, 1, 1), datetime(2025, 1, 31)),
        cloud_range=(0, 10)
    )
    
    start_time = datetime.now()

    ###### 2. 检索系列影像中云量最小的瓦片，当前只检索了band1
    target_tiles_map = {}
    max_cloud = 0
    for scene in scenes:
        scene_tiles = scene.get_tiles(polygon=region,cloud_range=(0,10), band=1)
        for tile in scene_tiles:
            if tile.tile_id not in target_tiles_map:
                target_tiles_map[tile.tile_id] = tile
                if tile.cloud > max_cloud: max_cloud = tile.cloud
            elif target_tiles_map[tile.tile_id].cloud > tile.cloud:
                target_tiles_map[tile.tile_id] = tile
    
    target_tiles = list(target_tiles_map.values())
    print(f"已检索到{len(target_tiles)}个瓦片, 用时{datetime.now() - start_time}")
    print(f"其中，瓦片最大云量为{max_cloud}")
    
    ###### 3. 合并瓦片 time-costly
    start_time = datetime.now()
    tif_paths = [xfer.URL.resolve(tile.url) for tile in target_tiles]
    merged_tif_path = xfer.URL.outputUrl('no_cloud_region_merged.tif')
    
    mtif(tif_paths, merged_tif_path)
    print(f"合并完成，用时{datetime.now() - start_time}")
    print("--------------------------------")