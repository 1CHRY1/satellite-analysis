from ..application.provider import get_satellite_database, get_tile_database, get_minio_client
import os

class URLUtil:
    
    @staticmethod
    def resolve(object_url: str):

        endpoint = get_minio_client().endpoint
        full_url = f"http://{endpoint}{object_url}"
        return full_url
    
    @staticmethod
    def dataUrl(path: str) -> str:
        path = path.lstrip('/').lstrip('\\')
        dir_path = os.path.join(".", "data")
        if not os.path.exists(dir_path):
            os.makedirs(dir_path, exist_ok=True)
        return os.path.join(dir_path, path)
    
    @staticmethod
    def outputUrl(path: str) -> str:
        path = path.lstrip('/').lstrip('\\')
        dir_path = os.path.join(".", "output")
        if not os.path.exists(dir_path):
            os.makedirs(dir_path, exist_ok=True)
        return os.path.join(dir_path, path)
    pass