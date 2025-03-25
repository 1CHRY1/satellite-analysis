# SDK Server
APP_PORT                                        =       8000
APP_DEBUG                                       =       True

# MySQL Config
MYSQL_HOST                                      =       "223.2.34.7"
MYSQL_USER                                      =       "root"
MYSQL_PASSWORD                                  =       "root"
MYSQL_DATABASE                                  =       "satellite"
MYSQL_TILE_DATABASE                             =       "tile"

# MinIO Config
MINIO_PORT                                      =       9000
MINIO_IP                                        =       "223.2.34.7"
MINIO_ENDPOINT                                  =       f"{MINIO_IP}:{MINIO_PORT}"
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