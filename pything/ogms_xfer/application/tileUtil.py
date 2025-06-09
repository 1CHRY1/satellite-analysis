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
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        data, mask = cog.part(bbox)
        bounds = bbox
        height, width = data.shape[1], data.shape[2]
        count = data.shape[0]
        dtype = data.dtype
        crs = cog.dataset.crs
        transform = from_bounds(*bounds, width=width, height=height)

        # 保存为TIF
        with rasterio.open(
            save_path,
            'w',
            driver='COG',
            height=height,
            width=width,
            count=count,
            dtype=dtype,
            crs=crs,
            transform=transform,
            nodata=nodata
        ) as dst:
            dst.write(data)
            dst.write_mask(mask)

        print(f"保存成功: {save_path}")


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
    if nodata is not None:
        options = {"nodata": nodata}
    else:
        options = {}
    
    with COGReader(cog_path, options=options) as cog:
        data, mask = cog.feature(shape=feature)
        # 通用提取Polygon和MultiPolygon的所有点
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
        minx, maxx = min(xs), max(xs)
        miny, maxy = min(ys), max(ys)
        bounds = (minx, miny, maxx, maxy)
        height, width = data.shape[1], data.shape[2]
        count = data.shape[0]
        dtype = data.dtype
        crs = cog.dataset.crs
        transform = from_bounds(*bounds, width=width, height=height)

        # 保存为TIF
        with rasterio.open(
            save_path,
            'w',
            driver='COG',
            height=height,
            width=width,
            count=count,
            dtype=dtype,
            crs=crs,
            transform=transform,
            nodata=nodata
        ) as dst:
            dst.write(data)
            dst.write_mask(mask)
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