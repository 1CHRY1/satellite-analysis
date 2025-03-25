from typing import TypedDict
from application.provider import init_minio, init_satellite_database, init_tile_database, init_services, init_project_info


class MinioConfig(TypedDict):
    endpoint: str
    access_key: str
    secret_key: str
    secure: bool

class DatabaseConfig(TypedDict):
    host: str
    user: str
    password: str
    satellite_database: str
    tile_database: str
    
class ProjectInfoConfig(TypedDict):
    project_id: str
    user_id: str
    bucket: str
    
class TransferEngineConfig(TypedDict):
    minio: MinioConfig
    database: DatabaseConfig
    project_info: ProjectInfoConfig

class TransferEngine:

    @classmethod
    def initialize(cls, config: TransferEngineConfig):
        
        minio_config = config["minio"]
        database_config = config["database"]    

        init_project_info(config["project_info"]["project_id"], config["project_info"]["user_id"], config["project_info"]["bucket"])
        init_minio(minio_config["endpoint"], minio_config["access_key"], minio_config["secret_key"], minio_config["secure"])
        init_satellite_database(database_config["host"], database_config["user"], database_config["password"], database_config["satellite_database"])
        init_tile_database(database_config["host"], database_config["user"], database_config["password"], database_config["tile_database"])
        init_services()

    @classmethod
    def Sensor(cls, sensor_id: str):
        from application.sensor import Sensor
        return Sensor(sensor_id)

    @classmethod
    def Product(cls, product_id: str):
        from application.product import Product
        return Product(product_id)
    
    @classmethod
    def Scene(cls, scene_id: str):
        from application.scene import Scene
        return Scene(scene_id)
    
    @classmethod
    def Image(cls, image_id: str):
        from application.image import Image
        return Image(image_id)
    
    @classmethod
    def Tile(cls, scene_id: str, tile_id: str):
        from application.tile import Tile
        return Tile(scene_id, tile_id)
    
    @classmethod
    def ProjectDataTransfer(cls):
        from application.export import ProjectDataTransfer
        return ProjectDataTransfer
