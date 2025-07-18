from minio import Minio
from minio.error import S3Error

class MinioClient:
    """A class for Minio client <read-only> operations"""
    def __init__(self, endpoint: str, access_key: str, secret_key: str, secure: bool):
        """Initialize the Minio client"""
        self.endpoint = endpoint
        self.client = Minio(
            endpoint, 
            access_key=access_key, 
            secret_key=secret_key, 
            secure=secure
        )

    def pull_file(self, bucket_name, object_name, output_file_path):
        """Pull file from storage"""
        found = self.client.bucket_exists(bucket_name)
        if not found:
            print(f"Bucket '{bucket_name}' does not exist.")
            return
        try:
            self.client.stat_object(bucket_name, object_name)
        except S3Error as e:
            print(f"Object '{object_name}' does not exist in bucket '{bucket_name}'.")
            return
        
        self.client.fget_object(bucket_name, object_name, output_file_path)
        
    def push_file(self, bucket_name, object_name, input_file_path):
        """Push file to storage"""
        try:
            # check if bucket exists
            found = self.client.bucket_exists(bucket_name)
            if not found:
                print(f"Bucket '{bucket_name}' does not exist. !! Create it !!")
                self.client.make_bucket(bucket_name)
                
            # check if object exists
            try:
                self.client.stat_object(bucket_name, object_name)
                raise ValueError(f"Data with path {object_name} already exists in bucket {bucket_name}.")
            except S3Error:
                pass  # if object not exists, continue

            self.client.fput_object(
                bucket_name,
                object_name,
                input_file_path
            )

            print(f"File was pushed to bucket '{bucket_name}' as '{object_name}'.")

        except S3Error as e:
            print(f"Error occurred: {e}")
            raise e
        
    def push_file_from_bytes(self, bucket_name, object_name, data, length):
        """Push file to storage from bytes"""
        try:
            # check if bucket exists
            found = self.client.bucket_exists(bucket_name)
            if not found:
                print(f"Bucket '{bucket_name}' does not exist. !! Create it !!")
                self.client.make_bucket(bucket_name)
                
            # check if object exists
            try:
                self.client.stat_object(bucket_name, object_name)
                raise ValueError(f"Data with path {object_name} already exists in bucket {bucket_name}.")
            except S3Error:
                pass  # if object not exists, continue
                
            self.client.put_object(bucket_name, object_name, data, length)

        except S3Error as e:
            print(f"Error occurred: {e}")
            raise e
        
    def delete_file(self, bucket_name, object_name):
        """Delete object from storage"""
        try:
            self.client.remove_object(bucket_name, object_name)
        except S3Error as e:
            print(f"Error occurred: {e}")
            raise e
