from sqlalchemy.orm import Session
from dataModel.product import Product
class ProductService:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, product_id: str):
        return self.db.query(Product).filter(Product.product_id == product_id).first()

    def get_all(self):
        return self.db.query(Product).all()

    def filter_by_name(self, product_name: str):
        return self.db.query(Product).filter(Product.product_name.like(f"%{product_name}%")).all()
    
    def filter_by_sensor_id(self, sensor_id: str):
        return self.db.query(Product).filter(Product.sensor_id == sensor_id).all()