from sqlalchemy import Column, Text, String, Integer, Float
from sqlalchemy import Table, MetaData
from geoalchemy2 import Geometry

from ..connection.database import DatabaseClient


####### TileBase ###################################
class TileBase(DatabaseClient.Base):
    # __tablename__ = 'xxxx'  # 动态表名无法直接映射
    __abstract__ = True  # 这个类不会映射到数据库，只是作为基类

    def __repr__(self): # call when print object
        return f"Tile(tile_id={self.tile_id}, \
            image_id={self.image_id}, \
            tile_level={self.tile_level}, \
            column_id={self.column_id}, \
            row_id={self.row_id}, \
            bucket={self.bucket}, \
            bounding_box={self.bounding_box}, \
            cloud={self.cloud}) \n"


####### Tile Factory ###################################
class TileFactory:
    """根据 sceneID 生成对应的 ORM 类"""
    def __init__(self, db_client: DatabaseClient):
        self.db_engine = db_client.engine
        self.metadata = MetaData()
        self.model_cache = {}  # data model cache

    def get_tile_model(self, scene_id):
        """
        动态创建 Tile 模型，绑定 sceneID 作为表名
        """
        
        lower_scene_id = scene_id.lower() # 表名小写
        
        # find in cache first
        if lower_scene_id in self.model_cache:
            return self.model_cache[lower_scene_id]
            
        if not self.db_engine.dialect.has_table(self.db_engine.connect(), lower_scene_id):
            raise ValueError(f"表 {lower_scene_id} 不存在于数据库中")

        # 手动定义所有列，避免使用autoload_with, autoload_with对于Geometry类型会发警告
        table = Table(
            lower_scene_id,
            self.metadata,
            Column("tile_id", String(36), primary_key=True, index=True),
            Column("image_id", String(15), index=True),
            Column("tile_level", String(36)),
            Column("column_id", Integer),
            Column("row_id", Integer),
            Column("path", String(255)),
            Column("bucket", String(36)),
            Column("cloud", Float),
            Column("band", Integer),
            Column("bounding_box", Geometry(geometry_type='POLYGON', srid=4326))
        )
        
        class_dict = {
            '__tablename__': lower_scene_id,
            '__table__': table
        }
        
        model_cls = type(
            f"Tile_{lower_scene_id}",
            (TileBase,),
            class_dict
        )
        
        self.model_cache[lower_scene_id] = model_cls
        return model_cls