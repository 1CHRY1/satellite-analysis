import json
from TransferEngine.application.provider import (
    init_minio, init_satellite_database, init_tile_database, init_services, init_project_info
)


class TransferEngine:

    @classmethod
    def initialize(cls, config_file_path: str):
        with open(config_file_path, 'r') as f:
            config = json.load(f)
        
        minio_config = config["minio"]
        database_config = config["database"]    

        init_project_info(config["project_info"]["project_id"], config["project_info"]["user_id"], config["project_info"]["bucket"])
        init_minio(minio_config["endpoint"], minio_config["access_key"], minio_config["secret_key"], minio_config["secure"])
        init_satellite_database(database_config["endpoint"], database_config["user"], database_config["password"], database_config["satellite_database"])
        init_tile_database(database_config["endpoint"], database_config["user"], database_config["password"], database_config["tile_database"])
        init_services()

    @classmethod
    def Sensor(cls, sensor_id: str):
        from TransferEngine.application.sensor import Sensor
        return Sensor(sensor_id)

    @classmethod
    def Product(cls, product_id: str):
        from TransferEngine.application.product import Product
        return Product(product_id)
    
    @classmethod
    def Scene(cls, scene_id: str):
        from TransferEngine.application.scene import Scene
        return Scene(scene_id)
    
    @classmethod
    def Image(cls, image_id: str):
        from TransferEngine.application.image import Image
        return Image(image_id)
    
    @classmethod
    def Tile(cls, scene_id: str, tile_id: str):
        from TransferEngine.application.tile import Tile
        return Tile(scene_id, tile_id)
    
    @classmethod
    def ProjectDataTransfer(cls):
        from TransferEngine.application.export import ProjectDataTransfer
        return ProjectDataTransfer
