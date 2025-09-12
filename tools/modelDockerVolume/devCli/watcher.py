import os, json, time, random
from threading import Timer
from rio_cogeo import cog_validate, cog_translate
from rio_cogeo.profiles import cog_profiles
from watchdog.observers import Observer
from watchdog.events import FileSystemEvent, FileSystemEventHandler
from datetime import datetime
from minio import Minio
from minio.error import S3Error
from sqlalchemy import create_engine, func
from sqlalchemy.orm import Session
from geoalchemy2 import Geometry
import rasterio
from shapely.geometry import Polygon
from shapely import wkt

######  Global variables  #####################################################
global minio_client, mysql_engine
global USER_ID, PROJECT_ID
global BUCKET_NAME, WATCH_DIR


######  Initialize  ###########################################################

def initialize(config_file_path: str):

    global minio_client, mysql_engine, mysql_dev_engine, BUCKET_NAME, WATCH_DIR, USER_ID, PROJECT_ID
    
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
    
    url = f"mysql+pymysql://{database_config['user']}:{database_config['password']}@{database_config['endpoint']}/{database_config['dev_database']}"
    # url = f"mysql+pymysql://{database_config['user']}:{database_config['password']}@{database_config['endpoint']}/{database_config['satellite_database']}"
    mysql_engine = create_engine(url)

###### Data Model of table<project_data> #########################################

from sqlalchemy import Column, String, DateTime
from sqlalchemy.orm import declarative_base

Base = declarative_base()
class ProjectData(Base):
    __tablename__ = 'project_data_table'

    data_id = Column(String(20), primary_key=True, index=True)
    project_id = Column(String(30), index=True)
    data_name = Column(String(30), index=True)
    user_id = Column(String(30), index=True)
    path = Column(String(100))
    bucket = Column(String(20))
    create_time = Column(DateTime)
    data_type = Column(String(10))
    bounding_box = Column(Geometry(geometry_type='POLYGON', srid=4326))

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
    final_file_path = input_file_path
    geo_bounds = None
    need_clean = False
    
    is_a_tif = is_tif(input_file_path)
    
    if is_a_tif and not is_cog(input_file_path):
        final_file_path = translate_tif_to_cog(input_file_path, os.path.join(os.path.dirname(__file__), 'temp_cog.tif'))
        need_clean = True
    
    if is_a_tif:
        try:
            with rasterio.open(input_file_path) as src:
                bounds = src.bounds
                src_crs = src.crs
                
                if src_crs and src_crs.to_string() != 'EPSG:4326':
                    bounds = rasterio.warp.transform_bounds(
                        src_crs,
                        'EPSG:4326',
                        bounds.left,
                        bounds.bottom,
                        bounds.right,
                        bounds.top
                    )
                
                polygon = Polygon([
                    (bounds[0], bounds[1]),  # left, bottom
                    (bounds[2], bounds[1]),  # right, bottom
                    (bounds[2], bounds[3]),  # right, top
                    (bounds[0], bounds[3]),  # left, top
                    (bounds[0], bounds[1])   # 闭合多边形
                ])
                geo_bounds = wkt.dumps(polygon)
                geo_bounds = func.ST_GeomFromText(geo_bounds, 4326, 'axis-order=long-lat')

        except Exception as e:
            print(f"提取边界框时出错：{str(e)}")
    
    try:
        
        found = minio_client.bucket_exists(BUCKET_NAME)
        if not found:
            minio_client.make_bucket(BUCKET_NAME)
            
        try:
            minio_client.stat_object(BUCKET_NAME, object_name)
            delete_remote_object(input_file_path)
        except S3Error:
            pass
            
        print(f"开始上传文件到MinIO: {BUCKET_NAME}/{object_name}")
        print(f"本地文件路径: {final_file_path}")
        
        file_size = os.path.getsize(final_file_path)
        with open(final_file_path, 'rb') as file_data:
            minio_client.put_object(
                BUCKET_NAME,
                object_name,
                file_data,
                file_size,
                content_type='application/octet-stream',
            )
        
        # 验证文件是否成功上传
        try:
            # 检查文件是否存在
            minio_client.stat_object(BUCKET_NAME, object_name)
            # 检查文件大小是否匹配
            remote_size = minio_client.stat_object(BUCKET_NAME, object_name).size
            if remote_size != file_size:
                raise Exception(f"文件大小不匹配: 本地 {file_size} 字节, 远程 {remote_size} 字节")
            
            print("MinIO文件上传验证成功")
            
            # 只有在确认MinIO上传成功后，才进行数据库操作
            with Session(mysql_engine) as session:
                session.add(ProjectData(
                    data_id=generate_id('D'),
                    project_id=PROJECT_ID,
                    data_name=object_name.split('/')[-1],
                    user_id=USER_ID,
                    path=object_name,
                    bucket=BUCKET_NAME,
                    create_time=datetime.now(),
                    data_type=object_name.split('.')[-1] if '.' in object_name else 'file',
                    bounding_box=geo_bounds
                ))
                session.commit()
                print("数据库记录添加成功")
                
        except S3Error as e:
            print(f"MinIO文件验证失败: {str(e)}")
            raise Exception("MinIO文件上传验证失败")
        
    except S3Error as e:
        print(f"MinIO上传错误: {str(e)}")
        raise e
    except Exception as e:
        print(f"发生未知错误: {str(e)}")
        raise e
    
    finally:
        if need_clean and os.path.exists(final_file_path):
            print("删除临时文件 :: ", final_file_path)
            os.remove(final_file_path)


###### Local Helper Functions #################################################
def generate_id(prefix: str):
    random_number = ''.join([str(random.randint(0, 9)) for _ in range(10)])
    return f'{prefix}{random_number}'

def get_object_name(file_path):
    global USER_ID, PROJECT_ID
    print("get_object_name file_path :: ", file_path)
    file_path_clone = file_path.replace('\\', '/')
    print("get_object_name file_path_clone :: ", file_path_clone)
    return '/user-files/' + USER_ID + '/' + PROJECT_ID + '/' + file_path_clone.split('/')[-1]

def is_tif(filepath):
    return filepath.endswith('.tif') or filepath.endswith('.TIF') or filepath.endswith('.tiff') or filepath.endswith('.TIFF')

def is_cog(filepath):
    try:
        return cog_validate(filepath, quiet=True)[0]
    except Exception as e:
        print(f"COG验证出错: {str(e)}")
        return False

dst_profile = cog_profiles.get("deflate")
def translate_tif_to_cog(input_tif, output_cog):
    cog_translate(input_tif, output_cog, dst_profile, in_memory=True, quiet=True)
    return output_cog


def make_bucket_public(minio_client, bucket_name):
    policy = f'''
    {{
      "Version": "2012-10-17",
      "Statement": [
        {{
          "Effect": "Allow",
          "Principal": "*",
          "Action": ["s3:GetObject"],
          "Resource": ["arn:aws:s3:::{bucket_name}/*"]
        }}
      ]
    }}
    '''
    minio_client.set_bucket_policy(bucket_name, policy)

from shapely.geometry import shape
def geojson_to_wkt(geojson_dict):
    # 仅支持单个多边形
    geom = shape(geojson_dict.get('features')[0].get('geometry'))
    return geom.wkt

######  Watcher  ##############################################################
class FileEventHandler(FileSystemEventHandler):
    
    def __init__(self):
        FileSystemEventHandler.__init__(self)
        self.timer = None
        self.file_event_history = {}  

    def on_any_event(self, event: FileSystemEvent) -> None:
        
        # 忽略目录
        if event.is_directory:
            return
        # 忽略 Minio 生成的部分文件
        if event.src_path.endswith('.part.minio'):
            return
        # 忽略临时 COG 文件
        if event.src_path.startswith('temp_cog'):
            return
        
        self.process_event(event)
    
            
    def process_event(self, event: FileSystemEvent):
        
        if(event.event_type == 'deleted'): # 只触发一次， 直接处理
            print('$2 deleted事件 : ', event.src_path )
            delete_remote_object(event.src_path)
        
        elif(event.event_type == 'modified'):
            print('$2 modified事件 : ', event.src_path )
            
        elif(event.event_type == 'moved'):
            print('$2 moved事件 : ', event.src_path )
            
        elif(event.event_type == 'created'):
            print('$2 created事件 : ', event.src_path )
    
        elif(event.event_type == 'closed'):
            print('$2 closed事件 : ', event.src_path )
            push_to_remote(event.src_path)
        
        else:
            print('$2 其他事件 : ', event.src_path, " : ", event.event_type)
            pass
    
            

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
    
    print("watchdog:: start watching... ")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()


if __name__ == "__main__":
    
    config_file_path = "config.json"
    initialize(config_file_path)
    start_watching()