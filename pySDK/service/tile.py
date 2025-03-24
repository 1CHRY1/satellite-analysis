from connection.database import DatabaseClient
from connection.minio import MinioClient
from dataModel.tile import TileFactory
from sqlalchemy.orm import Session
import os

class TileService:
    def __init__(self, db_client: DatabaseClient, minio_client: MinioClient):
        self.tile_factory = TileFactory(db_client)
        self.db_engine = db_client.engine
        self.minio_client = minio_client

    def get_tiles_by_image(self, image_id: str):
            """ 获取指定 `image_id` 下的所有瓦片 """
            TileModel = self.tile_factory.get_tile_model(image_id)  # 动态获取 ORM 模型
            with Session(self.db_engine) as session:
                return session.query(TileModel).all()

    def get_tile_by_id(self, image_id: str, tile_id: str):
        """ 根据 `image_id` 和 `tile_id` 获取单个瓦片 """
        TileModel = self.tile_factory.get_tile_model(image_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.tile_id == tile_id).first() if TileModel else None
        
    def pull_tile_by_id(self, image_id: str, tile_id: str, output_path: str):
        """ 根据 `image_id` 和 `tile_id` 拉取单个瓦片 """
        TileModel = self.tile_factory.get_tile_model(image_id)
        with Session(self.db_engine) as session:
            tile = session.query(TileModel).filter(TileModel.tile_id == tile_id).first() if TileModel else None
            if tile is None:   return None
      
            self.minio_client.pull_file(tile.bucket, tile.path, output_path)
            return tile
        
    def pull_tiles_by_ids(self, image_id: str, tile_ids: list[str], output_dir: str):
        """ 根据 `image_id` 和 `tile_ids` 拉取多个瓦片 """
        TileModel = self.tile_factory.get_tile_model(image_id)
        with Session(self.db_engine) as session:
            tiles = session.query(TileModel).filter(TileModel.tile_id.in_(tile_ids)).all()
            for tile in tiles:
                self.minio_client.pull_file(tile.bucket, tile.path, os.path.join(output_dir, f"{tile.tile_id}.tif"))

