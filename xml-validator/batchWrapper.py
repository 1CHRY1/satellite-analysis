import json
from glob import glob
import xml.etree.ElementTree as ET
import sys
import os
import re
from datetime import datetime

def load_product_configs(product_config_json_paths):
    print("[INFO] Loading multiple productConfig.json...\r\n")
    PRODUCT_CONFIGS = []
    index = 0
    for product_config_json_path in product_config_json_paths:
        index += 1
        print(f"[INFO] --------------- {index}th product -----------------\r\n")
        print(f"[INFO] Loading productConfig.json: {product_config_json_path}")
        PATH_EXISTS = os.path.exists(product_config_json_path)
        PRODUCT_CONFIG = {}
        if not PATH_EXISTS:
            exit_with_error(f"[ERROR] Product config file not found: {product_config_json_path}\r\n")
        else:
            json_product_config = json.load(open(product_config_json_path, 'r',encoding="UTF-8"))
            for key, value in json_product_config.items():
                PRODUCT_CONFIG[key] = value
            PRODUCT_CONFIGS.append(PRODUCT_CONFIG)

            field_list = ["SCENE_PREFIX", "TEMP_OUTPUT_DIR", "CUR_SENSOR_NAME", "CUR_PRODUCT_NAME", "CUR_RESOLUTION", "CUR_PERIOD", "TAGS"]
            for field in field_list:
                if PRODUCT_CONFIG.get(field) is None:
                    exit_with_error(f"[ERROR] {field} is not set in {product_config_json_path}\r\n")
            tag_field_list = ["source", "production", "category"]
            for field in tag_field_list:
                if PRODUCT_CONFIG["TAGS"].get(field) is None:
                    exit_with_error(f"[ERROR] TAGS: {field} is not set in {product_config_json_path}\r\n")
            print("[INFO] --------------------------------------------------\r\n")
    print("[INFO] multiple productConfig.json loaded\r\n\r\n")
    return PRODUCT_CONFIGS

def process_folder(PRODUCT_CONFIG, scenes_dir):
    print(f"[INFO] ------------ Processing Scene Folders: {PRODUCT_CONFIG['CUR_SENSOR_NAME']} ------------\r\n")
    scene_prefix = PRODUCT_CONFIG["SCENE_PREFIX"]
    if isinstance(scene_prefix, str):
        prefix_tuple = (scene_prefix,)
    else:
        prefix_tuple = tuple(scene_prefix)

    folders = [
        f for f in os.listdir(scenes_dir)
        if os.path.isdir(os.path.join(scenes_dir, f)) and f.startswith(prefix_tuple)
    ]
    folders.sort()
    print(f"[INFO] Found {len(folders)} scene folders\r\n")
    SCENE_CONFIGS = []
    for i, folder in enumerate(folders):
        folder_path = os.path.join(scenes_dir, folder)
        if not os.path.isdir(folder_path):
            continue

        # ------------------------- The Start ---------------------------
        print(f"[INFO] Validation start: Scene Config {folder}, index: {i + 1}\r\n")
        # 设置初始景配置
        SCENE_CONFIG = set_initial_scene_info(PRODUCT_CONFIG)

        # ------------------------- Get and Parse XML ---------------------------
        # 获取xml文件
        xml_files = glob(os.path.join(folder_path, "*.xml"))
        if len(xml_files) > 1:
            # 优先选择文件名中包含'metadata'（不区分大小写）的xml文件
            metadata_xml_files = [f for f in xml_files if 'metadata' in os.path.basename(f).lower()]
            if metadata_xml_files:
                xml_files = metadata_xml_files
        elif len(xml_files) == 0:
            exit_with_error(f"[ERROR] No XML file found in {folder_path}\r\n")
        XML_EXISTS = not(len(xml_files) == 0)
        if XML_EXISTS:
            xml_path = xml_files[0]
            SCENE_CONFIG["XML_PATH"] = xml_path

            # 解析xml文件
            SCENE_CONFIG = parse_xml(xml_path, SCENE_CONFIG)

        # ------------------------- Get and Validate Tifs ---------------------------
        # 获取tif文件（多波段和cloud）
        for file in os.listdir(folder_path):
            if file.lower().endswith('tif') or file.lower().endswith('tiff'):
                if "cloud" in file.lower():
                    SCENE_CONFIG["CLOUD_PATH"] = os.path.join(folder_path, file)
                else:
                    if SCENE_CONFIG["IS_SINGLE"] is False:
                        SCENE_CONFIG["SCENE_PATH"] = os.path.join(folder_path, file)
        
        if SCENE_CONFIG["SCENE_PATH"] is None:
            # 尝试单波段获取
            if not os.path.exists(folder_path):
                exit_with_error(f"[ERROR]: Path not exist: {folder_path}")
            
            # 获取所有子文件夹
            subfolders = [f for f in os.listdir(folder_path) 
                        if os.path.isdir(os.path.join(folder_path, f))]
            if not subfolders:
                exit_with_error(f"[ERROR] No scene file found in {folder_path}\r\n")
            else:
                # 优先选择名为'bands'的文件夹（不区分大小写）
                target_folder = None
                for folder in subfolders:
                    if folder.lower() == 'bands':
                        target_folder = folder
                        break
                # 如果没有找到'bands'文件夹，选择第一个文件夹
                if target_folder is None:
                    target_folder = subfolders[0]

                target_folder_path = os.path.join(folder_path, target_folder)
        
                # 获取所有TIF/TIFF文件
                tif_files = []
                for file in os.listdir(target_folder_path):
                    if file.lower().endswith(('.tif', '.tiff')):
                        tif_files.append(file)
                if not tif_files:
                    exit_with_error(f"[ERROR]: No tif files found in {target_folder_path}")
                print(f"Found {len(tif_files)} TIF file in {target_folder_path}")

                band_files = []
                
                # 正则表达式匹配B1/Band1等模式
                pattern = re.compile(r'(B|Band)(\d+)(?:\..+)?$', re.IGNORECASE)
                for file_path in tif_files:
                    # 获取不带扩展名的文件名
                    file_name = os.path.splitext(os.path.basename(file_path))[0]
                    match = pattern.search(file_name)
                    if match:
                        band_num = int(match.group(2))
                        band_files.append((band_num, os.path.join(target_folder_path, file_path)))
                # 处理有波段信息的文件
                result = [{'band': band, 'path': path} for band, path in sorted(band_files, key=lambda x: x[0])]
                # 最后按波段号排序
                result.sort(key=lambda x: x['band'])

                SCENE_CONFIG["SCENE_PATH"] = result
                # exit_with_error(f"[ERROR] No scene file found in {folder_path}\r\n")
            
        # ------------------------- Validate XMLs ---------------------------
        if XML_EXISTS:
            # Validate xml file
            field_list = ["TL", "TR", "BR", "BL"]
            for field in field_list:
                if SCENE_CONFIG["XML"][field] is None:
                    exit_with_error(f"[ERROR] {field} is not set in {xml_path}\r\n")
            image_time = SCENE_CONFIG["XML"]["IMAGE_TIME"]
            try:
                datetime.strptime(image_time, "%Y-%m-%d %H:%M:%S")
            except Exception:
                exit_with_error(f"[ERROR] IMAGE_TIME format error: {image_time}, should be yyyy-mm-dd hh:mm:ss, file: {xml_path}\r\n")
            SCENE_CONFIGS.append(SCENE_CONFIG)

        # ------------------------- The End ---------------------------
        if(i < 5):
            print(f"[INFO] Validation done: Scene Config {folder}, index: {i + 1}\r\n")
        elif(i == len(folders) - 1):
            print(f"[INFO] Validation done: Scene Config {folder}, index: {i + 1}, this is the last one\r\n")
        else:
            print(f"[INFO] ..")
    print("[INFO] ----------------------------------------\r\n\r\n")
    return SCENE_CONFIGS

def set_initial_scene_info(PRODUCT_CONFIG):
    SCENE_CONFIG = {}
    SCENE_CONFIG["TEMP_OUTPUT_DIR"] = PRODUCT_CONFIG["TEMP_OUTPUT_DIR"]
    SCENE_CONFIG["CUR_SENSOR_NAME"] = PRODUCT_CONFIG["CUR_SENSOR_NAME"]
    SCENE_CONFIG["CUR_PRODUCT_NAME"] = PRODUCT_CONFIG["CUR_PRODUCT_NAME"]
    SCENE_CONFIG["CUR_RESOLUTION"] = PRODUCT_CONFIG["CUR_RESOLUTION"]
    SCENE_CONFIG["CUR_PERIOD"] = PRODUCT_CONFIG["CUR_PERIOD"]
    SCENE_CONFIG["TAGS"] = PRODUCT_CONFIG["TAGS"]
    if PRODUCT_CONFIG.get("IS_SINGLE") is not None:
        SCENE_CONFIG["IS_SINGLE"] = PRODUCT_CONFIG["IS_SINGLE"]
    else:
        SCENE_CONFIG["IS_SINGLE"] = True
    SCENE_CONFIG["XML_PATH"] = None
    SCENE_CONFIG["SCENE_PATH"] = None
    SCENE_CONFIG["CLOUD_PATH"] = None
    SCENE_CONFIG["XML"] = {
        "IMAGE_TIME": None,
        "CLOUD": "0",
        "TL": None,
        "TR": None,
        "BR": None,
        "BL": None
    }
    return SCENE_CONFIG

def check_time_format(time_str):
    try:
        datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")
        return True
    except Exception:
        return False

def parse_xml(xml_path, SCENE_CONFIG):
    try:
        # 解析XML文件
        tree = ET.parse(xml_path)
        root = tree.getroot()

        # 获取时间信息
        start_time = root.find(".//StartTime")
        end_time = root.find(".//EndTime")
        center_time = root.find(".//CenterTime")
        produce_time = root.find(".//ProduceTime")
        receive_time = root.find(".//ReceiveTime")
        
        if start_time is not None and start_time.text and check_time_format(start_time.text):
            SCENE_CONFIG["XML"]["IMAGE_TIME"] = start_time.text
        elif end_time is not None and end_time.text and check_time_format(end_time.text):
            SCENE_CONFIG["XML"]["IMAGE_TIME"] = end_time.text
        elif center_time is not None and center_time.text and check_time_format(center_time.text):
            SCENE_CONFIG["XML"]["IMAGE_TIME"] = center_time.text
        elif produce_time is not None and produce_time.text and check_time_format(produce_time.text):
            SCENE_CONFIG["XML"]["IMAGE_TIME"] = produce_time.text
        elif receive_time is not None and receive_time.text and check_time_format(receive_time.text):
            SCENE_CONFIG["XML"]["IMAGE_TIME"] = receive_time.text
        else:
            exit_with_error(f"[ERROR] No time found in {xml_path} or time format error, should be yyyy-mm-dd hh:mm:ss\r\n")
        # Get cloud info
        cloud_value = root.find(".//CloudCoverPercent")
        if cloud_value is not None and cloud_value.text:
            SCENE_CONFIG["XML"]["CLOUD"] = cloud_value.text
        
        # 获取坐标信息
        def get_coords(lat_elem, lon_elem):
            lat = root.find(f".//{lat_elem}")
            lon = root.find(f".//{lon_elem}")
            if lat is not None and lon is not None and lat.text and lon.text:
                return [lon.text, lat.text]
            else:
                exit_with_error(f"[ERROR] No {lat_elem} or {lon_elem} found in {xml_path}\r\n")
        SCENE_CONFIG["XML"]["TL"] = get_coords("TopLeftLatitude", "TopLeftLongitude")
        SCENE_CONFIG["XML"]["TR"] = get_coords("TopRightLatitude", "TopRightLongitude")
        SCENE_CONFIG["XML"]["BR"] = get_coords("BottomRightLatitude", "BottomRightLongitude")
        SCENE_CONFIG["XML"]["BL"] = get_coords("BottomLeftLatitude", "BottomLeftLongitude")
    except Exception as e:
        exit_with_error(f"[ERROR] Error parsing XML file: {e}\r\nXML file: {xml_path}\r\n")
    finally:
        return SCENE_CONFIG

def exit_with_error(error_message):
    print(f"\033[91m{error_message}\033[0m")
    # sys.exit(1)