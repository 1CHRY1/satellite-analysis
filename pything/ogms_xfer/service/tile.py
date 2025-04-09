from sqlalchemy.orm import Session
from geoalchemy2 import Geometry  # <= not used but must be imported
from sqlalchemy import func
import os

from ..connection.database import DatabaseClient
from ..connection.minio import MinioClient
from ..dataModel.tile import TileFactory
from ..service.util import geojson_to_wkt

from dataclasses import dataclass
@dataclass
class GridCell:
    columnId: int
    rowId: int
class TileService:
    def __init__(self, db_client: DatabaseClient, minio_client: MinioClient):

        self.tile_factory = TileFactory(db_client)
        self.db_engine = db_client.engine
        self.minio_client = minio_client
        
    def get_tile(self, scene_id: str, tile_id: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            return session.query(TileModel).filter(TileModel.tile_id == tile_id).first() if TileModel else None

    def get_tiles(self, scene_id: str, image_id: str = None, cloud_range: tuple[float, float] = None, polygon: object = None, tile_ids: list[str] = None, band: int = None, tile_level: str = None, row_id: int = None, column_id: int = None, grid_cells: list[GridCell] = None):
        TileModel = self.tile_factory.get_tile_model(scene_id)  # 动态获取 ORM 模型
        with Session(self.db_engine) as session:
            query = session.query(TileModel)
            if image_id is not None:
                query = query.filter(TileModel.image_id == image_id)
            if cloud_range is not None:
                query = query.filter(TileModel.cloud.between(cloud_range[0], cloud_range[1]))
            if polygon is not None:
                wkt_polygon = geojson_to_wkt(polygon)
                query = query.filter(func.ST_Intersects(
                        TileModel.bounding_box,
                        func.ST_GeomFromText(wkt_polygon, 4326, 'axis-order=long-lat')
                    ))
            if tile_ids is not None:
                query = query.filter(TileModel.tile_id.in_(tile_ids))
            if band is not None:
                query = query.filter(TileModel.band == band)
            if tile_level is not None:
                query = query.filter(TileModel.tile_level == tile_level)
            if row_id is not None:
                query = query.filter(TileModel.row_id == row_id)
            if column_id is not None:
                query = query.filter(TileModel.column_id == column_id)
            if grid_cells is not None:
                rowids = [cell.rowId for cell in grid_cells]
                columnids = [cell.columnId for cell in grid_cells]
                query = query.filter(TileModel.row_id.in_(rowids) and TileModel.column_id.in_(columnids))
                
            return query.all()
        
    def pull_tile(self, scene_id: str, tile_id: str, output_path: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            tile = session.query(TileModel).filter(TileModel.tile_id == tile_id).first() if TileModel else None
            if tile is None:   return None
      
            self.minio_client.pull_file(tile.bucket, tile.path, output_path)
            return tile
        
    def pull_tiles(self, scene_id: str, tile_ids: list[str], output_dir: str):
        TileModel = self.tile_factory.get_tile_model(scene_id)
        with Session(self.db_engine) as session:
            tiles = session.query(TileModel).filter(TileModel.tile_id.in_(tile_ids)).all()
            for tile in tiles:
                self.minio_client.pull_file(tile.bucket, tile.path, os.path.join(output_dir, f"{tile.tile_id}.tif"))