from ogms_xfer import OGMS_Xfer as xfer
import json
from osgeo import gdal
from datetime import datetime
import concurrent.futures
from collections import defaultdict

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

def process_scene(scene, region):
    """处理单个场景并返回瓦片"""
    scene_tiles = scene.get_tiles(polygon=region, cloud_range=(0, 10), band=1)
    return scene_tiles

if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    xfer.initialize(config_file_path)
    
    ###### 1. 检索一系列影像
    
    # 感兴趣区
    with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
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

    # 并行处理所有场景以获取瓦片
    all_tiles = []
    with concurrent.futures.ThreadPoolExecutor() as executor:
        future_to_scene = {executor.submit(process_scene, scene, region): scene for scene in scenes}
        for future in concurrent.futures.as_completed(future_to_scene):
            scene_tiles = future.result()
            all_tiles.extend(scene_tiles)
    
    # 使用字典推导式找出每个瓦片ID对应的云量最小的瓦片
    tile_dict = {}
    for tile in all_tiles:
        if tile.tile_id not in tile_dict or tile.cloud < tile_dict[tile.tile_id].cloud:
            tile_dict[tile.tile_id] = tile
    
    target_tiles = list(tile_dict.values())
    max_cloud = max((tile.cloud for tile in target_tiles), default=0) if target_tiles else 0
    
    print(f"已检索到{len(target_tiles)}个瓦片, 用时{datetime.now() - start_time}")
    print(f"其中，瓦片最大云量为{max_cloud}")
    
    start_time = datetime.now()
    tif_paths = [xfer.URL.resolve(tile.url) for tile in target_tiles]
    merged_tif_path = xfer.URL.outputUrl('no_cloud_region_merged.tif')
    
    mtif(tif_paths, merged_tif_path)
    print(f"合并完成，用时{datetime.now() - start_time}")
    print("--------------------------------")