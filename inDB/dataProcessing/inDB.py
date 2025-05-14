import sys, os
from pathlib import Path

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



## Main ##############################################################################################
from dataProcessing.Utils.mySqlUtils import *
from dataProcessing.Utils.tifUtils import *
from dataProcessing.Utils.minioUtil import *
from dataProcessing.Utils.extractor import main, set_initial_config
import argparse
import json
import sys

CUR_TILE_LEVEL                                  =       '40031*20016'

def insert_to_db(scene_info, scene_conf, db_conf):
    sensor_name, product_name = scene_conf["CUR_SENSOR_NAME"], scene_conf["CUR_PRODUCT_NAME"]
    # insert sensor/product/scene/image into db in order
    if get_sensor_byName(sensor_name) is None:
        insert_sensor(sensor_name, sensor_name, None)
    resolution = scene_info["resolution"]
    period = scene_info["period"]
    # insert product
    if get_product_byName(sensor_name, product_name) is None:
        insert_product(sensor_name, product_name, None, resolution, period)
    # insert scene
    sceneId = insert_scene(sensor_name, product_name, scene_info["scene_name"], scene_info["image_time"], scene_info["tile_level_num"], scene_info["tile_levels"], scene_info["cloud_path"],
                            scene_info["crs"], scene_info["bbox"], None, scene_info["bands"], scene_info["band_num"], db_conf["MINIO_IMAGES_BUCKET"], scene_info["cloud"], scene_info["tags"])
    image_info_list = scene_info["image_info_list"]
    for info in image_info_list:
        # insert image
        image_id = insert_image(sceneId, info["tif_path"], info["band"], db_conf["MINIO_IMAGES_BUCKET"], info["cloud"])


# 命令行参数
parser = argparse.ArgumentParser(description='处理遥感影像数据并入库')
parser.add_argument('--db_config', type=str, required=True, help='数据库配置文件路径')
parser.add_argument('--scene_config', type=str, required=True, help='影像文件路径')
args = parser.parse_args()


if __name__ == "__main__":
    DB_CONFIG = {}
    SCENE_CONFIG = {}
    
    # 加载数据库配置, minio 之类的
    json_db_config = json.load(open(args.db_config, 'r',encoding="UTF-8"))
    for key, value in json_db_config.items():
        DB_CONFIG[key] = value
    
    # 加载场景配置, 传感器, 产品, 分辨率, 周期
    json_scene_config = json.load(open(args.scene_config, 'r',encoding="UTF-8"))
    for key, value in json_scene_config.items():
        SCENE_CONFIG[key] = value

    # 上传路径前缀    
    object_prefix = f'{SCENE_CONFIG["CUR_SENSOR_NAME"]}/{SCENE_CONFIG["CUR_PRODUCT_NAME"]}'
    
    print("程序开始执行")
    print("设定传感器：", SCENE_CONFIG['CUR_SENSOR_NAME'])
    print("设定产品：", SCENE_CONFIG['CUR_PRODUCT_NAME'])
    print("设定分辨率：", SCENE_CONFIG['CUR_RESOLUTION'])
    print("设定周期：", SCENE_CONFIG['CUR_PERIOD'])

    print("初始化配置...")
    set_initial_config(SCENE_CONFIG, DB_CONFIG)
    set_initial_mysql_config(DB_CONFIG)
    set_initial_minio_config(DB_CONFIG)
    print("初始化配置完成，正在验证配置")

    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    if cursor is None:
        print("数据库连接失败")
        sys.exit(1)
    try:
        client = getMinioClient()
        buckets = client.list_buckets()
    except Exception as e:
        print(f"MinIO连接失败：{e}")
        sys.exit(1)
    print("验证成功")

    start_time = datetime.now()
    scene_info = main(object_prefix)
    print(f"正在将 影像信息 写入数据库")
    insert_to_db(scene_info, SCENE_CONFIG, DB_CONFIG)
    print(f"共花时间{datetime.now() - start_time}")