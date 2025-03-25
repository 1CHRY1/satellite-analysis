from application.engine import TransferEngine
from application.engine import TransferEngineConfig
import config

if __name__ == "__main__":
    
    config = TransferEngineConfig(
        minio={
            "endpoint": config.MINIO_ENDPOINT,
            "access_key": config.MINIO_ACCESS_KEY,
            "secret_key": config.MINIO_SECRET_KEY,
            "secure": config.MINIO_SECURE
        },
        database={
            "host": config.MYSQL_HOST,
            "user": config.MYSQL_USER,
            "password": config.MYSQL_PASSWORD,
            "satellite_database": config.MYSQL_DATABASE,
            "tile_database": config.MYSQL_TILE_DATABASE
        },
        project_info={
            "project_id": "P1234567890",
            "user_id": "U1234567890",
            "bucket": "project-data-bucket"
        }
    )
    TransferEngine.initialize(config)

    # Testing
    sensor = TransferEngine.Sensor("S1234567890")
    if sensor is not None:
        print(sensor)
    else:
        print("传感器不存在")

    sensor = TransferEngine.Sensor("SE15303")
    products = sensor.get_all_products()
    scenes = products[0].get_all_scenes()
    images = scenes[0].get_all_band_images()
    #images[0].pull("D:\\t\\1\\1.tif")
    print('testing object ending')
    
    target_scene = TransferEngine.Scene("SC153032082")
    image = target_scene.get_all_band_images()[0]
    
    # s_tile = TransferEngine.Tile("SC906772444", "00014d8a-9702-4407-9437-d9e017b2c287")
    # s_tile.pull("D:\\t\\1\\s_tile.tif")

    tiles1 = target_scene.get_tiles_by_cloud(99.5)
    print(len(tiles1))
    
    # tiles2 = image.get_all_tiles()
    # print(len(tiles2))
    
    # tile_ids = []
    # for i in range(10):
    #     tile_ids.append(tiles2[i].tile_id)
    # image.pull_tiles_by_ids(tile_ids, "D:\\t\\2")
    
    print(' --------------------------------- ')
    print('great!')