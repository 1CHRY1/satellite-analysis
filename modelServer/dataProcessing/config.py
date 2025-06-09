import math
import os

class BaseConfig:
    APP_PORT                                        =       5001
    APP_DEBUG                                       =       False

    # API Version
    API_VERSION                                     =       '/v0'

    # API for TIF
    API_TIF_MERGE                                   =       API_VERSION + '/tif/merge'
    API_TIF_MERGE_V2                                =       API_VERSION + '/tif/merge_v2'
    API_TIF_calc_no_cloud                           =       API_VERSION + '/tif/calc_no_cloud'
    API_TIF_calc_no_cloud_grid                      =       API_VERSION + '/tif/calc_no_cloud_grid'
    API_TIF_calc_NDVI                               =       API_VERSION + '/tif/calc_NDVI'
    API_TIF_get_spectral_profile                    =       API_VERSION + '/tif/get_spectral_profile'
    API_TIF_calc_raster_point                       =       API_VERSION + '/tif/calc_raster_point'
    API_TIF_calc_raster_line                        =       API_VERSION + '/tif/calc_raster_line'

    # API for Task
    API_TASK_STATUS                                 =       API_VERSION + '/task/status'
    API_TASK_RESULT                                 =       API_VERSION + '/task/result'

    # Status Flag
    STATUS_UNLOCK                                   =       0b1
    STATUS_LOCK                                     =       0b10
    STATUS_RUNNING                                  =       0b100
    STATUS_COMPLETE                                 =       0b1000
    STATUS_PENDING                                  =       0b10000
    STATUS_ERROR                                    =       0b100000
    STATUS_DELETE                                   =       0b1000000

    # Data General Config
    EARTH_RADIUS                                    =       6371008.8
    EARTH_CIRCUMFERENCE                             =       2 * math.pi * EARTH_RADIUS
    GRID_RESOLUTION                                 =       1
    MAX_RUNNING_TASKS                               =       200

class DevSlkConfig(BaseConfig):
    # MinIO Config
    MINIO_PORT                                      =       30900
    MINIO_IP                                        =       "223.2.43.228"
    MINIO_ACCESS_KEY                                =       "jTbgNHEqQafOpUxVg7Ol"
    MINIO_SECRET_KEY                                =       "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
    MINIO_SECURE                                    =       False
    MINIO_IMAGES_BUCKET                             =       "test-images"
    MINIO_TILES_BUCKET                              =       "test-tiles"
    MINIO_GRID_BUCKET                               =       "test-tiles"
    MINIO_TEMP_FILES_BUCKET                         =       "temp-files"

    # MySQL Config
    MYSQL_HOST                                      =       "223.2.43.228"
    MYSQL_TILE_PORT                                 =       30779
    MYSQL_TILE_DB                                   =       "tile"
    MYSQL_RESOURCE_PORT                             =       30778
    MYSQL_RESOURCE_DB                               =       "resource"
    MYSQL_USER                                      =       "root"
    MYSQL_PWD                                       =       "123456"

    # Titiler Config
    TITILER_BASE_URL                                =       "http://223.2.43.228:31800"
    MOSAIC_CREATE_URL                               =       TITILER_BASE_URL + "/mosaic/create"

    # Gdal_Config
    GDAL_PROJ_LIB                                   =       r"F:\App\anaconda3\envs\bankModel\Library\share"

    TEMP_OUTPUT_DIR                                 =       r"/usr/resource/temp"

# --------------- class ProdConfig(BaseConfig): ---------------

# 配置映射字典 - 类似Spring Boot的profile机制
config = {
    'dev_slk': DevSlkConfig,
}

def get_config(profile=None):
    """获取配置类，如果profile为None则使用开发配置"""
    if profile is None:
        profile = 'dev_slk'
    
    return config.get(profile, config['dev_slk'])

# 获取当前环境配置 - 类似Spring Boot的 spring.profiles.active
CURRENT_PROFILE = os.getenv('APP_PROFILE', 'dev_slk')  # 默认使用dev_slk

# 获取配置类
def get_current_config():
    """获取当前环境的配置类"""
    return get_config(CURRENT_PROFILE)

# 创建全局配置实例 - 这是关键
CUR_CONFIG_CLASS = get_current_config()
current_config = CUR_CONFIG_CLASS()  # 实例化配置类