import sys, os
from pathlib import Path
import ssl

## 环境变量和文件模块 ##################################################################################
# nuitka 获取程序所在路径（对于打包的 .exe 文件）
if getattr(sys, 'frozen', False):
    base_path = os.path.dirname(sys.argv[0])
else:
    base_path = os.path.dirname(__file__)
os.environ['PROJ_LIB'] = base_path
print("os.environ['PROJ_LIB'] -- ", os.environ['PROJ_LIB'])

# 获取当前文件的绝对路径，并向上定位到项目根目录（tileGenerator）
project_root = Path(__file__).parent.parent  # 假设 inDB.py 在 dataProcessing/ 下
sys.path.append(str(project_root))  # 添加进 Python 路径


## 潜在依赖显式引入 ###################################################################################
import rasterio.sample
import rasterio._features
import morecantile
import _cffi_backend
## 此处注意替换本地环境中的两个文件，一个是morecantile，一个是certifi，QQ群里有相应文件


## Main ##############################################################################################
from dataProcessing.Utils.mySqlUtils import *
from dataProcessing.Utils.tifUtils import *
from dataProcessing.Utils.minioUtil import *
from dataProcessing.Utils.extractor import main, set_initial_config
from dataProcessing.Utils.batchWrapper import *
import argparse
import json
import sys
import traceback

CUR_TILE_LEVEL                                  =       '40031*20016'

def insert_to_db(scene_info, scene_conf, db_conf):
    sensor_name, product_name = scene_conf["CUR_SENSOR_NAME"], scene_conf["CUR_PRODUCT_NAME"]
    # insert sensor/product/scene/image into db in order
    if get_sensor_byName(sensor_name) is None:
        result_flag = insert_sensor(sensor_name, sensor_name, None)
        if result_flag is False:
            return False
    resolution = scene_info["resolution"]
    period = scene_info["period"]
    # insert product
    if get_product_byName(sensor_name, product_name) is None:
        result_flag = insert_product(sensor_name, product_name, None, resolution, period)
        if result_flag is False:
            return False
    # insert scene
    sceneId = insert_scene(sensor_name, product_name, scene_info["scene_name"], scene_info["image_time"], scene_info["tile_level_num"], scene_info["tile_levels"], scene_info["cloud_path"],
                            scene_info["crs"], scene_info["bbox"], None, scene_info["bands"], scene_info["band_num"], db_conf["MINIO_IMAGES_BUCKET"], scene_info["cloud"], scene_info["tags"], scene_info["no_data"])
    if sceneId is None:
        return False
    image_info_list = scene_info["image_info_list"]
    for info in image_info_list:
        # insert image
        image_id = insert_image(sceneId, info["tif_path"], info["band"], db_conf["MINIO_IMAGES_BUCKET"], info["cloud"])
        if image_id is None:
            return False
    return True

def check_if_scene_exists(scene_name):
    scene_id = get_scene_byName(scene_name)
    if scene_id is not None:
        return True
    else:
        return False
    
    

def batch_insert(PRODUCT_CONFIGS, DB_CONFIG):
    for i, PRODUCT_CONFIG in enumerate(PRODUCT_CONFIGS):
        print(f"========== [{PRODUCT_CONFIG['CUR_SENSOR_NAME']}] Insert Start ==========")
        append_to_log(args.output_log, f"========== [{PRODUCT_CONFIG['CUR_SENSOR_NAME']}] ==========\n")
        for j, SCENE_CONFIG in enumerate(PRODUCT_CONFIG["SCENE_CONFIGS"]):
            status = 'NotStarted'
            err_info = ""
            start_time = datetime.now()
            print(f"  [Scene {j+1}] {SCENE_CONFIG['SCENE_PATH']}")
            append_to_log(args.output_log, f"[Scene {j+1}] {SCENE_CONFIG['SCENE_PATH']}\n")
            try:
                # 获取scene name
                scene_name = ""
                if isinstance(SCENE_CONFIG["SCENE_PATH"], list):
                    # 单波段
                    if len(SCENE_CONFIG["SCENE_PATH"]) == 0:
                        exit_with_error(f"No scenes found in {SCENE_CONFIG["SCENE_PATH"]}")
                    file_path = SCENE_CONFIG["SCENE_PATH"][0]["path"]
                    
                    parent_dir = os.path.dirname(file_path)  # "D:/CODE/BANDS"
                    grandparent_dir = os.path.dirname(parent_dir)  # "D:/CODE"
                    grandparent_name = os.path.basename(grandparent_dir)  # "CODE"
                    scene_name = grandparent_name
                else:
                    # 多波段
                    scene_name = os.path.basename(os.path.dirname(SCENE_CONFIG["SCENE_PATH"]))

                if check_if_scene_exists(scene_name):
                    status = 'NotStarted'
                    print(f"    [NOT STARTED] Scene {j+1} already exists, skipping...")
                    err_info += f"[ERROR] Scene {scene_name} already exists\n"
                    continue

                object_prefix = f'{SCENE_CONFIG["CUR_SENSOR_NAME"]}/{SCENE_CONFIG["CUR_PRODUCT_NAME"]}'
                set_initial_config(SCENE_CONFIG, DB_CONFIG)
                try:
                    scene_info = main(object_prefix)
                    if scene_info is None:
                        status = 'Error'
                        err_info += "[ERROR] main() returned empty band list\n"
                        continue
                except Exception as e:
                    status = 'Error'
                    print(f"    \033[91m[ERROR] main() failed: {e}\033[0m")
                    print(f"    \033[91mError details: {traceback.format_exc()}\033[0m")
                    print("\033[91m--------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\033[0m\r\n\r\n")
                    err_info += f"[ERROR] main() failed: {e}\n"
                    err_info += f"[Traceback]\n{traceback.format_exc()}\n"
                    err_info += "--------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\r\n\r\n"
                    continue

                print(f"    [INFO] Writing to database...")
                try:
                    flag = insert_to_db(scene_info, SCENE_CONFIG, DB_CONFIG)
                    if flag is False:
                        status = 'Error'
                        err_info += "[ERROR] insert_to_db() failed\n[WARNING] Data may have been partially written, please check manually.\n"
                        continue
                    else:
                        status = 'Success'
                except Exception as e:
                    status = 'Error'
                    print(f"    \033[91m[ERROR] insert_to_db() failed: {e}\033[0m")
                    print(f"    \033[91mError details: {traceback.format_exc()}\033[0m")
                    print(f"    \033[91m[WARNING] Data may have been partially written, please check manually.\033[0m")
                    err_info += f"[ERROR] insert_to_db() failed: {e}\n"
                    err_info += f"[Traceback]\n{traceback.format_exc()}\n"
                    err_info += "[WARNING] Data may have been partially written, please check manually.\n"
                    continue
            except Exception as e:
                status = 'Error'
                print(f"    \033[91m[EXCEPTION] Unexpected error: {e}\033[0m")
                print(f"    \033[91mError details: {traceback.format_exc()}\033[0m")
                print("\033[91m--------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\033[0m\r\n\r\n")
                err_info += f"[EXCEPTION] Unexpected error: {e}\n"
                err_info += f"[Traceback]\n{traceback.format_exc()}\n"
                err_info += "--------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\r\n\r\n"
                continue
            finally:
                duration = datetime.now() - start_time
                print(f"    [Duration] {duration}")
                if status == 'Success':
                    print(f"    [SUCCESS] Scene {j+1} completed, {SCENE_CONFIG['SCENE_PATH']}")
                    append_to_log(args.output_log, f"[SUCCESS] Scene {j+1}, {SCENE_CONFIG['SCENE_PATH']}\n")
                elif status == 'Error':
                    print(f"    [FAILED] Scene {j+1} failed, {SCENE_CONFIG['SCENE_PATH']}")
                    append_to_log(args.output_log, f"[FAILED] Scene {j+1}, {SCENE_CONFIG['SCENE_PATH']}\n{err_info}")
                else:
                    print(f"    [NOT STARTED] Scene {j+1} not processed, program exited unexpectedly")
                    append_to_log(args.output_log, f"[NOT STARTED] Scene {j+1}, {SCENE_CONFIG['SCENE_PATH']}, program exited unexpectedly\n")
        print("========== Batch Insert End for this product ==========\n")
        append_to_log(args.output_log, "========== Batch Insert End for this product ==========\n\n")

# 命令行参数
parser = argparse.ArgumentParser(description='Process and insert remote sensing image data into the database')
parser.add_argument('--db_config', type=str, required=True, help='dbConfig.json file path')
parser.add_argument('--product_config', type=str, required=True, nargs='+', help='multiple productConfig.json file path')
parser.add_argument('--scenes_dir', type=str, required=True, help='scenes directory path')
parser.add_argument('--output_log', type=str, required=True, help='log file path')
args = parser.parse_args()


if __name__ == "__main__":
    print("------------ Program Start ------------\r\n\r\n")
    try:
        # 清除日志文件内容（如果存在），否则创建新文件
        try:
            with open(args.output_log, 'w', encoding='utf-8') as f:
                pass  # 直接清空文件内容
        except Exception as e:
            print(f"\033[91m[ERROR] Unable to clear log file: {e}\033[0m")
            sys.exit(1)

        #################### 1. 加载配置，验证配置，否则直接退出 ##########################
        # --1 加载DB配置
        DB_CONFIG = load_db_config(args.db_config)

        # --2 验证DB配置
        verify_db_config(DB_CONFIG)

        # --3 加载Product配置
        PRODUCT_CONFIGS = load_product_configs(args.product_config)
    except Exception as e:
        append_to_log(args.output_log, f"[Warning] Error happened in loading or verifying config, the data has not been written to the database.\n")
        append_to_log(args.output_log, f"[ERROR] {e}\n")
        append_to_log(args.output_log, f"[Traceback]\n{traceback.format_exc()}\n")
        exit_with_error(f"    \033[91m[ERROR] {e}\033[0m\n    Error details: {traceback.format_exc()}\n  --------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\n\n\033[0m")
    
    try:
        ################### 2. 依次每种产品下的景列表文件夹，加载验证XML #######################
        for i, PRODUCT_CONFIG in enumerate(PRODUCT_CONFIGS):
            PRODUCT_CONFIGS[i]["SCENE_CONFIGS"] = process_folder(PRODUCT_CONFIG, args.scenes_dir)
    except Exception as e:
        append_to_log(args.output_log, f"[Warning] Error happened in processing scene folders, the data has not been written to the database.\n")
        append_to_log(args.output_log, f"[ERROR] {e}\n")
        append_to_log(args.output_log, f"[Traceback]\n{traceback.format_exc()}\n")
        exit_with_error(f"\033[91m[ERROR] {e}\n Error details: {traceback.format_exc()}\n --------- !!!!!!!!!!!!!!!!!!!!!!!!!!! -------------\033[0m\n\n")
    #################### 3. 批量入库，适时反馈 ######################################
    batch_insert(PRODUCT_CONFIGS, DB_CONFIG)