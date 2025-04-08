from types import SimpleNamespace
from typing import List, Dict
import os

from osgeo import osr, gdal
from dataProcessing.imageConfig import CUR_PATTERN, CUR_PERIOD, CUR_TILE_LEVEL


# 获取本地文件列表
def get_files(directory: str) -> Dict[str, List[str]]:
    """
    扩展原函数，返回每个影像ID对应的多个波段路径。
    :param directory: 目录路径
    :return: 字典形式，key为影像ID，value为该影像的多个波段路径列表
    """
    files = {}

    # 遍历目录下的所有文件
    for file in os.listdir(directory):
        match = CUR_PATTERN.match(file)
        if match and file.lower().endswith('.tif'):
            # 提取文件中的影像ID和波段号
            image_id = match.group(0).split('_SR_B')[0]  # 优先去除 '_SR_B'
            if image_id == match.group(0):  # 如果 '_SR_B' 不存在，则尝试去除 '_B'
                image_id = match.group(0).split('_B')[0]
            band_number = match.group(4)  # 获取波段号B1、B2、B3等

            # 初始化影像的列表，如果尚未添加
            if image_id not in files:
                files[image_id] = []

            # 将文件路径添加到对应的影像ID下
            files[image_id].append(os.path.join(directory, file))

    return files


# 提取tif信息
def extract_info(file_path: str):
    """
    解析影像文件路径中的信息。

    :param file_path: 影像文件路径（单个文件）
    :return: 包含影像信息的字典，文件名不匹配时返回 None
    """
    from dataProcessing.Utils.tifUtils import convert_bbox_to_4326
    file_name = os.path.basename(file_path)  # 获取文件名
    match = CUR_PATTERN.match(file_name)
    if match:
        column_id, row_id, image_time, band = match.groups()
        # 读取影像文件
        dataset = gdal.Open(file_path)
        if not dataset:
            return None  # 影像文件无法打开

        # 获取影像坐标系信息
        projection = dataset.GetProjection()
        spatial_ref = osr.SpatialReference()
        spatial_ref.ImportFromWkt(projection)
        crs = spatial_ref.GetAttrValue("AUTHORITY", 1)  # 获取 EPSG 代码

        # 计算影像范围 (Bounding Box)
        geotransform = dataset.GetGeoTransform()
        bbox = convert_bbox_to_4326(dataset)

        resolution_x = abs(geotransform[1])
        # resolution_y = abs(geotransform[5])
        resolution = f"{resolution_x}m"
        # 释放资源
        dataset = None
        return SimpleNamespace(
            file_name=file_name,
            column_id=column_id,
            row_id=row_id,
            image_time=image_time,
            band=band,
            crs=crs,
            bbox=bbox,
            tile_level_num=1,
            tile_levels=CUR_TILE_LEVEL,
            resolution=resolution,
            period=CUR_PERIOD,
        )

    return None  # 文件名不匹配
