import io
import os

from minio import Minio
from minio.error import S3Error
from osgeo import gdal
from dataProcessing.config import current_config as CONFIG


def getMinioClient():
    client = Minio(
        f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}",
        access_key=CONFIG.MINIO_ACCESS_KEY,
        secret_key=CONFIG.MINIO_SECRET_KEY,
        secure=CONFIG.MINIO_SECURE
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

import os
import io
from minio.error import S3Error

# 定义一个通用的上传函数
def uploadMemFile(source, bucket_name, object_name):
    """
    统一上传入口，支持本地文件路径(str)和内存文件对象(MemoryFile, BytesIO等)
    
    :param source: 文件路径字符串 或 类似文件的对象(必须支持 read/seek/tell)
    :param bucket_name: 桶名称
    :param object_name: 上传到 Minio 的 key
    """
    client = getMinioClient()
    
    # 1. 确保 Bucket 存在
    try:
        if not client.bucket_exists(bucket_name):
            client.make_bucket(bucket_name)
    except S3Error as e:
        print(f"Check bucket error: {e}")
        return

    # 2. 准备数据流和长度
    data_stream = None
    data_length = 0
    file_to_close = None  # 用于标记是否需要手动关闭文件

    try:
        if isinstance(source, str):
            # --- 情况 A: 传入的是本地文件路径 ---
            # 使用 os.stat 获取文件大小，避免读取整个文件到内存
            try:
                data_length = os.stat(source).st_size
                # 打开文件流
                data_stream = open(source, "rb")
                file_to_close = data_stream
            except OSError as e:
                print(f"File path error: {e}")
                return
        else:
            # --- 情况 B: 传入的是内存对象 (MemoryFile, BytesIO) ---
            data_stream = source
            
            # 计算流的大小
            # 方法1: 如果有 getbuffer (BytesIO, MemoryFile通常有)，直接获取最高效
            if hasattr(data_stream, 'getbuffer'):
                data_length = data_stream.getbuffer().nbytes
            else:
                # 方法2: 使用 seek/tell 获取大小 (通用方法)
                current_pos = data_stream.tell()
                data_stream.seek(0, 2)  # 移动到末尾
                data_length = data_stream.tell()
                data_stream.seek(current_pos)  # 恢复位置
                
            # 确保指针在起始位置 (虽然你在外部 seek(0) 了，这里加一层保险)
            if data_stream.tell() != 0:
                data_stream.seek(0)

        # 3. 执行上传
        # Minio 的 put_object 可以直接流式上传，不需要像你之前那样 read() 成 bytes
        client.put_object(
            bucket_name,
            object_name,
            data_stream,
            data_length
        )
        print(f"Upload successful: {bucket_name}/{object_name} (Size: {data_length})")

    except S3Error as e:
        print(f"Minio upload error: {e}")
    except Exception as e:
        print(f"Unexpected error: {e}")
    finally:
        # 只有是我们内部打开的文件(情况A)才需要关闭
        # 外部传入的 MemoryFile(情况B) 由外部的 with 语句管理生命周期
        if file_to_close:
            file_to_close.close()


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
# TODO
def configure_minio_access4gdal(use_https=False):
    # 重置所有配置
    gdal.SetConfigOption('AWS_NO_SIGN_REQUEST', 'NO')

    # 基本配置
    gdal.SetConfigOption('AWS_ACCESS_KEY_ID', CONFIG.MINIO_ACCESS_KEY)
    gdal.SetConfigOption('AWS_SECRET_ACCESS_KEY', CONFIG.MINIO_SECRET_KEY)
    gdal.SetConfigOption('AWS_VIRTUAL_HOSTING', f"{CONFIG.MINIO_SECURE}")

    # 设置协议和端点
    protocol = 'https' if use_https else 'http'
    gdal.SetConfigOption('AWS_S3_ENDPOINT', f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}")
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
