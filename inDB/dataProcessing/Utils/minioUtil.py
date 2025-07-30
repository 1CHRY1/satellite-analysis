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
        print(f"[INFO] Uploading file to bucket '{bucketName}' as '{objectName}'.")
        client.put_object(
            bucketName,
            objectName,
            buffer,
            dataLength
        )
        print(f"[SUCCESS] File has been successfully uploaded to bucket '{bucketName}' as '{objectName}'.")
        return True

    except S3Error as e:
        print(f"\033[91m[ERROR] Error occurred: {e}\033[0m")
        # ✅ CHANGED: fallback when incomplete body or other stream-related issues occur
        if "IncompleteBody" in str(e) or "not enough data" in str(e):
            try:
                print("[INFO] Retrying with length=-1 and stream mode...")  # ✅ CHANGED
                buffer.seek(0)  # ✅ CHANGED
                client.put_object(
                    bucketName,
                    objectName,
                    data=buffer,
                    length=-1,
                    part_size=64 * 1024 * 1024  # 64MB，最多支持64G左右文件
                )
                print(f"[SUCCESS] Fallback upload successful for '{objectName}'.")  # ✅ CHANGED
                return True
            except Exception as retry_e:
                print(f"\033[91m[ERROR] Fallback upload failed: {retry_e}\033[0m")  # ✅ CHANGED
        return False

def uploadLocalFile(filePath: str, bucketName: str, objectName: str):
    with open(filePath, "rb") as file_data:
        file_bytes = file_data.read()
        dataLength = len(file_bytes)
        buffer_stream = io.BytesIO(file_bytes)
        buffer_stream.seek(0)
        return uploadFileToMinio(buffer_stream, dataLength, bucketName, objectName)


def upload_file_by_mc(file_path, bucket, object_prefix):
    cmd = ["mc", "mirror", file_path, f"myminio/{bucket}/{object_prefix}"]
    with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1,
                          universal_newlines=True) as process:
        for line in process.stdout:
            print(f"[INFO] {line}", end="")  # line already has newline
        for err in process.stderr:
            print(f"\033[91m[ERROR] {err}\033[0m", end="")  # line already has newline
    process.wait()
