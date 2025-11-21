import math
import os

class BaseConfig:
    APP_PORT                                        =       5000
    APP_DEBUG                                       =       False

    # API Version
    API_VERSION                                     =       '/v0'

    # API for TIF
    API_TIF_MERGE                                   =       API_VERSION + '/tif/merge'
    API_TIF_MERGE_V2                                =       API_VERSION + '/tif/merge_v2'
    API_TIF_calc_no_cloud                           =       API_VERSION + '/tif/calc_no_cloud'
    API_TIF_calc_no_cloud_grid                      =       API_VERSION + '/tif/calc_no_cloud_grid'
    API_TIF_calc_no_cloud_complex                   =       API_VERSION + '/tif/calc_no_cloud_complex'
    API_TIF_calc_NDVI                               =       API_VERSION + '/tif/calc_NDVI'
    API_TIF_get_spectral_profile                    =       API_VERSION + '/tif/get_spectral_profile'
    API_TIF_calc_raster_point                       =       API_VERSION + '/tif/calc_raster_point'
    API_TIF_calc_raster_line                        =       API_VERSION + '/tif/calc_raster_line'
    API_TIF_create_low_level_mosaic                 =       API_VERSION + '/mosaic/create_low_level_mosaic'
    API_TIF_create_low_level_mosaic_threads         =       API_VERSION + '/mosaic/create_low_level_mosaic_threads'

    # API for Task
    API_TASK_STATUS                                 =       API_VERSION + '/task/status'
    API_TASK_RESULT                                 =       API_VERSION + '/task/result'
    API_TASK_CANCEL                                 =       API_VERSION + '/task/cancel'
    
    # API for Mosaic (新增)
    API_MOSAIC_CREATE                               =       API_VERSION + '/mosaic/create'
    API_MOSAIC_STATUS                               =       API_VERSION + '/mosaic/status'
    API_MOSAIC_RESULT                               =       API_VERSION + '/mosaic/result'
    API_MOSAIC_LIST                                 =       API_VERSION + '/mosaic/list'

    # API for MethLib (新增)
    API_METHLIB                                     =       API_VERSION + '/methlib'

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
    
    # Mosaic Task Default Config (新增)
    MOSAIC_DEFAULT_GRID_RES                         =       150
    MOSAIC_DEFAULT_CRS                              =       4326
    MOSAIC_DEFAULT_Z_LEVEL                          =       8
    MOSAIC_DEFAULT_QUADKEY_ZOOM                     =       8
    MOSAIC_DEFAULT_START_TIME                       =       "2024-05-01"
    MOSAIC_DEFAULT_END_TIME                         =       "2025-06-30"
    MOSAIC_DEFAULT_REGION_ID                        =       "100000"
    MOSAIC_DEFAULT_SENSOR_NAME                      =       "GF-1_PMS"

class DevK8SConfig(BaseConfig):
    # MinIO Config
    MINIO_PORT                                      =       30900
    MINIO_IP                                        =       "223.2.34.8"
    MINIO_ACCESS_KEY                                =       "jTbgNHEqQafOpUxVg7Ol"
    MINIO_SECRET_KEY                                =       "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
    MINIO_SECURE                                    =       False
    MINIO_IMAGES_BUCKET                             =       "test-images"
    MINIO_TILES_BUCKET                              =       "test-tiles"
    MINIO_GRID_BUCKET                               =       "test-tiles"
    MINIO_SR_BUCKET                                 =       "temp-files/temp-superResolution"  # Super resolution
    MINIO_TEMP_FILES_BUCKET                         =       "temp-files"
    MINIO_USER_BUCKET                               =       "user"
    MINIO_USER_BASE_PATH                            =       "user-files"

    # MySQL Config
    MYSQL_HOST                                      =       "223.2.34.8"
    MYSQL_TILE_PORT                                 =       30779
    MYSQL_TILE_DB                                   =       "tile"
    MYSQL_RESOURCE_PORT                             =       31036
    MYSQL_RESOURCE_DB                               =       "ard_satellite"
    MYSQL_USER                                      =       "root"
    MYSQL_PWD                                       =       "123456"

    # Backend Config
    BACK_URL_PREFIX                                 =       "http://223.2.34.8:31584/api/"
    LOW_LEVEL_IMAGE_VISUALIZATION                   =       "v3/modeling/example/scenes/visualization"

    # Titiler Config
    TITILER_BASE_URL                                =       "http://223.2.34.8:31800"
    MOSAIC_CREATE_URL                               =       TITILER_BASE_URL + "/mosaic/create"

    TEMP_OUTPUT_DIR                                 =       r"/usr/resource/temp"

    # Limitation for Ray
    RAY_MEMORY                                      =       40 * 1024**3
    RAY_MEMORY_PER_TASK                             =       12 * 1024**3
    RAY_OBJECT_STORE_MEMORY                         =       RAY_MEMORY * 0.3
    RAY_NUM_CPUS                                    =       8
    RAY_SYSTEM_RESERVED_CPU                         =       0.5
    RAY_SYSTEM_RESERVED_MEMORY                      =       2 * 1024**3

    # MethLib Config
    METHOD_WD                                       =       r"/usr/resource/method_library/method_wd"
    METHOD_PD                                       =       r"/usr/resource/method_library/method_pd"

class VmodConfig(BaseConfig):
    # MinIO Config
    MINIO_PORT                                      =       9000
    MINIO_IP                                        =       "172.31.13.42"
    MINIO_ACCESS_KEY                                =       "OGMS"
    MINIO_SECRET_KEY                                =       "ogms250410"
    MINIO_SECURE                                    =       False
    MINIO_IMAGES_BUCKET                             =       "images"
    MINIO_TILES_BUCKET                              =       "tiles"
    MINIO_GRID_BUCKET                               =       "tiles"
    MINIO_SR_BUCKET                                 =       "temp-files/temp-superResolution"  # Super resolution
    MINIO_TEMP_FILES_BUCKET                         =       "temp-files"
    MINIO_USER_BUCKET                               =       "user"
    MINIO_USER_BASE_PATH                            =       "user-files"

    # MySQL Config
    MYSQL_HOST                                      =       "172.31.13.43"
    MYSQL_TILE_PORT                                 =       3306
    MYSQL_TILE_DB                                   =       "tile"
    MYSQL_RESOURCE_PORT                             =       3306
    MYSQL_RESOURCE_DB                               =       "ard_satellite"
    MYSQL_USER                                      =       "root"
    MYSQL_PWD                                       =       "ogms250410"

    # Titiler Config
    TITILER_BASE_URL                                =       "http://172.31.13.21:5050"
    MOSAIC_CREATE_URL                               =       TITILER_BASE_URL + "/mosaic/create"

    # Backend Config
    BACK_URL_PREFIX                                 =       "http://172.31.13.21:8999/api/"
    LOW_LEVEL_IMAGE_VISUALIZATION                   =       "v3/modeling/example/scenes/visualization"

    TEMP_OUTPUT_DIR                                 =       r"/usr/resource/temp"

    # Limitation for Ray
    RAY_MEMORY                                      =       35 * 1024**3
    RAY_MEMORY_PER_TASK                             =       12 * 1024**3
    RAY_OBJECT_STORE_MEMORY                         =       RAY_MEMORY * 0.3
    RAY_NUM_CPUS                                    =       36
    RAY_SYSTEM_RESERVED_CPU                         =       0.5
    RAY_SYSTEM_RESERVED_MEMORY                      =       2 * 1024**3

    # MethLib Config
    METHOD_WD                                       =       r"/usr/resource/method_library/method_wd"
    METHOD_PD                                       =       r"/usr/resource/method_library/method_pd"

class hxfConfig(BaseConfig):
    # MinIO Config
    MINIO_PORT                                      =       30900
    MINIO_IP                                        =       "223.2.34.8"
    MINIO_ACCESS_KEY                                =       "minioadmin"
    MINIO_SECRET_KEY                                =       "minioadmin"
    MINIO_SECURE                                    =       False
    MINIO_IMAGES_BUCKET                             =       "test-images"
    MINIO_TILES_BUCKET                              =       "test-tiles"
    MINIO_GRID_BUCKET                               =       "test-tiles"
    MINIO_TEMP_FILES_BUCKET                         =       "temp-files"
    MINIO_USER_BUCKET                               =       "user"
    MINIO_USER_BASE_PATH                            =       "user-files"

    # MySQL Config
    MYSQL_HOST                                      =       "223.2.43.238"
    MYSQL_TILE_PORT                                 =       3306
    MYSQL_TILE_DB                                   =       "tile"
    MYSQL_RESOURCE_PORT                             =       3306
    MYSQL_RESOURCE_DB                               =       "resource"
    MYSQL_USER                                      =       "root"
    MYSQL_PWD                                       =       "123456"

    # Backend Config
    BACK_URL_PREFIX                                 =       "http://223.2.43.238:8999/api/"
    LOW_LEVEL_IMAGE_VISUALIZATION                   =       "v3/modeling/example/scenes/visualization"

    # Titiler Config
    TITILER_BASE_URL                                =       "http://223.2.43.238:8000"
    MOSAIC_CREATE_URL                               =       TITILER_BASE_URL + "/mosaic/create"

    TEMP_OUTPUT_DIR                                 =       r"D:/code/test"

    # Limitation for Ray
    RAY_MEMORY                                      =       10 * 1024**3
    RAY_MEMORY_PER_TASK                             =       5 * 1024**3
    RAY_OBJECT_STORE_MEMORY                         =       RAY_MEMORY * 0.3
    RAY_NUM_CPUS                                    =       8
    RAY_SYSTEM_RESERVED_CPU                         =       0.5
    RAY_SYSTEM_RESERVED_MEMORY                      =       2 * 1024**3

    # MethLib Config
    METHOD_WD                                       =       r"/Users/paxton/Documents/4_tools/method_wd"
    METHOD_PD                                       =       r"/Users/paxton/Documents/4_tools/method_pd"


class zzwConfig(BaseConfig):
    # MinIO Config
    MINIO_PORT                                      =       30900
    MINIO_IP                                        =       "223.2.34.8"
    MINIO_ACCESS_KEY                                =       "jTbgNHEqQafOpUxVg7Ol"
    MINIO_SECRET_KEY                                =       "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
    MINIO_SECURE                                    =       False
    MINIO_IMAGES_BUCKET                             =       "test-images"
    MINIO_TILES_BUCKET                              =       "test-tiles"
    MINIO_GRID_BUCKET                               =       "test-tiles"
    MINIO_TEMP_FILES_BUCKET                         =       "temp-files"
    MINIO_USER_BUCKET                               =       "user"
    MINIO_USER_BASE_PATH                            =       "user-files"

    # MySQL Config
    MYSQL_HOST                                      =       "223.2.34.8"
    MYSQL_TILE_PORT                                 =       30779
    MYSQL_TILE_DB                                   =       "tile"
    MYSQL_RESOURCE_PORT                             =       31036
    MYSQL_RESOURCE_DB                               =       "satllite"
    MYSQL_USER                                      =       "root"
    MYSQL_PWD                                       =       "123456"

    # Backend Config
    BACK_URL_PREFIX                                 =       "http://223.2.34.8:31584/api/"
    LOW_LEVEL_IMAGE_VISUALIZATION                   =       "v3/modeling/example/scenes/visualization"

    # Titiler Config
    TITILER_BASE_URL                                =       "http://localhost:8000"
    MOSAIC_CREATE_URL                               =       TITILER_BASE_URL + "/mosaic/create"

    TEMP_OUTPUT_DIR                                 =       r"/Users/paxton/Documents/1_projects/2025-03_satellite/satellite-analysis/modelServer/temp"

    # Limitation for Ray
    RAY_MEMORY                                      =       40 * 1024**3
    RAY_MEMORY_PER_TASK                             =       5 * 1024**3
    RAY_OBJECT_STORE_MEMORY                         =       RAY_MEMORY * 0.3
    RAY_NUM_CPUS                                    =       8
    RAY_SYSTEM_RESERVED_CPU                         =       0.5
    RAY_SYSTEM_RESERVED_MEMORY                      =       2 * 1024**3

    # MethLib Config
    METHOD_WD                                       =       r"/Users/paxton/Documents/4_tools/method_wd"
    METHOD_PD                                       =       r"/Users/paxton/Documents/4_tools/method_pd"


class slkConfig(BaseConfig):
    # MinIO Config
    MINIO_PORT                                      =       30900
    MINIO_IP                                        =       "223.2.34.8"
    MINIO_ACCESS_KEY                                =       "jTbgNHEqQafOpUxVg7Ol"
    MINIO_SECRET_KEY                                =       "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
    MINIO_SECURE                                    =       False
    MINIO_IMAGES_BUCKET                             =       "test-images"
    MINIO_TILES_BUCKET                              =       "test-tiles"
    MINIO_GRID_BUCKET                               =       "test-tiles"
    MINIO_TEMP_FILES_BUCKET                         =       "temp-files"
    MINIO_USER_BUCKET                               =       "user"
    MINIO_USER_BASE_PATH                            =       "user-files"

    # MySQL Config
    MYSQL_HOST                                      =       "223.2.34.8"
    MYSQL_TILE_PORT                                 =       30779
    MYSQL_TILE_DB                                   =       "tile"
    MYSQL_RESOURCE_PORT                             =       31036
    MYSQL_RESOURCE_DB                               =       "satllite"
    MYSQL_USER                                      =       "root"
    MYSQL_PWD                                       =       "123456"

    # Backend Config
    BACK_URL_PREFIX                                 =       "http://223.2.34.8:31584/api/"
    LOW_LEVEL_IMAGE_VISUALIZATION                   =       "v3/modeling/example/scenes/visualization"

    # Titiler Config
    TITILER_BASE_URL                                =       "http://223.2.34.8:31800"
    MOSAIC_CREATE_URL                               =       TITILER_BASE_URL + "/mosaic/create"

    TEMP_OUTPUT_DIR                                 =       r"/Users/paxton/Documents/1_projects/2025-03_satellite/satellite-analysis/modelServer/temp"

    # Limitation for Ray
    RAY_MEMORY                                      =       40 * 1024**3
    RAY_MEMORY_PER_TASK                             =       5 * 1024**3
    RAY_OBJECT_STORE_MEMORY                         =       RAY_MEMORY * 0.3
    RAY_NUM_CPUS                                    =       8
    RAY_SYSTEM_RESERVED_CPU                         =       0.5
    RAY_SYSTEM_RESERVED_MEMORY                      =       2 * 1024**3

    # MethLib Config
    METHOD_WD                                       =       r"/Users/paxton/Documents/4_tools/method_wd"
    METHOD_PD                                       =       r"/Users/paxton/Documents/4_tools/method_pd"

# --------------- class ProdConfig(BaseConfig): ---------------

# 配置映射字典 - 类似Spring Boot的profile机制
config = {
    'k8s': DevK8SConfig,
    'vmod':VmodConfig,
    'hxf':hxfConfig,
    'zzw':zzwConfig,
    'slk':slkConfig
}

def get_config(profile=None):
    """获取配置类，如果profile为None则使用k8s配置"""
    if profile is None:
        profile = 'k8s'
    
    # 更安全的方式，避免KeyError
    default_config = config.get('k8s', DevK8SConfig)
    return config.get(profile, default_config)

# 获取配置类
def get_current_config():
    """获取当前环境的配置类"""
    return get_config(CURRENT_PROFILE)

os.environ['APP_PROFILE'] = 'slk'
# 获取当前环境配置 - 类似Spring Boot的 spring.profiles.active
CURRENT_PROFILE = os.getenv('APP_PROFILE', 'k8s')  # 默认使用k8s

# 创建全局配置实例 - 这是关键
CUR_CONFIG_CLASS = get_current_config()
current_config = CUR_CONFIG_CLASS()  # 实例化配置类