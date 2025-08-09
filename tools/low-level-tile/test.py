import mysql.connector

# # 连接到 MySQL 数据库
# connection = mysql.connector.connect(
#     host='223.2.34.8',        # 例如：'localhost' 或 '127.0.0.1'
#     port=31036,         # 例如：3306
#     user='root',        # 例如：'root'
#     password='123456',# 你的密码
#     database='satellite' # 你要连接的数据库名
# )

# # 创建一个游标对象
# cursor = connection.cursor()

# # 执行 SQL 查询
# # 列出databse中所有的表格
# # cursor.execute("SHOW TABLES")
# # 列出scene_table中的所有列
# cursor.execute("SHOW COLUMNS FROM scene_table")
# # cursor.execute("SHOW INDEX FROM scene_table")

# # 获取查询结果
# results = cursor.fetchall()

# # 打印查询结果
# for row in results:
#     print(row)

# # 关闭游标和连接
# cursor.close()
# connection.close()


import rasterio
from rio_tiler.io import COGReader
from rasterio.enums import Resampling
import random
from rio_tiler.mosaic import mosaic_reader
from rasterio.merge import merge
import io
from rasterio.merge import merge
from rasterio.plot import show
from rasterio.crs import CRS

path = "GF-1_PMS/Level_1A/tif/GF1B_PMS_E116.9_N36.9_20250414_L1A1228678698_beauty/Band_1.tif"
bulket = 'test-images'
prefix  = "223.2.34.8:30900"
# minio_client = Minio("223.2.34.8:30900", access_key='minioadmin', secret_key='minioadmin', secure=False)  
print("ssssssss")
cog_path = "http://223.2.34.8:30900/test-images/GF-1_PMS/Level_1A/tif/GF1B_PMS_E116.9_N36.9_20250414_L1A1228678698_beauty/Band_1.tif"
tif_list = []

with COGReader(cog_path, options={'nodata': 0}) as reader:
    info = reader.info()
    preview = reader.preview(indexes=1, max_size=100)
    wgs84_crs = CRS.from_epsg(4326)
    preview = preview.reproject(dst_crs=wgs84_crs)
    print(preview.crs)
    
    # save preiview to file in memory
    memory_file = io.BytesIO()
    preview.to_raster(memory_file)
    memory_file.seek(0)
    tif_list.append(memory_file)
with rasterio.open(tif_list[0]) as in_memory_raster:
    print(in_memory_raster.profile) 
    #resize后save
    data = in_memory_raster.read(1)
    show(data,cmap='terrain')
    # 获取原始的宽度和高度
    original_width = in_memory_raster.width
    original_height = in_memory_raster.height
    
    # 设置新的尺寸，假设我们要将宽度和高度缩小为原来的一半
    new_width = original_width // 2
    new_height = original_height // 2    
    # 更新 profile 信息
    new_profile = in_memory_raster.profile.copy()
    new_profile.update(
        width=new_width,
        height=new_height,
        transform=in_memory_raster.transform * in_memory_raster.transform.scale(
            (original_width / new_width), (original_height / new_height)
        )
    )
    resized_data = in_memory_raster.read(
        1, 
        out_shape=(1, new_height, new_width), 
        resampling=Resampling.bilinear  # 使用线性插值进行缩放
    )    
    with rasterio.open("resized_output.tif", 'w', **new_profile) as dst:
        dst.write(resized_data, 1)  # 写入第一波段的数据    

    # print(preview)
    # print(info.height)

    # temp_img_data = reader.tile(x, y, z, tilesize=256)