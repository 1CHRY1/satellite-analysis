from sqlalchemy import Column, Text, String, Integer, Float
from sqlalchemy import Table, MetaData
from geoalchemy2 import Geometry

from ..connection.database import DatabaseClient


####### TileBase ###################################
class TileBase(DatabaseClient.Base):
    # __tablename__ = 'xxxx'  # 动态表名无法直接映射
    __abstract__ = True  # 这个类不会映射到数据库，只是作为基类
    # tile_id = Column(String(36), primary_key=True, index=True)
    image_id = Column(String(15), index=True)
    tile_level = Column(String(36))
    column_id = Column(Integer)
    row_id = Column(Integer)    
    path = Column(String(255))
    bucket = Column(String(36))
    cloud = Column(Float)
    band = Column(Integer)
    # Ignore the warning:  
    # SAWarning: Did not recognize type 'geometry' of column 'bounding_box'
    bounding_box = Column(Geometry(geometry_type='POLYGON'))

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
        
        # print(" create new model for ", lower_scene_id) # debug
        
        class DynamicTile(TileBase):
            __tablename__ = lower_scene_id 
            __table__ = Table(
                lower_scene_id,
                self.metadata,
                Column("tile_id", String(36), primary_key=True, index=True),
                autoload_with=self.db_engine,
                )

        self.model_cache[lower_scene_id] = DynamicTile

        return DynamicTile
