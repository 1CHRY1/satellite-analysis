import io
import subprocess

from minio import Minio
from minio.error import S3Error
DB_CONFIG = {}

def set_initial_minio_config(D_CONFIG):
    global DB_CONFIG
    DB_CONFIG = D_CONFIG

def getMinioClient():
    global DB_CONFIG
    client = Minio(
        f"{DB_CONFIG['MINIO_IP']}:{DB_CONFIG['MINIO_PORT']}",
        access_key=DB_CONFIG['MINIO_ACCESS_KEY'],
        secret_key=DB_CONFIG['MINIO_SECRET_KEY'],
        secure=DB_CONFIG['MINIO_SECURE']
    )
    return client

def file_exists(bucketName, objectName):
    client = getMinioClient()
    try:
        client.stat_object(bucketName, objectName)
        return True
    except S3Error as e:
        # print(f"\033[91m[ERROR] File not found: {objectName}\033[0m")
        return False