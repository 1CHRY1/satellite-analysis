from minio import Minio
from minio.error import S3Error
from osgeo import gdal

endpoint = "223.2.34.7:9000"
accessKey = "jTbgNHEqQafOpUxVg7Ol"
secretKey = "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
secure = False

def getMinioClient():
    client = Minio(
        endpoint,
        access_key=accessKey,
        secret_key=secretKey,
        secure=False
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

# 为gdal配置MinIO S3访问
def configure_minio_access4gdal(use_https=False):
    # 重置所有配置
    gdal.SetConfigOption('AWS_NO_SIGN_REQUEST', 'NO')

    # 基本配置
    gdal.SetConfigOption('AWS_ACCESS_KEY_ID', accessKey)
    gdal.SetConfigOption('AWS_SECRET_ACCESS_KEY', secretKey)
    gdal.SetConfigOption('AWS_VIRTUAL_HOSTING', 'FALSE')

    # 设置协议和端点
    protocol = 'https' if use_https else 'http'
    gdal.SetConfigOption('AWS_S3_ENDPOINT', endpoint)
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
