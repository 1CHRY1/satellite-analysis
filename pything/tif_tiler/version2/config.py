import os

MINIO_CONFIG = {
    "dev": {
        "endpoint": "223.2.34.8:30900",
        "access_key": "minioadmin",
        "secret_key": "minioadmin",
        "bucket": "temp-files",
        "dir": "mosaicjson",
    },
    "K8s": {
        "endpoint": "minio.example.com:9000",
        "access_key": "K8s-user",
        "secret_key": "K8s-password",
        "bucket": "K8s-files",
        "dir": "K8s-mosaicjson",
    },
    "Vmod": {
        "endpoint": "minio.example.com:9000",
        "access_key": "Vmod-user",
        "secret_key": "Vmod-password",
        "bucket": "Vmod-files",
        "dir": "Vmod-mosaicjson",
    },
}

# 改这里
CURRENT_ENV = "dev"  #
minio_config = MINIO_CONFIG[CURRENT_ENV]

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), 'router', "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()