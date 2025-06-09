import json, glob
from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime

config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################

# 感兴趣区
with open(xfer.URL.dataUrl('region2.geojson'), "r") as f:
    region = json.load(f)
    feature = region['features'][0]

# 目标产品和影像查询
scene = xfer.Scene().query(scene_name='LC08_L2SP_120035_20250217_20250226_02_T1')[0]
image = scene.get_band_image("1")
image_url = xfer.URLUtil.resolve(image.url)
print("已获取到影像资源路径...")

# 区域影像快速几何截取
print("开始区域影像几何裁剪...")
xfer.TileUtil.read_image_feature_save(image_url, feature, xfer.URL.outputUrl('region_image.tif'))
print("区域影像几何裁剪完成...")

## 区域格网合并
grid_resolution = 20 # km
grid_helper = xfer.Toolbox.GridHelper(grid_resolution)
grid_cells = grid_helper.get_grid_cells(region)
print(f"该区域包含{len(grid_cells)}个格网...")

for grid_cell in grid_cells:
    bbox = grid_helper.get_grid_bbox(grid_cell)
    xfer.TileUtil.read_image_box_save(image_url, bbox, xfer.URL.outputUrl(f'grid_{grid_cell.columnId}_{grid_cell.rowId}.tif'))
print("区域影像格网已裁剪并保存...")

tif_paths = glob.glob(xfer.URL.outputUrl('grid_*.tif'))
merged_tif_path = xfer.URL.outputUrl('region_merged_tile.tif')
xfer.Toolbox.merge_tiles(tif_paths, merged_tif_path)
print("区域影像格网合并完成...")
