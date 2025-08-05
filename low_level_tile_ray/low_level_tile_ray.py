# low_level_tile_ray.py

import ray
import time
import json
import io
from minio import Minio
from scene_fetcher import SceneFetcher
from grid_mosaic import GridMosaic
from cogeo_mosaic.mosaic import MosaicJSON
import mercantile
from rasterio.crs import CRS


# 初始化 Ray
ray.init(num_cpus=8, ignore_reinit_error=True) 

@ray.remote
def process_grid(grid, sensor_name, fetcher, crs, z_level):
    try:
        scenes = fetcher.get_scenes_for_grid(sensor_name, grid['coordinates'][0])
        if len(scenes) > 0:
            print(f"处理中... Grid {grid['rowId']}-{grid['columnId']} 包含 {len(scenes)} 个场景")
            grid_mosaic = GridMosaic(grid['coordinates'][0], scenes, crs_id=crs, z_level=z_level)
            minio_path, bounds, crs_info = grid_mosaic.create_mosaic_with_metadata()
            
            # 返回文件路径和元数据
            return {
                'path': minio_path,
                'bounds': bounds,
                'crs': crs_info,
                'grid_coords': grid['coordinates'][0]
            }
        else:
            return None
    except Exception as e:
        print(f"❌ Error in grid {grid['rowId']}-{grid['columnId']}: {e}")
        import traceback
        traceback.print_exc()
        return None

def bounds_to_tiles(bounds, zoom_level):
    """
    将地理边界转换为瓦片坐标列表
    """
    west, south, east, north = bounds
    tiles = list(mercantile.tiles(west, south, east, north, zoom_level))
    return [(tile.x, tile.y, tile.z) for tile in tiles]

def tile_to_quadkey(x, y, z):
    """
    将瓦片坐标转换为quadkey格式
    """
    quadkey = ""
    for i in range(z, 0, -1):
        digit = 0
        mask = 1 << (i - 1)
        if (x & mask) != 0:
            digit += 1
        if (y & mask) != 0:
            digit += 2
        quadkey += str(digit)
    return quadkey

def create_mosaicjson_from_metadata(cog_metadata_list, bucket_name, quadkey_zoom=8):
    """
    根据COG元数据列表直接创建MosaicJSON，无需发起HTTP请求
    使用指定的quadkey_zoom级别生成标准的MosaicJSON格式
    """
    minio_base_url = "http://223.2.34.8:30900"
    
    # 初始化瓦片映射
    tiles_map = {}
    
    # 计算全局边界
    all_bounds = [item['bounds'] for item in cog_metadata_list if item['bounds']]
    if not all_bounds:
        raise ValueError("没有有效的边界信息")
    
    global_west = min(bounds[0] for bounds in all_bounds)
    global_south = min(bounds[1] for bounds in all_bounds)
    global_east = max(bounds[2] for bounds in all_bounds)
    global_north = max(bounds[3] for bounds in all_bounds)
    global_bounds = [global_west, global_south, global_east, global_north]
    
    print(f"全局边界: {global_bounds}")
    print(f"使用 quadkey_zoom 级别: {quadkey_zoom}")
    
    # 使用指定的quadkey_zoom级别创建瓦片映射
    for item in cog_metadata_list:
        if not item['bounds']:
            continue
            
        bounds = item['bounds']
        file_url = f"{minio_base_url}/{bucket_name}/{item['path']}"
        
        # 获取该边界在quadkey_zoom级别下覆盖的瓦片
        tile_coords = bounds_to_tiles(bounds, quadkey_zoom)
        
        for x, y, z in tile_coords:
            # 转换为quadkey格式
            quadkey = tile_to_quadkey(x, y, z)
            if quadkey not in tiles_map:
                tiles_map[quadkey] = []
            tiles_map[quadkey].append(file_url)
    
    # 创建标准的MosaicJSON结构
    mosaic_definition = {
        "mosaicjson": "0.0.3",
        "name": None,
        "description": None,
        "version": "1.0.0",
        "attribution": None,
        "minzoom": quadkey_zoom,
        "maxzoom": quadkey_zoom,
        "quadkey_zoom": quadkey_zoom,
        "bounds": global_bounds,
        "center": [
            (global_west + global_east) / 2,
            (global_south + global_north) / 2,
            quadkey_zoom
        ],
        "tiles": tiles_map,
        "tilematrixset": None,
        "asset_type": None,
        "asset_prefix": None,
        "data_type": None,
        "colormap": None,
        "layers": None
    }
    
    return mosaic_definition

def upload_mosaicjson(minio_client, bucket_name, mosaic_definition, output_object_name):
    """
    将MosaicJSON上传到MinIO
    """
    try:
        mosaic_json_string = json.dumps(mosaic_definition, indent=4)
        mosaic_bytes = mosaic_json_string.encode('utf-8')
        
        # 检查存储桶是否存在
        found = minio_client.bucket_exists(bucket_name)
        if not found:
            minio_client.make_bucket(bucket_name)
        
        # 上传到 MinIO
        minio_client.put_object(
            bucket_name=bucket_name,
            object_name=output_object_name,
            data=io.BytesIO(mosaic_bytes),
            length=len(mosaic_bytes),
            content_type='application/json'
        )
        print(f"✅ MosaicJSON 成功上传至: minio://{bucket_name}/{output_object_name}")
        return True
    except Exception as e:
        print(f"❌ 上传 MosaicJSON 失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    grid_res = 150
    crs = 4326
    z_level = 8

    fetcher = SceneFetcher(email="253301116@qq.com", password="123456")
    fetcher.login()
    
    grids_data = fetcher.get_grids(region_id="100000", resolution=grid_res)
    print(f"网格总数：{len(grids_data)}")

    fetcher.submit_query(
        start_time="2024-05-01", end_time="2025-06-30", region_id="100000", resolution=grid_res
    )
    
    sensor_name = "GF-1_PMS"
    
    start = time.time()
    futures = [process_grid.remote(grid, sensor_name, fetcher, crs, z_level) for grid in grids_data]
    results = ray.get(futures)
    print(f"\n所有格网处理完成，耗时: {time.time() - start:.2f} 秒")
    
    # 过滤出成功的结果
    successful_results = [result for result in results if result is not None]
    
    print(f"\n--- 成功处理的COG文件: {len(successful_results)} 个 ---")
    if successful_results:
        for result in successful_results:
            print(f"文件: {result['path']}, 边界: {result['bounds']}")
    else:
        print("没有成功生成的COG文件。")

    # 创建和上传MosaicJSON
    if successful_results:
        print("\n--- 正在生成 MosaicJSON (无需HTTP请求) ---")
        
        minio_client = Minio(
            "223.2.34.8:30900",
            access_key="minioadmin",
            secret_key="minioadmin",
            secure=False
        )
        
        bucket = "temp-files"
        minio_dir = "national-mosaicjson"
        mosaic_output_path = f"{minio_dir}/mosaic.json"
        
        # 使用元数据直接创建MosaicJSON
        start_mosaic = time.time()
        mosaic_definition = create_mosaicjson_from_metadata(
            successful_results, 
            bucket,
            quadkey_zoom=8  # 使用固定的quadkey_zoom级别
        )
        print(f"MosaicJSON创建耗时: {time.time() - start_mosaic:.2f} 秒")
        
        # 上传MosaicJSON
        upload_mosaicjson(minio_client, bucket, mosaic_definition, mosaic_output_path)
        
        print(f"\n🎉 总共处理了 {len(successful_results)} 个格网")
        print(f"📊 MosaicJSON 包含 {len(mosaic_definition['tiles'])} 个瓦片映射")
    else:
        print("\n由于没有成功的COG文件，跳过MosaicJSON的生成。")

if __name__ == "__main__":
    main()