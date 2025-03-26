from sqlalchemy import Column, String, Float

from TransferEngine.connection.database import DatabaseClient

class Image(DatabaseClient.Base):
    __tablename__ = 'image_table'  # map to table in database

    image_id = Column(String(36), primary_key=True, index=True)
    scene_id = Column(String(36), index=True)
    tif_path = Column(String(255))
    band = Column(String(255))
    bucket = Column(String(36))
    cloud = Column(Float)

    def __repr__(self): # call when print object
        return f"Image(image_id={self.image_id}, \
            scene_id={self.scene_id}, \
            tif_path={self.tif_path}, \
            band={self.band}, \
            bucket={self.bucket}, \
            cloud={self.cloud}) \n"