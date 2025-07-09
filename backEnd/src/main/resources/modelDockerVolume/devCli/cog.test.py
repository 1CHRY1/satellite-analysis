from osgeo import gdal
import numpy as np
import time
import os

def print_info(title):
    print("\n" + "=" * 40)
    print(title)
    print("=" * 40)

def get_file_info(file_path):
    """获取是否为Tiled，是否有Overviews"""
    info = gdal.Info(file_path)
    is_tiled = "Block=" in info
    has_overviews = "Overviews" in info
    return is_tiled, has_overviews, info

def read_full_image(file_path):
    start = time.time()
    dataset = gdal.Open(file_path)
    if dataset is None:
        print("❌ Failed to open:", file_path)
        return None, None

    band = dataset.GetRasterBand(1)
    data = band.ReadAsArray()
    elapsed = time.time() - start
    print(f"✅ Full image read: {data.shape} | Time: {elapsed:.3f}s")
    return data, elapsed

def read_partial_image(file_path, xoff=0, yoff=0, xsize=512, ysize=512):
    start = time.time()
    dataset = gdal.Open(file_path)
    if dataset is None:
        print("❌ Failed to open:", file_path)
        return None, None

    band = dataset.GetRasterBand(1)
    data = band.ReadAsArray(xoff, yoff, xsize, ysize)
    elapsed = time.time() - start
    print(f"✅ Partial image read: ({xsize}, {ysize}) | Time: {elapsed:.3f}s")
    return data, elapsed


def compare_tifs(name, file_path):
    print_info(f"📂 Analyzing: {name}")
    
    is_tiled, has_overviews, gdalinfo = get_file_info(file_path)
    print(f"🧱 Tiled: {'Yes' if is_tiled else 'No'}")
    print(f"🔍 Overviews: {'Yes' if has_overviews else 'No'}")

    # Cog优势在于随机访问，如果读取整个波段，那仍然会比较慢
    # print("⏱ Reading full image...")
    # _, full_time = read_full_image(file_path)

    print("⏱ Reading partial image...")
    # _, partial_time = read_partial_image(file_path, 512, 512, 560, 770)
    _, partial_time = read_partial_image(file_path, 51, 912, 1985, 2511)

    return {
        "name": name,
        "tiled": is_tiled,
        "overviews": has_overviews,
        # "full_read_time": full_time,
        "partial_read_time": partial_time
    }

# local_cogTif = r"D:\edgedownload\LC08_L2SP_121038_20200922_20201006_02_T2\bandMergeCOG.tif"
# local_norTif = r"D:\edgedownload\LC08_L2SP_121038_20200922_20201006_02_T2\bandMerge.tif"

minio_cogTif = "/vsicurl/http://223.2.43.228:30900/test-images/qa%2FbandMergeCOG.tif"
minio_norTif = "/vsicurl/http://223.2.43.228:30900/test-images/qa%2FbandMerge.tif"

results = []

# results.append(compare_tifs("Local COG", local_cogTif))
# results.append(compare_tifs("Local Normal", local_norTif))
results.append(compare_tifs("Remote COG", minio_cogTif))
results.append(compare_tifs("Remote Normal", minio_norTif))

print_info("📊 Summary Comparison Table")

header = f"{'Name':<20} | {'Tiled':<6} | {'Overviews':<10} | {'Size(MB)':<10} | {'FullRead(s)':<12} | {'PartialRead(s)'}"
print(header)
print("-" * len(header))

for r in results:
    print(r)
