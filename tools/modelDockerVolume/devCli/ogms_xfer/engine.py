import json
from .application.provider import (
    init_minio, init_satellite_database, init_services, init_project_info
)

class OGMS_Xfer:

    @classmethod
    def initialize(cls, config_file_path: str):
        with open(config_file_path, 'r') as f:
            config = json.load(f)
        
        minio_config = config["minio"]
        database_config = config["database"]    

        init_project_info(config["project_info"]["project_id"], config["project_info"]["user_id"], config["project_info"]["bucket"])
        init_minio(minio_config["endpoint"], minio_config["access_key"], minio_config["secret_key"], minio_config["secure"])
        init_satellite_database(database_config["endpoint"], database_config["user"], database_config["password"], database_config["satellite_database"])
        init_services()

    @classmethod
    def Sensor(cls, sensor_id: str = None):
        from .application.sensor import Sensor
        return Sensor(sensor_id) if sensor_id is not None else Sensor

    @classmethod
    def Product(cls, product_id: str = None):
        from .application.product import Product
        return Product(product_id) if product_id is not None else Product
    
    @classmethod
    def Scene(cls, scene_id: str = None):
        from .application.scene import Scene
        return Scene(scene_id) if scene_id is not None else Scene
    
    @classmethod
    def Image(cls, image_id: str = None):
        from .application.image import Image
        return Image(image_id) if image_id is not None else Image

    from .application import toolbox, tileUtil
    Toolbox = toolbox
    TileUtil = tileUtil

    from .application.urlutil import URLUtil
    URL = URLUtil
    
    from . import modelStore
    ModelStore = modelStore