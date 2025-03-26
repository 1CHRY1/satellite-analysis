import os, json, time, random
from threading import Timer
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

def push_to_remote(input_file_path):
    """Push file to minio and update database"""
    
    global minio_client, mysql_engine, USER_ID, PROJECT_ID, BUCKET_NAME
    object_name = get_object_name(input_file_path)
    
    try:
        # check if bucket exists
        found = minio_client.bucket_exists(BUCKET_NAME)
        if not found:
            print(f"Bucket '{BUCKET_NAME}' does not exist. ! Create bucket !")
            minio_client.make_bucket(BUCKET_NAME)
            
        # check if object exists
        try:
            minio_client.stat_object(BUCKET_NAME, object_name)
            print(f"Data with path {object_name} already exists in bucket {BUCKET_NAME}. ! Deleting object !")
            
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
        

        print(f"File was pushed to bucket '{BUCKET_NAME}' as '{object_name}'.")

    except S3Error as e:
        print(f"Error occurred: {e}")
        raise e
    
    
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
        print(f"Error occurred: {e}")
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
    
    def __init__(self, delay = 1.0):
        FileSystemEventHandler.__init__(self)
        self.delay = delay
        self.timer = None

    def on_any_event(self, event: FileSystemEvent) -> None:
        """事件防抖"""
        if self.timer:
            self.timer.cancel()
            
        # 这里第二个参数需要是一个函数对象，这就出问题了
        # self.timer = Timer(1, self.process_event(event))
        
        # 在js里我们用(e)=>{ haha(e) }包一层就可以, python里用args传参 , 或者 用lambda
        # self.timer = Timer(1, self.process_event, args=(event,))
        
        self.timer = Timer(1, lambda: self.process_event(event))
        self.timer.start()
    
    def process_event(self, event):
        
        if event.event_type == 'created' or event.event_type == 'modified':
            print(f" # {event.event_type} #  {event.src_path} -- trigger push/update to remote")
            push_to_remote(event.src_path)
            
        elif event.event_type == 'deleted':
            print(f" # {event.event_type} #  {event.src_path} -- trigger delete from remote")
            delete_remote_object(event.src_path)
            
        else:
            print(f" # {event.event_type} #  {event.src_path} -- do nothing")
            push_to_remote(event.src_path)


def start_watching():
    """启动文件监听"""
    observer = Observer()
    event_handler = FileEventHandler(delay = 0.5) # delay 0.5s
    observer.schedule(event_handler, WATCH_DIR, recursive=False)
    observer.start()
    
    print(f"(((o(*ﾟ▽ﾟ*)o))) :: Start watching directory: {WATCH_DIR} \n")

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