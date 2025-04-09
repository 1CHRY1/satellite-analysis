from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime
import json

config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################
# 感兴趣区
with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
    region = json.load(f)

grid_cells = xfer.Toolbox.polygon_to_grid_cells(region)
print(f"该区域包含{len(grid_cells)}个格网...")

# 目标产品和影像查询
product = xfer.Product().query(product_name="landset8_L2SP")[0]
scenes = xfer.Scene().query(
    product_id=product.product_id,
    polygon=region,
    time_range=(datetime(2021, 1, 1), datetime(2025, 1, 31)),
    cloud_range=(0, 10)
)

start_time = datetime.now()

# 无云瓦片检索
target_tiles_map = {}
max_cloud = 0
for scene in scenes:
    scene_tiles = scene.get_tiles(cloud_range=(0,10), band=1, grid_cells=grid_cells)
    for tile in scene_tiles:
        if tile.tile_id not in target_tiles_map:
            target_tiles_map[tile.tile_id] = tile
            if tile.cloud > max_cloud: max_cloud = tile.cloud
        elif target_tiles_map[tile.tile_id].cloud > tile.cloud:
            target_tiles_map[tile.tile_id] = tile

target_tiles = list(target_tiles_map.values())
print(f"已检索到{len(target_tiles)}个瓦片, 用时{datetime.now() - start_time}")
print(f"其中，瓦片最大云量为{max_cloud}")

tif_paths = [xfer.URL.resolve(tile.url) for tile in target_tiles]
merged_tif_path = xfer.URL.outputUrl('no_cloud_region_merged.tif')

# 合并瓦片
print("开始合并瓦片...")
start_time = datetime.now()
xfer.Toolbox.merge_tiles(tif_paths, merged_tif_path)
print(f"合并完成，用时{datetime.now() - start_time}")
print("--------------------------------")