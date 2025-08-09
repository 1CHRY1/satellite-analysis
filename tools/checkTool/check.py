import sys, os
from pathlib import Path
import ssl

## 环境变量和文件模块 ##################################################################################
# nuitka 获取程序所在路径（对于打包的 .exe 文件）
if getattr(sys, 'frozen', False):
    base_path = os.path.dirname(sys.argv[0])
else:
    base_path = os.path.dirname(__file__)


import _cffi_backend
## 此处注意替换本地环境中的两个文件，一个是morecantile，一个是certifi，QQ群里有相应文件


## Main ##############################################################################################
from Utils.mySqlUtils import *
from Utils.minioUtil import *
from Utils.validate import *
import argparse
import json
import sys
import traceback


def check():
    append_to_log(args.output_log, f"The following scenes do not exist in MinIO:\r\n")
    scenes = get_all_scenes()
    for scene in scenes:
        scene_id = scene["scene_id"]
        scene_name = scene["scene_name"]
        images = get_images_by_scene_id(scene_id)
        flag = True
        for image in images:
            tif_path = image["tif_path"]
            if not file_exists(DB_CONFIG["MINIO_IMAGES_BUCKET"], tif_path):
                flag = False
                break
        if flag:
            pass
        else:
            append_to_log(args.output_log, f"{scene_name}\r\n")
            print(f"\033[91m{scene_name} does not exist in MinIO.\033[0m\n")
            for image in images:
                delete_image_by_id(image["image_id"])
            delete_scene_by_id(scene_id)

    print("------------ Program End ------------\r\n\r\n")


    
# 命令行参数
parser = argparse.ArgumentParser(description='Process and insert remote sensing image data into the database')
parser.add_argument('--db_config', type=str, required=True, help='dbConfig.json file path')
parser.add_argument('--output_log', type=str, required=True, help='log file path')
args = parser.parse_args()


if __name__ == "__main__":
    print("------------ Program Start ------------\r\n\r\n")
    try:

        #################### 1. 加载配置，验证配置，否则直接退出 ##########################
        # --1 加载DB配置
        DB_CONFIG = load_db_config(args.db_config)

        # --2 验证DB配置
        verify_db_config(DB_CONFIG)

    except Exception as e:
        exit_with_error(f"    \033[91m[ERROR] {e}\033[0m\n    Error details: {traceback.format_exc()}\n  --------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\n\n\033[0m")
    
    try:
        check()
    except Exception as e:
        exit_with_error(f"\033[91m[ERROR] {e}\n Error details: {traceback.format_exc()}\n --------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\033[0m\n\n")