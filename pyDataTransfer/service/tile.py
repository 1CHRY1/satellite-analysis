from connection.database import DatabaseClient
from connection.minio import MinioClient
from dataModel.tile import TileFactory
from sqlalchemy.orm import Session
from geoalchemy2 import Geometry  # <= not used but must be imported
import os

class TileService:
    def __init__(self, db_client: DatabaseClient, minio_client: MinioClient):

        self.tile_factory = TileFactory(db_client)
        self.db_engine = db_client.engine
        self.minio_client = minio_client

    def get_tiles_by_scene(self, scene_id: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)  # 动态获取 ORM 模型
        with Session(self.db_engine) as session:
            return session.query(TileModel).all()
    
    def get_tiles_by_scene_and_image(self, scene_id: str, image_id: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.image_id == image_id).all()

    def get_tiles_by_scene_and_cloud(self, scene_id: str, mincloud: float):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.cloud >= mincloud).all()
        
    def get_tiles_by_scene_and_image_and_cloud(self, scene_id: str, image_id: str, mincloud: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.image_id == image_id, TileModel.cloud >= mincloud).all()

    def get_tile_by_id(self, scene_id: str, tile_id: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.tile_id == tile_id).first() if TileModel else None
        
    def get_tiles_by_ids(self, scene_id: str, tile_ids: list[str]):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.tile_id.in_(tile_ids)).all()
    
    def pull_tile_by_id(self, scene_id: str, tile_id: str, output_path: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            tile = session.query(TileModel).filter(TileModel.tile_id == tile_id).first() if TileModel else None
            if tile is None:   return None
      
            self.minio_client.pull_file(tile.bucket, tile.path, output_path)
            return tile
        
    def pull_tiles_by_ids(self, scene_id: str, tile_ids: list[str], output_dir: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            tiles = session.query(TileModel).filter(TileModel.tile_id.in_(tile_ids)).all()
            for tile in tiles:
                self.minio_client.pull_file(tile.bucket, tile.path, os.path.join(output_dir, f"{tile.tile_id}.tif"))