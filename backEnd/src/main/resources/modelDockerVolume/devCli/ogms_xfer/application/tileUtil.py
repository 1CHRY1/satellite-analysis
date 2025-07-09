from rio_tiler.io import COGReader
from typing import Tuple, Dict
import rasterio
from rasterio.transform import from_bounds

def read_image_point(cog_path: str, lon: float, lat: float, nodata=None):
    """
    Read the point of the COG file.
    """
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        pointData = cog.point(lon, lat)
        pointValue = pointData.data[0].tolist()
        
    return pointValue


def read_image_box(cog_path: str, bbox: Tuple[float, float, float, float], nodata=None):
    """
    Read the part of the COG file by bbox.
    Args:
        cog_path: The path to the COG file.
        bbox: The bounding box of the part.
        nodata: The nodata value of the COG file.
    Returns:
        data: The data of the part.
        mask: The mask of the part.
    """
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        data, mask = cog.part(bbox)
    return data, mask


def read_image_box_save(cog_path: str, bbox: Tuple[float, float, float, float], save_path: str, nodata=None):
    """
    Read the part of the COG file by bbox.
    Args:
        cog_path: The path to the COG file.
        bbox: The bounding box of the part.
        nodata: The nodata value of the COG file.
        save_path: The path to save the part.
    Returns:
        None
    """
    minx, miny, maxx, maxy = bbox
    coordinates = [[
        [minx, miny],
        [minx, maxy],
        [maxx, maxy],
        [maxx, miny],
        [minx, miny]
    ]]
    feature = {
        "type": "Feature",
        "properties": {},
        "geometry": {
            "type": "Polygon",
            "coordinates": coordinates
        }
    }
    return read_image_feature_save(cog_path, feature, save_path, nodata)


def read_image_feature(cog_path: str, feature: Dict, nodata=None):
    """
    Read the part of the COG file by geojson feature.
    Args:
        cog_path: The path to the COG file.
        feature: The geojson feature.
        nodata: The nodata value of the COG file.
    Returns:
        data: The data of the part.
        mask: The mask of the part.
    """
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        data, mask = cog.feature(shape=feature)
    return data, mask


def read_image_feature_save(cog_path: str, feature: Dict, save_path: str, nodata=None) -> str:
    """
    Read the part of the COG file by geojson feature.
    Args:
        cog_path: The path to the COG file.
        feature: The geojson feature.
        nodata: The nodata value of the COG file.   
        save_path: The path to save the part.
    Returns:
        None
    """
    """
    更简洁的版本，直接使用rio_tiler的part方法
    """
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        # 使用part方法，直接传入bbox
        geometry = feature.get("geometry")
        coords = geometry.get("coordinates")
        geom_type = geometry.get("type")
        
        if geom_type == "Polygon":
            all_points = [pt for ring in coords for pt in ring]
        elif geom_type == "MultiPolygon":
            all_points = [pt for polygon in coords for ring in polygon for pt in ring]
        else:
            raise ValueError("只支持Polygon和MultiPolygon")
        
        xs = [pt[0] for pt in all_points]
        ys = [pt[1] for pt in all_points]
        bbox = (min(xs), min(ys), max(xs), max(ys))
        
        # 使用part方法获取数据，它会自动处理坐标转换
        img = cog.part(bbox, dst_crs=cog.dataset.crs)
        
        # 保存为TIF
        with rasterio.open(
            save_path,
            'w',
            driver='COG',
            height=img.height,
            width=img.width,
            count=img.count,
            dtype=img.array.dtype,
            crs=cog.dataset.crs,
            transform=img.transform,
            nodata=nodata
        ) as dst:
            dst.write(img.array)
            if hasattr(img, 'mask'):
                dst.write_mask(img.mask)
    
    return save_path


def get_cog_statistics(cog_path: str, nodata=None):
    """
    Get the statistics of the tile.
    Args:
        cog_path: The path to the COG file.
        bbox: The bounding box of the part.
        nodata: The nodata value of the COG file.
    Returns:
        statistics: The statistics of the tile.
    """
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        statistics = cog.statistics()
    return statistics