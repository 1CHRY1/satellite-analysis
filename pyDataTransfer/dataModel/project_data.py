from sqlalchemy import Column, String, DateTime
from connection.database import DatabaseClient


class ProjectData(DatabaseClient.Base):
    __tablename__ = 'project_data'  # map to table in database

    data_id = Column(String(20), primary_key=True, index=True)
    project_id = Column(String(30), index=True)
    data_name = Column(String(30), index=True)
    user_id = Column(String(30), index=True)
    path = Column(String(100))
    bucket = Column(String(20))
    create_time = Column(DateTime)
    data_type = Column(String(10))

    def __repr__(self): # call when print object
        return f"ProjectData(data_id={self.data_id}, \
            project_id={self.project_id}, \
            data_name={self.data_name}, \
            user_id={self.user_id}, \
            path={self.path}, \
            bucket={self.bucket}, \
            create_time={self.create_time}, \
            data_type={self.data_type}) \n"