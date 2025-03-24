# from geoalchemy2 import Geometry ## error here
from sqlalchemy import Column, Text, String, Integer, Float
from connection.database import DatabaseClient

####### TileBase ###################################
class TileBase(DatabaseClient.Base):
    # __tablename__ = 'xxxx'  # 动态表名无法直接映射

    __abstract__ = True  # 这个类不会映射到数据库，只是作为基类
    tile_id = Column(String(36), primary_key=True, index=True)
    image_id = Column(String(36), index=True)
    tile_level = Column(Integer)
    column_id = Column(Integer)
    row_id = Column(Integer)    
    path = Column(String(255))
    bucket = Column(String(36))
    # bounding_box = Column(Geometry())
    cloud = Column(Float)

    def __repr__(self): # call when print object
        return f"Tile(tile_id={self.tile_id}, \
            image_id={self.image_id}, \
            tile_level={self.tile_level}, \
            column_id={self.column_id}, \
            row_id={self.row_id}, \
            bucket={self.bucket}, \
            cloud={self.cloud}) \n"




####### Tile Factory ###################################
from sqlalchemy import Table, MetaData
from dataModel.tile import TileBase
from connection.database import DatabaseClient

class TileFactory:
    """根据 sceneID 生成对应的 ORM 类"""
    def __init__(self, db_client: DatabaseClient):
        self.db_engine = db_client.engine
        self.metadata = MetaData()

    def get_tile_model(self, image_id):
        """
        动态创建 Tile 模型，绑定 imageID 作为表名
        """
        class DynamicTile(TileBase):
            __tablename__ = image_id
            __table__ = Table(image_id, self.metadata, autoload_with=self.db_engine)

        return DynamicTile
