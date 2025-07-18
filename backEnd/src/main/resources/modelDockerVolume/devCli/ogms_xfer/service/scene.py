from sqlalchemy.orm import Session
from datetime import datetime
from sqlalchemy import func
from ..dataModel.scene import Scene
from ..service.util import geojson_to_wkt

class SceneService:
    def __init__(self, db: Session):
        self.db = db

    def get_scene(self, scene_id: str):
        return self.db.query(Scene).filter(Scene.scene_id == scene_id).first()

    def get_scenes(self, scene_name: str = None, product_id: str = None, polygon: object = None, time_range: tuple[datetime, datetime] = None, cloud_range: tuple[float, float] = None):
        query = self.db.query(Scene)
        if scene_name is not None:
            query = query.filter(Scene.scene_name.like(f"%{scene_name}%"))
        if product_id is not None:
            query = query.filter(Scene.product_id == product_id)
        if polygon is not None:
            wkt_polygon = geojson_to_wkt(polygon)
            query = query.filter(func.ST_Intersects(
                        Scene.bounding_box,
                        func.ST_GeomFromText(wkt_polygon, 4326, 'axis-order=long-lat')
                    ))
        if time_range is not None:
            query = query.filter(Scene.scene_time.between(time_range[0], time_range[1]))
        if cloud_range is not None:
            query = query.filter(Scene.cloud.between(cloud_range[0], cloud_range[1]))
        return query.all()
    
    def get_all(self):
        return self.db.query(Scene).all()