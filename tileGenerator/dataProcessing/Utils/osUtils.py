import io
import os
from minio import Minio
from minio.error import S3Error
from osgeo import gdal
import dataProcessing.config as config

def getMinioClient():
    client = Minio(
        f"{config.MINIO_IP}:{config.MINIO_PORT}",
        access_key=config.MINIO_ACCESS_KEY,
        secret_key=config.MINIO_SECRET_KEY,
        secure=config.MINIO_SECURE
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

def getFileFromMinio(bucketName, objectName, filePath):
    client = getMinioClient()
    found = client.bucket_exists(bucketName)
    if not found:
        print(f"Bucket '{bucketName}' does not exist.")
        return
    try:
        client.stat_object(bucketName, objectName)
    except S3Error as e:
        if e.code == 'NoSuchKey':
            print(f"Object '{objectName}' does not exist in bucket '{bucketName}'.")
            return
        else:
            raise
    client.fget_object(bucketName, objectName, filePath)

def uploadLocalFile(filePath: str, bucketName: str, objectName: str):
    with open(filePath, "rb") as file_data:
        file_bytes = file_data.read()
        dataLength = len(file_bytes)
        buffer_stream = io.BytesIO(file_bytes)
        uploadFileToMinio(buffer_stream, dataLength, bucketName, objectName)


def uploadLocalDirectory(directory_path, bucket_name, minio_directory_prefix=""):
    # 获取MinIO客户端
    client = getMinioClient()

    # 确保存储桶存在，如果不存在则创建
    if not client.bucket_exists(bucket_name):
        client.make_bucket(bucket_name)
        print(f"创建存储桶 {bucket_name}")

    upload_count = 0

    # 遍历本地目录
    for root, dirs, files in os.walk(directory_path):
        for file in files:
            # 获取完整的本地文件路径
            local_file_path = os.path.join(root, file)

            # 计算相对路径，用于在MinIO中构建对象路径
            relative_path = os.path.relpath(local_file_path, directory_path)

            # 构建MinIO中的对象名称
            if minio_directory_prefix:
                minio_object_name = os.path.join(minio_directory_prefix, relative_path).replace("\\", "/")
            else:
                minio_object_name = relative_path.replace("\\", "/")

            # 获取文件大小和MIME类型
            file_size = os.path.getsize(local_file_path)

            try:
                # 上传文件
                client.fput_object(
                    bucket_name,
                    minio_object_name,
                    local_file_path
                )
                print(f"已上传: {local_file_path} 到 {bucket_name}/{minio_object_name}")
                upload_count += 1
            except Exception as e:
                print(f"上传失败: {local_file_path}, 错误: {str(e)}")

    print(f"上传完成，共上传 {upload_count} 个文件到 {bucket_name} 存储桶")
    return upload_count

# gdal配置
def configure_minio_access4gdal(use_https=False):
    # 重置所有配置
    gdal.SetConfigOption('AWS_NO_SIGN_REQUEST', 'NO')

    # 基本配置
    gdal.SetConfigOption('AWS_ACCESS_KEY_ID', config.MINIO_ACCESS_KEY)
    gdal.SetConfigOption('AWS_SECRET_ACCESS_KEY', config.MINIO_SECRET_KEY)
    gdal.SetConfigOption('AWS_VIRTUAL_HOSTING', f"{config.MINIO_SECURE}")

    # 设置协议和端点
    protocol = 'https' if use_https else 'http'
    gdal.SetConfigOption('AWS_S3_ENDPOINT', f"{config.MINIO_IP}:{config.MINIO_PORT}")
    gdal.SetConfigOption('AWS_HTTPS', 'YES' if use_https else 'NO')

    # 设置超时
    gdal.SetConfigOption('GDAL_HTTP_TIMEOUT', '60')  # 设置60秒超时

    # 区域设置
    gdal.SetConfigOption('AWS_REGION', 'us-east-1')

    # 启用详细错误报告
    gdal.SetConfigOption('CPL_DEBUG', 'ON')

    # 启用VSI HTTP缓存
    gdal.SetConfigOption('VSI_CACHE', 'TRUE')
    gdal.SetConfigOption('VSI_CACHE_SIZE', '1000000')  # 设置缓存大小为1MB

    # 启用日志
    gdal.SetConfigOption('CPL_LOG', 'YES')
