from sqlalchemy.orm import Session
from ..dataModel.sensor import Sensor

class SensorService:
    def __init__(self, db: Session):
        self.db = db

    def get_sensor(self, sensor_id: str):
        return self.db.query(Sensor).filter(Sensor.sensor_id == sensor_id).first()

    def get_sensors(self, sensor_name_like: str = None, platform_name_like: str = None):
        query = self.db.query(Sensor)
        if sensor_name_like is not None:
            query = query.filter(Sensor.sensor_name.like(f"%{sensor_name_like}%"))
        if platform_name_like is not None:
            query = query.filter(Sensor.platform.like(f"%{platform_name_like}%"))
        return query.all()
    
    def get_all(self):
        return self.db.query(Sensor).all()
