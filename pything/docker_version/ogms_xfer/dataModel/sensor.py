from sqlalchemy import Column, Text, String

from ..connection.database import DatabaseClient

class Sensor(DatabaseClient.Base):
    __tablename__ = 'sensor_table'  # map to table in database

    sensor_id = Column(String(36), primary_key=True, index=True)
    sensor_name = Column(String(255), index=True)
    platform_name = Column(String(255))
    description = Column(Text)

    def __repr__(self): # call when print object
        return f"Sensor(sensor_id={self.sensor_id}, \
            sensor_name={self.sensor_name}, \
            platform_name={self.platform_name}, \
            description={self.description}) \n"