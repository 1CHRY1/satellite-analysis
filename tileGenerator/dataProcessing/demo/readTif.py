import sys
import os
import geopandas as gpd
import rasterio
from rasterio.mask import mask
from shapely.geometry import box

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(project_root)

from Utils.geometryUtils import getTifBBoxAndCrs, save_bbox_to_shp
from Utils.gridUtils import getGridsByBBox
from Utils.tifUtils import clip_raster_by_polygon, is_polygon_boundary_valid, is_polygon_intersect_raster

def save_bbox_to_shp(bbox_list, output_path):
    # 创建一个空的 GeoDataFrame
    gdf = gpd.GeoDataFrame(columns=['geometry'], geometry='geometry')

    # 遍历 bbox 列表
    for bbox in bbox_list:
        minx, miny, maxx, maxy = bbox.bounds
        # 创建一个 Shapely 的 box 对象
        geom = box(minx, miny, maxx, maxy)
        # 将 box 对象添加到 GeoDataFrame 中
        gdf = gdf._append({'geometry': geom}, ignore_index=True)

    # 设置 GeoDataFrame 的坐标系（这里假设为 WGS84）
    gdf.crs = 'EPSG:4326'

    # 保存为 shp 文件
    gdf.to_file(output_path)

path = "D:\\1study\\Work\\2025_03_05_satellite\\tileGenerator"

tiff_path = f'{path}\\landset5\\LT51190382000261BJC00\\LT51190382000261BJC00_Origin.TIF'
geometry, crs = getTifBBoxAndCrs(tiff_path)

grid_list = getGridsByBBox(geometry)
# save_bbox_to_shp(grid_list, r"E:/DownLoads/test")
for i in range(len(grid_list)):
    if (is_polygon_intersect_raster(tiff_path, grid_list[i]) and is_polygon_boundary_valid(tiff_path, grid_list[i])):
        clip_raster_by_polygon(tiff_path, grid_list[i], f"D:\\1study\\Work\\2025_03_05_satellite\\tileGenerator\\tiles_pro\\{i}.tif")
# clip_raster_by_shapefile(tiff_path, "E:\\DownLoads\\test\\agrid.shp", "E:\\DownLoads\\test\\test.tif")
# clipRasterByBBox(grid_list[600], tiff_path, "E:\\DownLoads\\test\\test.tif")
print(grid_list)