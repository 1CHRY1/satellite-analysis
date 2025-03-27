import os, json, time, random
from watchdog.observers import Observer
from watchdog.events import FileSystemEvent, FileSystemEventHandler
from datetime import datetime
from minio import Minio
from minio.error import S3Error
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

######  Global variables  #####################################################
global minio_client, mysql_engine
global USER_ID, PROJECT_ID
global BUCKET_NAME, WATCH_DIR
# minio_client = None
# mysql_engine = None
# USER_ID = None
# PROJECT_ID = None
# BUCKET_NAME = None
# WATCH_DIR = None


######  Initialize  ###########################################################

def initialize(config_file_path: str):

    global minio_client, mysql_engine, BUCKET_NAME, WATCH_DIR, USER_ID, PROJECT_ID
    
    with open(config_file_path, 'r') as f:
        config = json.load(f)
    
    minio_config = config["minio"]
    database_config = config["database"]
    USER_ID = config["project_info"]["user_id"]
    PROJECT_ID = config["project_info"]["project_id"]
    BUCKET_NAME = config["project_info"]["bucket"]
    WATCH_DIR = config["project_info"]["watch_dir"]
    
    secure = minio_config["secure"]
    secure = False
    minio_client = Minio(
        minio_config["endpoint"], 
        access_key=minio_config["access_key"], 
        secret_key=minio_config["secret_key"], 
        secure=secure
    )
    
    url = f"mysql+pymysql://{database_config['user']}:{database_config['password']}@{database_config['endpoint']}/{database_config['satellite_database']}"
    mysql_engine = create_engine(url)


###### Data Model of table<project_data> #########################################

from sqlalchemy import Column, String, DateTime
from sqlalchemy.orm import declarative_base

Base = declarative_base()
class ProjectData(Base):
    __tablename__ = 'project_data'

    data_id = Column(String(20), primary_key=True, index=True)
    project_id = Column(String(30), index=True)
    data_name = Column(String(30), index=True)
    user_id = Column(String(30), index=True)
    path = Column(String(100))
    bucket = Column(String(20))
    create_time = Column(DateTime)
    data_type = Column(String(10))

    def __repr__(self):
        return f"ProjectData(data_id={self.data_id}, \
            data_name={self.data_name}, \
            path={self.path}, \
            bucket={self.bucket}, \
            create_time={self.create_time}, \
            data_type={self.data_type})"


######  File Operations  #########################################################
def delete_remote_object(delete_file_path):
    """Delete object from storage"""
    
    global minio_client, mysql_engine, USER_ID, PROJECT_ID, BUCKET_NAME
    object_name = get_object_name(delete_file_path)
    try:
        # delete object from minio
        minio_client.remove_object(BUCKET_NAME, object_name)
        
        # delete object from database
        with Session(mysql_engine) as session:
            session.query(ProjectData).filter(
                ProjectData.user_id == USER_ID,
                ProjectData.project_id == PROJECT_ID,
                ProjectData.bucket == BUCKET_NAME,
                ProjectData.path == object_name,
            ).delete()
            session.commit()
            
    except S3Error as e:
        raise e

def push_to_remote(input_file_path):
    """Push file to minio and update database"""
    
    global minio_client, mysql_engine, USER_ID, PROJECT_ID, BUCKET_NAME
    object_name = get_object_name(input_file_path)
    
    try:
        # check if bucket exists
        found = minio_client.bucket_exists(BUCKET_NAME)
        if not found:
            minio_client.make_bucket(BUCKET_NAME)
            
        # check if object exists
        try:
            minio_client.stat_object(BUCKET_NAME, object_name)
            delete_remote_object(input_file_path)
            
        except S3Error:
            pass  # if object not exists, continue
            
        # push file to minio
        minio_client.fput_object(
            BUCKET_NAME,
            object_name,
            input_file_path
        )
        # insert data into database
        with Session(mysql_engine) as session:
            session.add(ProjectData(
                data_id=generate_id('D'),
                project_id=PROJECT_ID,
                data_name=object_name.split('/')[-1],
                user_id=USER_ID,
                path=object_name,
                bucket=BUCKET_NAME,
                create_time=datetime.now(),
                data_type=object_name.split('.')[-1] if '.' in object_name else 'file'
            ))
            session.commit()
        
    except S3Error as e:
        raise e
    



###### Local Helper Functions #################################################
def generate_id(prefix: str):
    random_number = ''.join([str(random.randint(0, 9)) for _ in range(10)])
    return f'{prefix}{random_number}'

def get_object_name(file_path):
    global USER_ID, PROJECT_ID
    file_path_clone = file_path.replace('\\', '/')
    return '/' + USER_ID + '/' + PROJECT_ID + '/' + file_path_clone.split('/')[-1]


######  Watcher  ##############################################################
class FileEventHandler(FileSystemEventHandler):
    
    def __init__(self):
        FileSystemEventHandler.__init__(self)

    def on_any_event(self, event: FileSystemEvent) -> None:
        """直接处理事件"""
        if event.event_type == 'created' or event.event_type == 'modified':
            push_to_remote(event.src_path)
            
        elif event.event_type == 'deleted':
            delete_remote_object(event.src_path)
            
        elif event.event_type == 'moved':
            delete_remote_object(event.src_path)
            push_to_remote(event.dest_path)
            
        else:
            raise ValueError(f"Unknown event type: {event.event_type}")

def clear_test_bucket():
    """Clear test bucket"""
    global minio_client, BUCKET_NAME
    minio_client.remove_bucket(BUCKET_NAME)


def start_watching():
    """启动文件监听"""
    observer = Observer()
    event_handler = FileEventHandler()
    observer.schedule(event_handler, WATCH_DIR, recursive=False)
    observer.start()
    
    # print(f"(((o(*ﾟ▽ﾟ*)o))) :: Start watching directory: {WATCH_DIR} \n")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()


if __name__ == "__main__":
    
    config_file_path = "D:\\t\\watcher_test_config.json"
    initialize(config_file_path)
    start_watching()