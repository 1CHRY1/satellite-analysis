from typing import Union
import os, random
from datetime import datetime
from sqlalchemy.orm import Session

from ..connection.minio import MinioClient
from ..dataModel.project_data import ProjectData

class ProjectDataService:
    def __init__(self, db: Session, minio_client: MinioClient):
        self.db = db
        self.minio_client = minio_client

    def get_by_id(self, data_id: str):
        return self.db.query(ProjectData).filter(ProjectData.data_id == data_id).first()
    
    def get_by_name(self, data_name: str):
        return self.db.query(ProjectData).filter(ProjectData.data_name == data_name).first()

    def get_all(self):
        return self.db.query(ProjectData).all()

    def filter_by_name(self, data_name: str):
        return self.db.query(ProjectData).filter(ProjectData.data_name.like(f"%{data_name}%")).all()
    
    def filter_by_project_id(self, project_id: str):
        return self.db.query(ProjectData).filter(ProjectData.project_id == project_id).all()
    
    def filter_by_user_id(self, user_id: str):
        return self.db.query(ProjectData).filter(ProjectData.user_id == user_id).all()
    
    def pull_data(self, data: Union[str, ProjectData], output_path: str):
        if isinstance(data, str):
            data = self.get_by_id(data)
        if data:
            self.minio_client.pull_file(data.bucket, data.path, output_path)
        else:
            raise ValueError(f"Data not found")
    
    def push_data(self, project_data: ProjectData, input_path: str):
        
        # validate
        if not project_data.bucket or not project_data.path:
            raise ValueError("Bucket and path must be provided for the project data.")
        
        # check input file exists
        if not os.path.exists(input_path):
            raise ValueError(f"File not found: {input_path}")
        
        # push to minio
        self.minio_client.push_file(project_data.bucket, project_data.path, input_path)
        
        # insert to db
        self.db.add(project_data)
        self.db.commit()
        
        print(f"Data pushed to {project_data.bucket}/{project_data.path}")
    
    def push_data_from_bytes(self, project_data: ProjectData, data: bytes, length: int):
        
        # validate
        if not project_data.bucket or not project_data.path:
            raise ValueError("Bucket and path must be provided for the project data.")
        existing_data = self.get_by_id(project_data.data_id)
        if existing_data:
            raise ValueError(f"Data with ID {project_data.data_id} already exists.")
        
        # push to minio
        self.minio_client.push_file_from_bytes(project_data.bucket, project_data.path, data, length)
        
        # insert to db
        self.db.add(project_data)
        self.db.commit()

        print(f"Data pushed to {project_data.bucket}/{project_data.path}")
        
    def create_project_data(self, data_name: str, project_id: str, user_id: str, bucket: str, data_type: str):
    # create meta-data
        data_id = 'D' + generate_id()
        create_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        path = f"/{user_id}/{project_id}/{data_name}"

        project_data = ProjectData(
            data_id=data_id,
            data_name=data_name,
            path=path,
            project_id=project_id,
            user_id=user_id,
            bucket=bucket,
            create_time=create_time,
            data_type=data_type
        )
        return project_data
    
    def delete_project_data(self, data: Union[str, ProjectData]):
        if isinstance(data, str):
            data = self.get_by_id(data)

        if data:
            # delete from minio
            self.minio_client.delete_file(data.bucket, data.path)
            
            # delete from db
            self.db.delete(data)
            self.db.commit()
            print(f"Data deleted: {data.data_id}")

        else:
            raise ValueError(f"Data not found")
    
    
## local helper function
def generate_id():
    random_number = ''.join([str(random.randint(0, 9)) for _ in range(10)])
    return f'{random_number}'