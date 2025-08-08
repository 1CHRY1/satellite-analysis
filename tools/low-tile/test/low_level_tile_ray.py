import ray
import time
import random
from scene_fetcher import SceneFetcher
from grid_mosaic import GridMosaic
import rasterio
import os
from config import email, password, grid_res, crs, z_level, start_time, end_time, region_id, sensor_name, test_num
# 初始化 Ray
cpu_count = os.cpu_count()
recommended_cpus = max(cpu_count - 8, int(cpu_count * 0.75))
ray.init(num_cpus=recommended_cpus)

@ray.remote
def process_grid(grid, sensor_name, fetcher, crs, z_level, quiet=True):
    try:
        start_time = time.time()
        scenes = fetcher.get_scenes_for_grid(sensor_name, grid['coordinates'][0])
        if len(scenes) > 0:
            if not quiet:
                print(f"{grid['rowId']}-{grid['columnId']} time in get_scenes_for_grid: {time.time() - start_time}\n", flush=True)
            start_time = time.time()
            # print(f"total scenes: {len(scenes)} in grid {grid['rowId']}-{grid['columnId']}", flush=True)
            grid_mosaic = GridMosaic(grid['coordinates'][0], scenes, crs_id=crs, z_level=z_level)
            prefix_minio = "."
            put_path = grid_mosaic.create_mosaic(prefix_minio, quiet=quiet)
            if not quiet:
                print(f"{grid['rowId']}-{grid['columnId']} time in create_mosaic: {time.time() - start_time}\n", flush=True)
            # print(f"put_path: {put_path}", flush=True)
            return ("success", put_path)
        else:
            return ("no_data", None)
    except Exception as e:
        print(f"❌ Error in grid {grid['rowId']}-{grid['columnId']}: {e}", flush=True)
        import traceback
        traceback.print_exc()
        return ("error", None)



def main():
    # 实例化 SceneFetcher 类
    fetcher = SceneFetcher(email=email, password=password)
    fetcher.login()
    
    # 获取格网数据
    grids_data = fetcher.get_grids(region_id=region_id, resolution=grid_res)
    print(f"grids count:{len(grids_data)}", flush=True)

    # 提交一次数据检索
    retrieved_data = fetcher.submit_query(
        start_time=start_time, end_time=end_time, region_id=region_id, resolution=grid_res
    )
    
    # 获取影像路径
    
    
    start = time.time()
    # 并行处理每个格网
    futures = [process_grid.remote(grid,sensor_name,fetcher,crs, z_level) for grid in grids_data]
    results = ray.get(futures)
    has_data = sum(1 for res in results if res[0] == "success")
    no_data = sum(1 for res in results if res[0] == "no_data")
    print(f"grids count with data: {has_data}, grids count without data: {no_data}\n", flush=True)
    print(f"time in proces_grid: {time.time() - start}\n", flush=True)
    # todo：生成一个mosaicjson
    start = time.time()
    random_grids = random.sample(grids_data, min(test_num, len(grids_data)))
    futures = [process_grid.remote(grid,sensor_name,fetcher,crs, z_level, quiet=False) for grid in random_grids]
    results = ray.get(futures)
    has_data = sum(1 for res in results if res[0] == "success")
    no_data = sum(1 for res in results if res[0] == "no_data")
    print(f"random test grids count with data: {has_data}, grids count without data: {no_data}\n", flush=True)
    print(f"time in random test proces_grid: {time.time() - start}\n", flush=True)
    
if __name__ == "__main__":
    main()
