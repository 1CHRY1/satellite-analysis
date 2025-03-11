import rasterio
from shapely.geometry import box
from pyproj import Transformer
import geopandas as gpd

def getTifBBoxAndCrs(tiff_path):
    try:
        with rasterio.open(tiff_path) as src:
            bounds = src.bounds
            original_crs = src.crs
            original_geometry = box(*bounds)

            if original_crs.to_epsg() == 4326:
                return original_geometry, original_crs
            transformer = Transformer.from_crs(original_crs, 'EPSG:4326', always_xy=True)
            minx, miny = transformer.transform(bounds.left, bounds.bottom)
            maxx, maxy = transformer.transform(bounds.right, bounds.top)

            new_geometry = box(minx, miny, maxx, maxy)
            new_crs = rasterio.crs.CRS.from_epsg(4326)

            return new_geometry, new_crs

    except Exception as e:
        print(f"Wrong when reading TIFF: {e}")
        return None, None

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