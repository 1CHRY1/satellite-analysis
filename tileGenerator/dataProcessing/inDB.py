from Utils.mySqlUtils import *
from Utils.tifUtils import *
from Utils.geometryUtils import *
from Utils.osUtils import *
from dataProcessing.Utils.filenameUtils import get_files
import config
from dataProcessing.imageConfig import CUR_TILE_LEVEL

os.environ['PROJ_LIB'] = config.GDAL_PROJ_LIB
object_prefix = f'{config.TEMP_SENSOR_NAME}/{config.TEMP_PRODUCT_NAME}'  # 上传路径前缀

def insert_to_db(scene_info_list, sensor_name, product_name):
    # insert sensor/product/scene/image/tile into db in order
    if get_sensor_byName(sensor_name) is None:
        insert_sensor(sensor_name, sensor_name, None)
    # if scene exists
    if (len(scene_info_list) > 0):
        resolution = scene_info_list[0].resolution
        period = scene_info_list[0].period
        # insert product
        if get_product_byName(sensor_name, product_name) is None:
            insert_product(sensor_name, product_name, None, resolution, period)
        for scene_info in scene_info_list:
            # insert scene
            sceneId = insert_scene(sensor_name, product_name, scene_info.scene_name, scene_info.image_time, scene_info.tile_level_num, scene_info.tile_levels, scene_info.png_path,
                                   scene_info.crs, scene_info.bbox, None, scene_info.bands, scene_info.band_num, config.MINIO_IMAGES_BUCKET, scene_info.cloud)
            image_info_list = scene_info.image_info_list
            # create a tile table for this image(tiles are stored in tile db)
            create_tile_table(sceneId)
            for info in image_info_list:
                # insert image
                image_id = insert_image(sceneId, info.tif_path, info.band, config.MINIO_IMAGES_BUCKET, info.cloud)
                # insert tiles
                insert_batch_tile(sceneId, image_id, CUR_TILE_LEVEL, info.tile_info_list, info.band)

if __name__ == "__main__":
    landset_files = get_files(config.TEMP_INPUT_DIR)
    # scene_info_list包含了景、波段、瓦片信息
    scene_info_list = process_and_upload(landset_files, config.MINIO_IMAGES_BUCKET, object_prefix)
    insert_to_db(scene_info_list, config.TEMP_SENSOR_NAME, config.TEMP_PRODUCT_NAME)
