import math
import os
import weakref

APP_PORT                                        =       5000
APP_DEBUG                                       =       True

# API Version
API_VERSION                                     =       '/v0'

# API for TIF
API_TIF_MERGE                                   =       API_VERSION + '/tif/merge'

# API for Model Case
API_MC_DELETE                                   =       API_VERSION + '/mc'
API_MC_ERROR                                    =       API_VERSION + '/mc/error'
API_MC_STATUS                                   =       API_VERSION + '/mc/status'
API_MC_RESULT                                   =       API_VERSION + '/mc/result'
API_MC_PRE_ERROR_CASES                          =       API_VERSION + '/mc/pre-error-cases'

# Status Flag
STATUS_UNLOCK                                   =       0b1
STATUS_LOCK                                     =       0b10
STATUS_RUNNING                                  =       0b100
STATUS_COMPLETE                                 =       0b1000
STATUS_NONE                                     =       0b10000
STATUS_ERROR                                    =       0b100000
STATUS_DELETE                                   =       0b1000000

# MinIO Config
MINIO_PORT                                      =       9000
MINIO_IP                                        =       "223.2.34.7"
MINIO_ACCESS_KEY                                =       "jTbgNHEqQafOpUxVg7Ol"
MINIO_SECRET_KEY                                =       "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
MINIO_SECURE                                    =       False
MINIO_IMAGES_BUCKET                             =       "test-images"
MINIO_TILES_BUCKET                              =       "test-tiles"
MINIO_GRID_BUCKET                               =       "test-tiles"
MINIO_TEMP_FILES_BUCKET                         =       "temp-files"

# Temp Config
TEMP_INPUT_DIR                                  =       r"E:\Landset8_test\LC08_L2SP_118038_20240320_20240402_02_T1"
TEMP_OUTPUT_DIR                                 =       r"E:\Landset8_test\test"
TEMP_SENSOR_NAME                                =       "landset8_test"
TEMP_PRODUCT_NAME                               =       "landset8_L2SP_test"
TEMP_CLOUD                                      =       0

# Gdal_Config
GDAL_PROJ_LIB                                   =       r"C:\Users\lkshi\.conda\envs\Python312\Library\share\proj"

# Data General Config
EARTH_RADIUS                                    =       6371008.8
EARTH_CIRCUMFERENCE                             =       2 * math.pi * EARTH_RADIUS
GRID_RESOLUTION                                 =       50
