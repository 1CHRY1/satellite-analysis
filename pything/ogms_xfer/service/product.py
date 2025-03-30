from sqlalchemy.orm import Session
from ..dataModel.product import Product

class ProductService:
    def __init__(self, db: Session):
        self.db = db

    def get_product(self, product_id: str):
        return self.db.query(Product).filter(Product.product_id == product_id).first()
    
    def get_products(self, sensor_id: str = None, product_name_like: str = None, resolution: str = None, period: str = None):
        query = self.db.query(Product)
        if product_name_like is not None:
            query = query.filter(Product.product_name.like(f"%{product_name_like}%"))
        if sensor_id is not None:
            query = query.filter(Product.sensor_id == sensor_id)
        if resolution is not None:
            query = query.filter(Product.resolution.like(f"%{resolution}%"))
        if period is not None:
            query = query.filter(Product.period.like(f"%{period}%"))
        return query.all()
    
    def get_all(self):
        return self.db.query(Product).all()