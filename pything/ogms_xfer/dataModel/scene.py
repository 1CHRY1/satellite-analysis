from geoalchemy2 import Geometry
from sqlalchemy import Column, Text, String, DateTime, Integer, Float

from ..connection.database import DatabaseClient

class Scene(DatabaseClient.Base):
    __tablename__ = 'scene_table'  # map to table in database

    scene_id = Column(String(36), primary_key=True, index=True)
    product_id = Column(String(36), index=True)
    scene_name = Column(String(255), index=True)
    scene_time = Column(DateTime)
    sensor_id = Column(String(36), index=True)
    coordinate_system = Column(String(255))
    bounding_box = Column(Geometry(geometry_type='POLYGON')) # not sure
    description = Column(Text)
    png_path = Column(String(255))
    bands = Column(Text)
    band_num = Column(Integer)
    bucket = Column(String(255))
    cloud = Column(Float)
    
    def __repr__(self): # call when print object
        return f"Scene(scene_id={self.scene_id}, \
            product_id={self.product_id}, \
            scene_name={self.scene_name}, \
            scene_time={self.scene_time}, \
            sensor_id={self.sensor_id}, \
            coordinate_system={self.coordinate_system}, \
            bounding_box={self.bounding_box}, \
            description={self.description}, \
            png_path={self.png_path}, \
            bands={self.bands}, \
            band_num={self.band_num}, \
            bucket={self.bucket}, \
            cloud={self.cloud}) \n"