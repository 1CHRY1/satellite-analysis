from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime
import json

config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################
# 目标产品和影像查询
scene = xfer.Scene("SC906772444")
nir_image = scene.get_band_image("5")
red_image = scene.get_band_image("4")

# 感兴趣区
with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
    region = json.load(f)

grid_cells = xfer.Toolbox.polygon_to_grid_cells(region)
print(f"该区域包含{len(grid_cells)}个格网...")
nir_region_tiles = nir_image.get_tiles_by_grid_cells(grid_cells)
red_region_tiles = red_image.get_tiles_by_grid_cells(grid_cells)

nir_tif_paths = [xfer.URL.resolve(tile.url) for tile in nir_region_tiles]
red_tif_paths = [xfer.URL.resolve(tile.url) for tile in red_region_tiles]
print(f"已检索到{len(nir_region_tiles)}个瓦片")

start_time = datetime.now()
nir_region_tif_path = xfer.URL.outputUrl('nir_region.tif')
red_region_tif_path = xfer.URL.outputUrl('red_region.tif')

xfer.Toolbox.merge_tiles(nir_tif_paths, nir_region_tif_path)
xfer.Toolbox.merge_tiles(red_tif_paths, red_region_tif_path)

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