from ..application.provider import get_satellite_database, get_tile_database, get_minio_client

class URLUtil:
    
    @staticmethod
    def resolve(object_url: str):

        endpoint = get_minio_client().endpoint
        full_url = f"http://{endpoint}{object_url}"
        return full_url
    
    pass