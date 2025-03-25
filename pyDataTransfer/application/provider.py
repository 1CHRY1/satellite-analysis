from connection.database import DatabaseClient
from connection.minio import MinioClient
from sqlalchemy.orm import scoped_session, sessionmaker

from service.project_data import ProjectDataService
from service.sensor import SensorService
from service.image import ImageService
from service.product import ProductService
from service.scene import SceneService
from service.tile import TileService

class Singleton:
    _instances = {}
    _values = {}
    @classmethod
    def get_instance(cls, id, class_type=None, *args, **kwargs):
        if id not in cls._instances:
            cls._instances[id] = class_type(*args, **kwargs)
        return cls._instances[id]
    
    @classmethod
    def set_value(cls, id, value):
        cls._values[id] = value
    
    @classmethod
    def get_value(cls, id):
        return cls._values[id]


def init_satellite_database(host: str, user: str, password: str, database: str):

    return Singleton.get_instance(
        id="satellite-database",
        class_type=DatabaseClient,
        host=host, user=user, 
        password=password, database=database)


def init_tile_database(host: str, user: str, password: str, database: str):

    return Singleton.get_instance(
        id="tile-database",
        class_type=DatabaseClient,
        host=host, user=user, 
        password=password, database=database)
   

def init_minio(endpoint: str, access_key: str, secret_key: str, secure: bool):

    return Singleton.get_instance(
        id="minio",
        class_type=MinioClient,
        endpoint=endpoint, access_key=access_key, secret_key=secret_key, secure=secure)


def init_project_info(project_id: str, user_id: str, bucket: str):

    Singleton.set_value(id="project_id", value=project_id)
    Singleton.set_value(id="user_id", value=user_id)
    Singleton.set_value(id="project_bucket", value=bucket)


def init_services():
    
    minio_client = Singleton.get_instance(id="minio", class_type=MinioClient)
    satellite_db_client = Singleton.get_instance(id="satellite-database", class_type=DatabaseClient)
    tile_db_client = Singleton.get_instance(id="tile-database", class_type=DatabaseClient)
    
    # # 这里获取一个 session，并确保多个 Service 共享同一个 session
    # satellite_session = next(satellite_db_client.get_db())

    # 创建 Scoped Session 以保证多个 Service 共享同一个 session
    satellite_session_factory = sessionmaker(bind=satellite_db_client.engine)
    satellite_session = scoped_session(satellite_session_factory)
    
    # tile_session_factory = sessionmaker(bind=tile_db_client.engine)
    # tile_session = scoped_session(tile_session_factory)


    Singleton.get_instance("sensor_service",       SensorService,    satellite_session)
    
    Singleton.get_instance("product_service",      ProductService,   satellite_session)
   
    Singleton.get_instance("scene_service",        SceneService,     satellite_session)
    
    Singleton.get_instance("image_service",        ImageService,     satellite_session, minio_client)
    
    Singleton.get_instance("tile_service",         TileService,      tile_db_client, minio_client) # 特殊一点，因为要动态反射

    Singleton.get_instance("project_data_service", ProjectDataService, satellite_session, minio_client)