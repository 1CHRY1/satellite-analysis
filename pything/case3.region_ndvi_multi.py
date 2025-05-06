from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime
import json
import os
import multiprocessing as mp
from functools import partial

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

# 创建输出目录
output_dir = os.path.dirname(xfer.URL.outputUrl('ndvi_tiles'))
os.makedirs(output_dir, exist_ok=True)

# 定义单个瓦片NDVI计算函数
def calculate_tile_ndvi(nir_path, red_path, tile_index):
    ndvi_tile_path = xfer.URL.outputUrl(f'ndvi_tile_{tile_index}.tif')
    xfer.ModelStore.ndvi(nir_path, red_path, ndvi_tile_path)
    return ndvi_tile_path

# 使用多进程计算每个瓦片的NDVI
def process_tiles_parallel():
    start_time = datetime.now()
    
    # 创建进程池
    # num_processes = mp.cpu_count()
    num_processes = 6
    print(f"使用{num_processes}个进程进行并行计算...")
    
    # 准备参数
    tile_indices = list(range(len(nir_tif_paths)))
    ndvi_tile_paths = []
    
    # 使用进程池并行计算
    with mp.Pool(processes=num_processes) as pool:
        # 使用partial固定其他参数
        calc_func = partial(calculate_tile_ndvi, tile_index=None)
        
        # 创建任务列表
        tasks = [(nir_tif_paths[i], red_tif_paths[i], i) for i in tile_indices]
        
        # 执行并行计算
        ndvi_tile_paths = pool.starmap(calculate_tile_ndvi, tasks)
    
    end_time = datetime.now()
    print(f"所有瓦片NDVI计算完成，用时{end_time - start_time}")
    print("--------------------------------")
    
    return ndvi_tile_paths

# 执行并行计算
ndvi_tile_paths = process_tiles_parallel()

# 合并所有NDVI瓦片
start_time = datetime.now()
ndvi_output_path = xfer.URL.outputUrl('ndvi_region.tif')
xfer.Toolbox.merge_tiles(ndvi_tile_paths, ndvi_output_path)

end_time = datetime.now()
print(f"NDVI瓦片合并完成，用时{end_time - start_time}")
print("--------------------------------")

