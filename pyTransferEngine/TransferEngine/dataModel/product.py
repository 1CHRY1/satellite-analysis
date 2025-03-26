from sqlalchemy import Column, Text, String, Float, Integer

from TransferEngine.connection.database import DatabaseClient

class Product(DatabaseClient.Base):
    __tablename__ = 'product_table'  # map to table in database

    product_id = Column(String(36), primary_key=True, index=True)
    sensor_id = Column(String(36), index=True)
    product_name = Column(String(255), index=True)
    description = Column(Text)
    resolution = Column(String(255))
    period = Column(Integer)

    def __repr__(self): # call when print object
        return f"Product(product_id={self.product_id}, \
            sensor_id={self.sensor_id}, \
            product_name={self.product_name}, \
            description={self.description}, \
            resolution={self.resolution}, \
            period={self.period}) \n"