from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime
import json
import os
import multiprocessing as mp
from functools import partial


def dir_to_tif_list(dir_path):
    tif_list = []
    for file in os.listdir(dir_path):
        if file.endswith(".tif"):
            tif_list.append(os.path.join(dir_path, file))
    print(f"已获取到{len(tif_list)}个瓦片...")
    return tif_list

# 定义单个瓦片NDVI计算函数
def calculate_tile_ndvi(nir_path, red_path, tile_index, output_dir):
    ndvi_tile_path = os.path.join(output_dir, f'ndvi_tile_{tile_index}.tif')
    # 在这里直接使用GDAL或其他库计算NDVI，避免导入OGMS_Xfer
    from osgeo import gdal
    import numpy as np
    
    # 读取NIR和RED波段
    nir_ds = gdal.Open(nir_path)
    red_ds = gdal.Open(red_path)

    nir_data = nir_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    red_data = red_ds.GetRasterBand(1).ReadAsArray().astype(np.float32)
    
    # 计算NDVI
    # ndvi = np.where((nir_data + red_data) != 0, (nir_data - red_data) / (nir_data + red_data), 0)
    ndvi = (nir_data - red_data) / (nir_data + red_data + 1e-10)
    
    # 保存NDVI结果
    driver = gdal.GetDriverByName('GTiff')
    out_ds = driver.Create(ndvi_tile_path, nir_ds.RasterXSize, nir_ds.RasterYSize, 1, gdal.GDT_Float32)
    out_ds.SetGeoTransform(nir_ds.GetGeoTransform())
    out_ds.SetProjection(nir_ds.GetProjection())
    out_ds.GetRasterBand(1).WriteArray(ndvi)
    
    # 清理资源
    nir_ds = None
    red_ds = None
    out_ds = None
    
    return ndvi_tile_path

# 使用多进程计算每个瓦片的NDVI
def process_tiles_parallel(nir_tif_paths, red_tif_paths, output_dir):
    start_time = datetime.now()
    
    # 创建进程池
    num_processes = max(1, mp.cpu_count() - 1)
    # num_processes = 1
    print(f"使用{num_processes}个进程进行并行计算...")
    
    # 准备参数
    tile_indices = list(range(len(nir_tif_paths)))
    ndvi_tile_paths = []
    
    # 使用进程池并行计算
    with mp.Pool(processes=num_processes) as pool:
        # 创建任务列表
        tasks = [(nir_tif_paths[i], red_tif_paths[i], i, output_dir) for i in tile_indices]
        
        # 执行并行计算
        results = pool.starmap(calculate_tile_ndvi, tasks)
        
        # 过滤掉None结果
        ndvi_tile_paths = [path for path in results if path is not None]
    
    end_time = datetime.now()
    print(f"所有瓦片NDVI计算完成，用时{end_time - start_time}")
    print(f"成功处理了{len(ndvi_tile_paths)}/{len(tile_indices)}个瓦片")
    print("--------------------------------")
    
    return ndvi_tile_paths


##################################################
# 主程序
if __name__ == "__main__":
    # 获取输入文件路径
    nir_tif_paths = dir_to_tif_list("D:\edgedownload\multi_output\LT51190382000261BJC00_B4")
    red_tif_paths = dir_to_tif_list("D:\edgedownload\multi_output\LT51190382000261BJC00_B3")

    # 创建输出目录
    output_dir = os.path.dirname(xfer.URL.outputUrl('ndvi_tiles'))
    os.makedirs(output_dir, exist_ok=True)

    # 执行并行计算
    ndvi_tile_paths = process_tiles_parallel(nir_tif_paths, red_tif_paths, output_dir)

    # 检查是否有有效的NDVI瓦片
    if not ndvi_tile_paths:
        print("错误：没有生成有效的NDVI瓦片，无法进行合并")
        exit(1)

    # 合并所有NDVI瓦片
    start_time = datetime.now()
    ndvi_output_path = xfer.URL.outputUrl('ndvi_region.tif')
    xfer.Toolbox.merge_tiles(ndvi_tile_paths, ndvi_output_path)

    end_time = datetime.now()
    print(f"NDVI瓦片合并完成，用时{end_time - start_time}")
    print("--------------------------------")

