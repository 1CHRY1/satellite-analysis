from sqlalchemy.orm import Session
from dataModel.scene import Scene

class SceneService:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, scene_id: str):
        return self.db.query(Scene).filter(Scene.scene_id == scene_id).first()

    def get_all(self):
        return self.db.query(Scene).all()

    def filter_by_name(self, scene_name: str):
        return self.db.query(Scene).filter(Scene.scene_name.like(f"%{scene_name}%")).all()
    
    def filter_by_product_id(self, product_id: str):
        return self.db.query(Scene).filter(Scene.product_id == product_id).all()

