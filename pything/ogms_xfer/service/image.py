from sqlalchemy.orm import Session
from typing import Union 

from ..dataModel.image import Image
from ..connection.minio import MinioClient

class ImageService:
    def __init__(self, db: Session, minio_client: MinioClient):
        self.db = db
        self.minio_client = minio_client

    def get_image(self, image_id: str):
        return self.db.query(Image).filter(Image.image_id == image_id).first()

    def get_images(self, scene_id: str = None, band: str = None, cloud_range: tuple[float, float] = None):
        query = self.db.query(Image)
        if scene_id is not None:
            query = query.filter(Image.scene_id == scene_id)
        if band is not None:
            query = query.filter(Image.band.like(f"%{band}%"))
        if cloud_range is not None:
            query = query.filter(Image.cloud.between(cloud_range[0], cloud_range[1]))
        return query.all() if band is None else query.first()
    
    def pull_image(self, image: Union[str, Image], output_path: str):
        if isinstance(image, str):
            image = self.get_image(image)
        if image is not None:
            self.minio_client.pull_file(image.bucket, image.tif_path, output_path)
        else:
            raise ValueError(f"Image not found")