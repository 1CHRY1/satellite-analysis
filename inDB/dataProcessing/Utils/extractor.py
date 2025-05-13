import math
import os.path
import uuid
from types import SimpleNamespace
import sys

from lxml import etree
from osgeo import gdal, osr
import json
from datetime import datetime, timezone, timedelta
from dataProcessing.Utils.filenameUtils import get_tif_files
from dataProcessing.Utils.metaDataUtils import get_xml_content, get_bbox_from_xml_node
from dataProcessing.Utils.minioUtil import uploadLocalFile, upload_file_by_mc
from dataProcessing.Utils.tifUtils import convert_tif2cog, convert_bbox_to_4326

EARTH_RADIUS                                    =       6371008.8
EARTH_CIRCUMFERENCE                             =       2 * math.pi * EARTH_RADIUS
CUR_TILE_LEVEL                                  =       '40031*20016'
CUR_CLOUD_VALUE                                 =       0
SCENE_CONFIG                                    =       {}
DB_CONFIG                                       =       {}


#####（转COG直接在上传前做就行）#####
##### 1. def preset() #####
##### -- 读取配置，是单波段还是多波段？
##### -- def get_basic_scene_info_from_ds(ds)
#####    -- 获取空间参考信息，time，bbox，band列表(?)信息，以防XML中没有
##### -- def process_single_band(scene_path)
#####    -- 单波段处理，读取band和path
#####    -- 读取其中一个波段即可，get_basic_scene_info_from_ds()
#####    -- （不需要预览图）
#####    -- 返回单波段路径列表和景的一些属性信息
##### -- def process_multi_band(scene_path)
#####    -- 多波段处理，大体还按下面的
#####    -- 读取多波段数据，get_basic_scene_info_from_ds()
#####    -- （不需要预览图）
#####    -- 生成的单波段convert_tif2cog()，返回单波段路径列表和景的一些属性信息

##### 2. def get_scene_info(band_path_list, scene_basic_info:dict, object_prefix)
#####    -- 主功能：属性数据组织
#####    -- (取消png_path, 换成cloud_path)
#####    -- 读取 XML 文件，获取信息，与scene_basic_info比较取优
#####    -- get scene_info.file_path(also SCENE_CONFIG["CLOUD_PATH"])
#####    -- set scene_info.cloud_path(temp field, object_name)
#####    -- 返回scene_info, band_path_list

##### 3. def get_image_info(image_path, scene_name, object_prefix)
#####    -- 主功能：组织单波段影像属性信息
#####    -- 组织信息
#####    -- get image_info.tif_path(object_name)
#####    -- set image_info.file_path(temp field)
#####    -- 返回image_info

##### 4. def upload_data(scene_info)
#####    -- 主功能：上传实体数据
#####    -- scene_info.image_info_list[${image_info.tif_path}, ${image_info.file_path}, ${image_info.cloud_path}, ${image_info.file_path}]
#####    -- uploadLocalFile(convert_tif2cog(image_path), DB_CONFIG["MINIO_IMAGES_BUCKET"], object_name)

##### 5. def main(object_prefix):
#####    -- preset()
#####    -- get_scene_info(band_path_list, scene_basic_info:dict, object_prefix)
#####    -- get_image_info()
#####    -- upload_data(scene_info)
#####    -- return

def grid2lnglat(grid_x, grid_y, world_grid_num):
    """return the left-top geo position of the grid"""
    lng = grid_x / world_grid_num[0] * 360.0 - 180.0
    lat = 90.0 - grid_y / world_grid_num[1] * 180.0
    return lng, lat # left top (lng, lat)


def get_basic_info_from_ds(dataset):
    basic_info = {}
    # 获取图像的空间参考和仿射变换信息
    basic_info["geotransform"] = dataset.GetGeoTransform()
    basic_info["projection"] = dataset.GetProjection()
    if basic_info["projection"] is None or basic_info["projection"] == "":
        print("缺失投影信息")
    spatial_ref = osr.SpatialReference()
    spatial_ref.ImportFromWkt(basic_info["projection"])
    basic_info["crs"] = spatial_ref.GetAttrValue("AUTHORITY", 1)  # 获取 EPSG 代码
    if basic_info["crs"] is None:
        print("缺失EPSG CODE，已恢复默认4326")
        basic_info["crs"] = 4326
    basic_info["bbox"] = convert_bbox_to_4326(dataset)
    if isinstance(SCENE_CONFIG["SCENE_PATH"], list):
        # 单波段
        scene_path_list = SCENE_CONFIG["SCENE_PATH"]
        basic_info["bands"] = [scene_path["band"] for scene_path in scene_path_list]
        basic_info["band_num"] = len(basic_info["bands"])
    else:
        # 多波段
        scene_path = SCENE_CONFIG["SCENE_PATH"] 
        basic_info["band_num"] = dataset.RasterCount
        basic_info["bands"] = list(range(1, basic_info["band_num"] + 1))
    return basic_info


def get_basic_info_from_xml():
    global SCENE_CONFIG, DB_CONFIG
    # 初始默认值
    basic_info = {
        "cloud": "0",
        "image_time": "2025-01-01 00:00:00",
        "bbox": None,
        "bands": None,
        "band_num": None,
        "resolution": SCENE_CONFIG["CUR_RESOLUTION"],
        "period": SCENE_CONFIG["CUR_PERIOD"]
    }

    # 读取初步XML
    xml_path = SCENE_CONFIG["XML_PATH"]
    try:
        xml_content = get_xml_content(xml_path)
        root = etree.XML(xml_content, parser=etree.XMLParser(recover=True))
        # node = root.find('ProductMetaData')
        imageMetaDataNode = root.find('ImageMetaData')
        productInfoNode = root.find('ProductInfo')

    except Exception as e:
        print(f"Error processing the xml: {e}")
        return basic_info
    if imageMetaDataNode is None or productInfoNode is None:
        print(f"Error processing the xml")
        return basic_info

    # 安全地获取各个标签，标签缺失则保持默认值
    cloud_nodes = imageMetaDataNode.xpath('.//ImageQuality/CloudCoverPercent')
    if cloud_nodes is not None and cloud_nodes[0] is not None and cloud_nodes[0].text:
        basic_info["cloud"] = cloud_nodes[0].text
    time_nodes = imageMetaDataNode.xpath('.//GeneralInfo/EndTime')
    if time_nodes is not None and time_nodes[0] is not None and time_nodes[0].text:
        basic_info["image_time"] = parse_time(time_nodes[0])
    try:
        productMetaDataNode = productInfoNode.find('ProductMetaData')
        if productMetaDataNode is not None:
            basic_info["bbox"] = get_bbox_from_xml_node(productMetaDataNode)
    except Exception:
        pass  # 保持默认值
    
    # 暂时不管band
    # bands_node = node.find('Bands')
    # if bands_node is not None and bands_node.text:
    #     basic_info["bands"] = bands_node.text.split(',')
    #     basic_info["band_num"] = len(basic_info["bands"])

    return basic_info


def parse_time(time_node):
    if time_node is None or not time_node.text:
        return None
    
    time_str = time_node.text.strip()
    BEIJING_TZ = timezone(timedelta(hours=8))
    
    # List of possible time formats to try
    time_formats = [
        '%Y-%m-%d %H:%M:%S',  # Existing format
        '%Y-%m-%dT%H:%M:%S.%fZ',  # ISO 8601 with milliseconds
        '%Y-%m-%dT%H:%M:%SZ',  # ISO 8601 without milliseconds
        '%Y-%m-%dT%H:%M:%S.%f%z',  # ISO 8601 with timezone
        '%Y-%m-%dT%H:%M:%S%z'  # ISO 8601 with timezone, no milliseconds
    ]
    
    for fmt in time_formats:
        try:
            # Parse the time, converting to local time if a timezone is present
            parsed_time = datetime.strptime(time_str, fmt)
            
            # If timezone is naive, assume BEIJING_TZ
            if parsed_time.tzinfo is None:
                parsed_time = parsed_time.replace(tzinfo=BEIJING_TZ)
            
            # Convert to local time and remove timezone info
            local_time = parsed_time.astimezone().replace(tzinfo=None)
            
            # Return in the desired format
            return local_time.strftime('%Y-%m-%d %H:%M:%S')
        except ValueError:
            continue
    
    # If no format matches, return None
    return None


def get_scene_basic_info(info_from_ds, info_from_xml):
    # 比较两个参数，得出最后结果
    # 以info_from_ds为主，冲突的变量是bbox/bands/band_num，需要新加cloud/image_time/resolution/period
    basic_info = info_from_ds
    basic_info["cloud"] = info_from_xml["cloud"]
    basic_info["image_time"] = info_from_xml["image_time"]
    basic_info["resolution"] = info_from_xml["resolution"]
    basic_info["period"] = info_from_xml["period"]
    # bbox XML优先, bands/band_num DS优先
    if info_from_xml["bbox"] is not None:
        basic_info["bbox"] = info_from_xml["bbox"]
    if basic_info["bbox"] is None:
        print("空间范围 Bbox 缺失，程序自动退出")
        sys.exit(1)
    if info_from_ds["bands"] is None:
        basic_info["bands"] = info_from_xml["bands"]
    if info_from_ds["band_num"] is None:
        basic_info["band_num"] = info_from_xml["band_num"]
    return basic_info


def process_single_band(scene_path_list):
    global SCENE_CONFIG, DB_CONFIG
    if len(scene_path_list) == 0:
        raise ValueError("scene_path_list is empty")
    print(f"正在读取单波段tif文件(波段：{scene_path_list[0]["band"]})，请稍后...")
    dataset = gdal.Open(scene_path_list[0]["path"])
    if not dataset:
        raise FileNotFoundError(f"Unable to open file: {scene_path_list[0]["path"]}")
    print("读取完毕")
    scene_basic_info = get_basic_info_from_ds(dataset)
    band_list = scene_path_list
    return band_list, scene_basic_info


def process_multi_band(scene_path):
    global SCENE_CONFIG, DB_CONFIG
    temp_dir = SCENE_CONFIG["TEMP_OUTPUT_DIR"]
    print("正在读取多波段tif文件，请稍后...")
    dataset = gdal.Open(scene_path)
    if not dataset:
        raise FileNotFoundError(f"Unable to open file: {scene_path}")
    print("读取完毕")
    scene_basic_info = get_basic_info_from_ds(dataset)
    try:
        band_list = []

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
            single_band_dataset.SetGeoTransform(scene_basic_info["geotransform"])
            single_band_dataset.SetProjection(scene_basic_info["projection"])

            # 将波段数据写入新文件
            single_band_dataset.GetRasterBand(1).WriteArray(band_data)
            print(f"波段 {band_idx} 写入完毕")
            # 清理
            single_band_dataset.FlushCache()
            band_list.append({"path": band_filename, "band": band_idx})

        return band_list, scene_basic_info

    except Exception as e:
        print(f"Error processing the file: {e}")
        return [], scene_basic_info


def preset():
    global SCENE_CONFIG, DB_CONFIG
    if isinstance(SCENE_CONFIG["SCENE_PATH"], list):
        # 单波段是一个json列表(path, band)
        scene_path_list = SCENE_CONFIG["SCENE_PATH"]
        band_list, scene_basic_info = process_single_band(scene_path_list)
    else:
        # 多波段只是一个路径
        scene_path = SCENE_CONFIG["SCENE_PATH"]
        band_list, scene_basic_info = process_multi_band(scene_path)
    return band_list, scene_basic_info


def get_scene_info(object_prefix, scene_basic_info_from_ds):
    global SCENE_CONFIG, DB_CONFIG
    scene_path = SCENE_CONFIG["SCENE_PATH"]
    #初始化scene_info
    scene_basic_info_from_xml = get_basic_info_from_xml()
    scene_basic_info = get_scene_basic_info(scene_basic_info_from_ds, scene_basic_info_from_xml)
    scene_info = scene_basic_info
    scene_info["image_info_list"] = []
    scene_info["scene_name"] = ""
    scene_info["tile_level_num"] = 1
    scene_info["tile_levels"] = CUR_TILE_LEVEL
    scene_info["tags"] = json.dumps(SCENE_CONFIG["TAGS"])

    if isinstance(scene_path, list):
        # 单波段
        file_path = scene_path[0]["path"]
        parent_dir = os.path.dirname(file_path)  # "D:/CODE/BANDS"
        grandparent_dir = os.path.dirname(parent_dir)  # "D:/CODE"
        grandparent_name = os.path.basename(grandparent_dir)  # "CODE"
        scene_info["scene_name"] = grandparent_name
    else:
        # 多波段
        scene_info["scene_name"] = os.path.basename(os.path.dirname(scene_path))
    
    if SCENE_CONFIG.get("CLOUD_PATH") is not None:
        object_name = f"{object_prefix}/cloud/{scene_info["scene_name"]}_cloud.tif"
        scene_info["cloud_path"] = object_name
    else:
        scene_info["cloud_path"] = None
    # single_band_path_list, png_path, crs = preset()
    # uploadLocalFile(png_path, DB_CONFIG["MINIO_IMAGES_BUCKET"], object_name)
    # 上传xml
    # uploadLocalFile(SCENE_CONFIG["NEW_XML_PATH"], DB_CONFIG["MINIO_IMAGES_BUCKET"], f"{object_prefix}/xml/{scene_info.scene_name}.xml")
    
    return scene_info


def get_image_info(image, scene_info, object_prefix):
    global SCENE_CONFIG, DB_CONFIG
    image_info = {}
    image_path = image["path"]
    image_info["band"] = image["band"]
    image_info["image_name"] = os.path.basename(image_path)
    image_info["tif_path"] = f"{object_prefix}/tif/{scene_info["scene_name"]}/Band_{image_info["band"]}.tif"
    image_info["file_path"] = image_path
    image_info["cloud"] = scene_info["cloud"]
    return image_info


def upload_data(scene_info):
    global SCENE_CONFIG, DB_CONFIG
    # 上传单波段影像
    image_info_list = scene_info["image_info_list"]
    for image_info in image_info_list:
        uploadLocalFile(convert_tif2cog(image_info["file_path"]), DB_CONFIG["MINIO_IMAGES_BUCKET"], image_info["tif_path"])
    # 上传云量波段
    if SCENE_CONFIG.get("CLOUD_PATH") is not None:
        uploadLocalFile(convert_tif2cog(SCENE_CONFIG["CLOUD_PATH"]), DB_CONFIG["MINIO_IMAGES_BUCKET"], scene_info["cloud_path"])


def main(object_prefix):
    band_list, scene_basic_info_from_ds = preset()
    scene_info = get_scene_info(object_prefix, scene_basic_info_from_ds)
    image_info_list = []
    for band_item in band_list:
        image_info = get_image_info(band_item, scene_info, object_prefix)
        image_info_list.append(image_info)
    scene_info["image_info_list"] = image_info_list
    upload_data(scene_info)
    return scene_info


def set_initial_config(SC_CONFIG, D_CONFIG):
    global SCENE_CONFIG, DB_CONFIG
    SCENE_CONFIG = SC_CONFIG
    DB_CONFIG = D_CONFIG
