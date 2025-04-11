import json
from datetime import datetime
from ogms_xfer import OGMS_Xfer as xfer

config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################
# 感兴趣区
with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
    region = json.load(f)

grid_cells = xfer.Toolbox.polygon_to_grid_cells(region)
print(f"该区域包含{len(grid_cells)}个格网...")

# 目标产品和影像查询
scene = xfer.Scene("SC906772444")
image = scene.get_band_image("1")
tiles = image.get_tiles_by_grid_cells(grid_cells)

tif_paths = [xfer.URL.resolve(tile.url) for tile in tiles]
merged_tif_path = xfer.URL.outputUrl('region_merged_tile.tif')
print("已获取到瓦片资源路径...")

# 合并瓦片
start_time = datetime.now()
print("开始合并瓦片...")
xfer.Toolbox.merge_tiles(tif_paths, merged_tif_path)
end_time = datetime.now()
print(f"区域影像检索合并完成，用时{end_time - start_time}")
print("--------------------------------------")