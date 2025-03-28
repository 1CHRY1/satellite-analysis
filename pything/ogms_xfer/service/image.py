from sqlalchemy.orm import Session
from typing import Union 

from ..dataModel.image import Image
from ..connection.minio import MinioClient

class ImageService:
    def __init__(self, db: Session, minio_client: MinioClient):
        self.db = db
        self.minio_client = minio_client

    def get_by_id(self, image_id: str):
        return self.db.query(Image).filter(Image.image_id == image_id).first()

    def get_all(self):
        return self.db.query(Image).all()
    
    def filter_by_scene_id(self, scene_id: str):
        return self.db.query(Image).filter(Image.scene_id == scene_id).all()
    
    def filter_by_scene_id_and_band(self, scene_id: str, band: str):
        return self.db.query(Image).filter(Image.scene_id == scene_id, Image.band == band).first()
    
    def pull_image(self, image: Union[str, Image], output_path: str):
        if isinstance(image, str):
            image = self.get_by_id(image)
        if image is not None:
            self.minio_client.pull_file(image.bucket, image.tif_path, output_path)
        else:
            raise ValueError(f"Image not found")