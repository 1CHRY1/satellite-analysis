from minio import Minio
from minio.error import S3Error

endpoint = "localhost:9000"
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