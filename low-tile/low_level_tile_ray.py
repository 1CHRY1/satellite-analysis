import ray
import time
from scene_fetcher import SceneFetcher
from grid_mosaic import GridMosaic
import rasterio


# 初始化 Ray
ray.init(num_cpus=36)

@ray.remote
def process_grid(grid, sensor_name, fetcher, crs, z_level):
    try:
        scenes = fetcher.get_scenes_for_grid(sensor_name, grid['coordinates'][0])
        if len(scenes) > 0:
            # print(f"total scenes: {len(scenes)} in grid {grid['rowId']}-{grid['columnId']}", flush=True)
            grid_mosaic = GridMosaic(grid['coordinates'][0], scenes, crs_id=4326, z_level=10)
            prefix_minio = "."
            put_path = grid_mosaic.create_mosaic(prefix_minio)
            # print(f"put_path: {put_path}", flush=True)
            return put_path
    except Exception as e:
        print(f"❌ Error in grid {grid['rowId']}-{grid['columnId']}: {e}", flush=True)
        import traceback
        traceback.print_exc()
        return None



def main():
    grid_res = 150
    crs = 4326
    z_level = 8

    # 实例化 SceneFetcher 类
    fetcher = SceneFetcher(email="loop@ogms.com", password="ogms")
    fetcher.login()
    
    # 获取格网数据
    grids_data = fetcher.get_grids(region_id="100000", resolution=grid_res)
    print(f"grids count:{len(grids_data)}", flush=True)

    # 提交一次数据检索
    retrieved_data = fetcher.submit_query(
        start_time="2024-05-01", end_time="2025-07-30", region_id="100000", resolution=grid_res
    )
    
    # 获取影像路径
    sensor_name = "Month_map"
    
    start = time.time()
    # 并行处理每个格网
    futures = [process_grid.remote(grid,sensor_name,fetcher,crs, z_level) for grid in grids_data]
    results = ray.get(futures)
    print(f"time in proces_grid: {time.time() - start}", flush=True)
    # todo：生成一个mosaicjson

    
if __name__ == "__main__":
    main()
