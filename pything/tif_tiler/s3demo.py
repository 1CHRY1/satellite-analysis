
# ########### Testing MinIO S3 Client ###########
# import boto3
# from botocore.exceptions import NoCredentialsError, PartialCredentialsError

# # MinIO 配置
# MINIO_ENDPOINT = "223.2.34.7:9000"
# MINIO_ACCESS_KEY = "jTbgNHEqQafOpUxVg7Ol"
# MINIO_SECRET_KEY = "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
# MINIO_SECURE = False  # 如果使用 HTTPS，请设置为 True
# MINIO_BUCKET = "test-images"
# OBJECT_NAME = "landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B1.TIF"


# # 创建 S3 客户端
# s3 = boto3.client(
#     's3',
#     endpoint_url=f'http://{MINIO_ENDPOINT}',
#     aws_access_key_id=MINIO_ACCESS_KEY,
#     aws_secret_access_key=MINIO_SECRET_KEY,
#     use_ssl=MINIO_SECURE
# )

# try:
#     # 列出所有存储桶
#     response = s3.list_buckets()
#     print("存储桶列表:")
#     for bucket in response['Buckets']:
#         print(f'  - {bucket["Name"]}')

#     # 测试列出某个存储桶中的对象
#     bucket_name = "test-images"  # 替换为你的存储桶名称
#     objects = s3.list_objects_v2(Bucket=bucket_name)
#     print(f"\n'{bucket_name}' 存储桶中的对象:")
#     if 'Contents' in objects:
#         for obj in objects['Contents']:
#             print(f'  - {obj["Key"]}')
#     else:
#         print("该存储桶为空或不存在。")
    
#     s3.download_file(Bucket=MINIO_BUCKET, Key=OBJECT_NAME, Filename="D:\\t\\4\\wwhat.TIF")
# except (NoCredentialsError, PartialCredentialsError):
#     print("凭证错误，请检查你的 MinIO 配置。")
# except Exception as e:
#     print(f"发生错误: {e}")
    
    
########## Testing Rasterio Open S3 ##########
import rasterio
from rasterio.session import AWSSession
import boto3
import os


# MinIO 配置
MINIO_ENDPOINT = "http://223.2.34.7:9000"
MINIO_ACCESS_KEY = "jTbgNHEqQafOpUxVg7Ol"
MINIO_SECRET_KEY = "7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M"
MINIO_BUCKET = "test-images"
OBJECT_NAME = "landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B1.TIF"


# 设置环境变量
os.environ["AWS_ACCESS_KEY_ID"] = MINIO_ACCESS_KEY
os.environ["AWS_SECRET_ACCESS_KEY"] = MINIO_SECRET_KEY
os.environ["AWS_S3_ENDPOINT"] = MINIO_ENDPOINT
os.environ["AWS_HTTPS"] = "NO"
os.environ["AWS_VIRTUAL_HOSTING"] = "FALSE"

# 创建 S3 Session
session = boto3.Session(
    aws_access_key_id=MINIO_ACCESS_KEY,
    aws_secret_access_key=MINIO_SECRET_KEY
)
aws_session = AWSSession(session, region_name="", endpoint_url=MINIO_ENDPOINT)

cog_url = f"{MINIO_ENDPOINT}/{MINIO_BUCKET}/{OBJECT_NAME}"


# 读取 COG 影像
with rasterio.Env(aws_session):
    with rasterio.open(cog_url) as src:
        print(f"影像尺寸: {src.width} x {src.height}")
        print(f"投影: {src.crs}")
