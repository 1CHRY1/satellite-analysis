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


def uploadFileToMinio(buffer, dataLength, bucketName, objectName):
    client = getMinioClient()
    try:
        found = client.bucket_exists(bucketName)
        if not found:
            client.make_bucket(bucketName)
        else:
            print(f"Bucket '{bucketName}' already exists.")

        client.put_object(
            bucketName,
            objectName,
            buffer,
            dataLength
        )
        print(f"File has been successfully uploaded to bucket '{bucketName}' as '{objectName}'.")

    except S3Error as e:
        print(f"Error occurred: {e}")


def uploadLocalFile(filePath: str, bucketName: str, objectName: str):
    with open(filePath, "rb") as file_data:
        file_bytes = file_data.read()
        dataLength = len(file_bytes)
        buffer_stream = io.BytesIO(file_bytes)
        uploadFileToMinio(buffer_stream, dataLength, bucketName, objectName)


def upload_file_by_mc(file_path, bucket, object_prefix):
    cmd = ["mc", "mirror", file_path, f"myminio/{bucket}/{object_prefix}"]
    with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1,
                          universal_newlines=True) as process:
        for line in process.stdout:
            print(line, end="")  # 逐行输出，不额外换行
        for err in process.stderr:
            print("ERROR:", err, end="")  # 逐行输出错误信息
    process.wait()
