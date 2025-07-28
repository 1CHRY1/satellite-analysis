import os

import rasterio
from rio_cogeo.cogeo import cog_translate, cog_info
from rio_cogeo.profiles import cog_profiles
import numpy as np

########## TIF 转 COG ##########
def convert_tif2cog(tif_path, SCENE_CONFIG):
    if cog_info(tif_path)["COG"]:
        return tif_path
    else:
        temp_dir = SCENE_CONFIG["TEMP_OUTPUT_DIR"]
        output_cog_tif = os.path.join(temp_dir, os.path.basename(tif_path))
        with rasterio.open(tif_path) as src:
            profile = cog_profiles.get("deflate")
            # cog_translate(src, output_cog_tif, profile, in_memory=False)
            cog_translate(src, output_cog_tif, profile, in_memory=True, overview_resampling="nearest", resampling="nearest", allow_intermediate_compression=False, temporary_compression="LZW", config={"GDAL_NUM_THREADS": "ALL_CPUS"})
        return output_cog_tif

import numpy as np
import rasterio
from rasterio.enums import Compression

def writeCog():
    # 输入文件路径（原始 512x512 块大小的 TIFF）
    input_tif_path = r"/Users/paxton/Downloads/1653_296.tif"
    # 输出文件路径（新的 256x256 块大小的 COG）
    output_tif_path = r"/Users/paxton/Downloads/1653_296_256block.tif"

    # 1. 读取原始数据
    with rasterio.open(input_tif_path) as src:
        img = src.read()  # 读取所有波段数据
        transform = src.transform
        crs = src.crs

    # 2. 写入新的 COG，强制块大小为 256x256
    with rasterio.open(
        output_tif_path, 'w',
        driver='COG',  # 使用 COG 驱动
        height=img.shape[1],
        width=img.shape[2],
        count=img.shape[0],  # 波段数
        dtype=img.dtype,
        nodata=0,  # 设置 NoData 值（根据需求调整）
        crs=crs,
        transform=transform,
        BIGTIFF='YES',  # 处理大文件
        # COG 专用选项：强制块大小为 256x256
        BLOCKSIZE=256,
        COMPRESS='LZW',  # 压缩算法
        OVERVIEWS='AUTO',  # 自动生成金字塔
        OVERVIEW_RESAMPLING='NEAREST'  # 金字塔重采样方法
    ) as dst:
        dst.write(img)  # 写入数据

    print(f"COG 文件已生成，块大小强制为 256x256：{output_tif_path}")

# 调用函数
writeCog()