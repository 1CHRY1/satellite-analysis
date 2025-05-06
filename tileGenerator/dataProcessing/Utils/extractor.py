import math
import os.path
import uuid
from types import SimpleNamespace

from lxml import etree
from osgeo import gdal, osr

from dataProcessing.Utils.filenameUtils import get_tif_files
from dataProcessing.Utils.metaDataUtils import get_xml_content, get_bbox_from_xml_node
from dataProcessing.Utils.minioUtil import uploadLocalFile, upload_file_by_mc
from dataProcessing.Utils.tifUtils import convert_tif2cog, create_preview_image

EARTH_RADIUS                                    =       6371008.8
EARTH_CIRCUMFERENCE                             =       2 * math.pi * EARTH_RADIUS
CUR_TILE_LEVEL                                  =       '40031*20016'
CUR_CLOUD_VALUE                                 =       0
SCENE_CONFIG                                    =       {}
DB_CONFIG                                       =       {}

def grid2lnglat(grid_x, grid_y, world_grid_num):
    """return the left-top geo position of the grid"""
    lng = grid_x / world_grid_num[0] * 360.0 - 180.0
    lat = 90.0 - grid_y / world_grid_num[1] * 180.0
    return lng, lat # left top (lng, lat)


def preset():
    global SCENE_CONFIG, DB_CONFIG
    scene_path = SCENE_CONFIG["SCENE_PATH"]
    temp_dir = SCENE_CONFIG["TEMP_OUTPUT_DIR"]
    try:
        file_path_list = []

        # 打开多波段的tif文件
        print("正在读取tif文件，请稍后...")
        dataset = gdal.Open(scene_path)
        print("读取完毕")

        if not dataset:
            raise FileNotFoundError(f"Unable to open file: {scene_path}")


        # 获取图像的空间参考和仿射变换信息
        geotransform = dataset.GetGeoTransform()
        projection = dataset.GetProjection()
        spatial_ref = osr.SpatialReference()
        spatial_ref.ImportFromWkt(projection)
        crs = spatial_ref.GetAttrValue("AUTHORITY", 1)  # 获取 EPSG 代码

        # 获取图像的波段数
        num_bands = dataset.RasterCount

        # 循环处理每个波段
        for band_idx in range(1, num_bands + 1):
            print(f"正在写入波段 {band_idx} ...")
            # 获取波段数据
            band = dataset.GetRasterBand(band_idx)
            band_data = band.ReadAsArray()

            # 创建保存单波段文件的路径
            band_filename = os.path.join(temp_dir, f"{band_idx}_{str(uuid.uuid4())}.tif")

            # 创建并保存单波段tif文件
            driver = gdal.GetDriverByName('GTiff')
            single_band_dataset = driver.Create(
                band_filename,
                dataset.RasterXSize,
                dataset.RasterYSize,
                1,  # 单波段
                gdal.GDT_Float32  # 数据类型
            )

            # 设置空间参考信息
            single_band_dataset.SetGeoTransform(geotransform)
            single_band_dataset.SetProjection(projection)

            # 将波段数据写入新文件
            single_band_dataset.GetRasterBand(1).WriteArray(band_data)
            print(f"波段 {band_idx} 写入完毕")
            # 清理
            single_band_dataset.FlushCache()
            file_path_list.append(band_filename)

        # 这里可以调用处理PNG的代码
        print("正在处理PNG...")
        if SCENE_CONFIG["PREVIEW_PATH"] is not None:
            if os.path.exists(SCENE_CONFIG["PREVIEW_PATH"]):
                png_path = SCENE_CONFIG["PREVIEW_PATH"]
            else:
                png_path = create_preview_image(dataset, os.path.join(temp_dir, f"{uuid.uuid4()}.png"))
        else:
            png_path = create_preview_image(dataset, os.path.join(temp_dir, f"{uuid.uuid4()}.png"))
        print("PNG处理完成")

        return file_path_list, png_path, crs

    except Exception as e:
        print(f"Error processing the file: {e}")
        return []


def get_scene_info(object_prefix):
    global SCENE_CONFIG, DB_CONFIG
    xml_path, scene_path = SCENE_CONFIG["XML_PATH"], SCENE_CONFIG["SCENE_PATH"]
    
    xml_content = get_xml_content(xml_path)
    root = etree.XML(xml_content, parser=etree.XMLParser(recover=True))
    scene_info = SimpleNamespace(image_info_list=[], bands=[], scene_name="")
    node = root.find('ProductMetaData')
    scene_info.cloud = node.find('CloudPercent').text  # 获取 <CloudPercent> 标签
    scene_info.image_time =   node.find('EndTime').text
    scene_info.bbox = get_bbox_from_xml_node(node)
    scene_info.bands = node.find('Bands').text.split(',')
    scene_info.band_num = len(scene_info.bands)
    scene_info.resolution = SCENE_CONFIG["CUR_RESOLUTION"]
    scene_info.period = SCENE_CONFIG["CUR_PERIOD"]

    scene_info.scene_name = os.path.splitext(os.path.basename(scene_path))[0]
    scene_info.tile_level_num = 1
    scene_info.tile_levels = CUR_TILE_LEVEL
    single_band_path_list, png_path, crs = preset()
    object_name = f"{object_prefix}/png/{scene_info.scene_name}.png"
    uploadLocalFile(png_path, DB_CONFIG["MINIO_IMAGES_BUCKET"], object_name)
    # 上传xml
    uploadLocalFile(SCENE_CONFIG["NEW_XML_PATH"], DB_CONFIG["MINIO_IMAGES_BUCKET"], f"{object_prefix}/xml/{scene_info.scene_name}.xml")
    scene_info.png_path = object_name
    scene_info.crs = crs
    return scene_info, single_band_path_list


def get_image_info(image_path, scene_name, object_prefix):
    global SCENE_CONFIG, DB_CONFIG
    image_info = SimpleNamespace()
    image_info.image_name = os.path.basename(image_path)
    image_info.band = int(image_info.image_name.split('_')[0])
    object_name = f"{object_prefix}/tif/{scene_name}/B{image_info.band}.tif"
    image_info.tif_path = object_name
    image_info.cloud = CUR_CLOUD_VALUE
    uploadLocalFile(convert_tif2cog(image_path), DB_CONFIG["MINIO_IMAGES_BUCKET"], object_name)
    return image_info


def get_tile_info(tile_path, scene_name, band_name, object_prefix):
    global SCENE_CONFIG, DB_CONFIG
    # get world grid num
    grid_resolution_in_meter = 1 * 1000
    world_grid_num_x = math.ceil(EARTH_CIRCUMFERENCE / grid_resolution_in_meter)
    world_grid_num_y = math.ceil(EARTH_CIRCUMFERENCE / 2.0 / grid_resolution_in_meter)
    world_grid_num = [world_grid_num_x, world_grid_num_y]

    tile_name = os.path.basename(tile_path)
    parts = tile_name.split('_')
    grid_id_x = int(parts[1])  # i 是分割后的第二部分
    grid_id_y = int(parts[2].split('.')[0])  # j 是分割后的第三部分，并去除 ".tif" 后缀

    # 根据tile_path, return tile_info
    tile_info = {}
    left_lng, top_lat = grid2lnglat(grid_id_x, grid_id_y, world_grid_num)
    right_lng, bottom_lat = grid2lnglat(grid_id_x + 1, grid_id_y + 1, world_grid_num)
    wgs84_corners = [(left_lng, top_lat), (right_lng, top_lat), (right_lng, bottom_lat), (left_lng, bottom_lat),
                     (left_lng, top_lat)]
    coords_str = ", ".join([f"{x} {y}" for x, y in wgs84_corners])
    wkt_polygon = f"POLYGON(({coords_str}))"

    tile_info['bbox'] = wkt_polygon
    tile_info['bucket'] = DB_CONFIG["MINIO_TILES_BUCKET"]
    # TODO
    tile_info['cloud'] = 0
    tile_info[
        'path'] = f"{object_prefix}/{scene_name}/{band_name}/{os.path.basename(tile_path)}"
    tile_info['column_id'] = grid_id_x
    tile_info['row_id'] = grid_id_y
    return tile_info


def get_scene_info_list(object_prefix):
    global SCENE_CONFIG, DB_CONFIG
    scene_info_list = []
    scene_info, single_band_path_list = get_scene_info(object_prefix)
    image_info_list = []
    for path in single_band_path_list:
        print(f"正在上传单波段影像 {path} ...")
        image_info = get_image_info(path, scene_info.scene_name, object_prefix)
        print(f"上传完毕")
        band_dir = os.path.join(SCENE_CONFIG["TILES_PATH"], f"band_{image_info.band}")
        band_name = f"B{image_info.band}"
        tile_path_list = get_tif_files(band_dir)
        tile_info_list = []
        print("正在上传瓦片...")
        upload_file_by_mc(band_dir, DB_CONFIG["MINIO_TILES_BUCKET"], f"{object_prefix}/{scene_info.scene_name}/{band_name}")
        print("正在整理瓦片信息...")
        for tile_path in tile_path_list:
            tile_info = get_tile_info(tile_path, scene_info.scene_name, band_name, object_prefix)
            tile_info_list.append(tile_info)
        image_info.tile_info_list = tile_info_list
        image_info_list.append(image_info)
        print(f"波段 {band_name} 瓦片信息整理完成")
    scene_info.image_info_list = image_info_list
    scene_info_list.append(scene_info)
    return scene_info_list


def set_initial_config(SC_CONFIG, D_CONFIG):
    global SCENE_CONFIG, DB_CONFIG
    SCENE_CONFIG = SC_CONFIG
    DB_CONFIG = D_CONFIG
