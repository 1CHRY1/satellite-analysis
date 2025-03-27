from sqlalchemy.orm import Session
from TransferEngine.dataModel.sensor import Sensor

class SensorService:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, sensor_id: str):
        return self.db.query(Sensor).filter(Sensor.sensor_id == sensor_id).first()

    def get_all(self):
        return self.db.query(Sensor).all()

    def filter_by_name(self, sensor_name: str):
        return self.db.query(Sensor).filter(Sensor.sensor_name.like(f"%{sensor_name}%")).all()