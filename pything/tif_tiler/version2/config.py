import os

COMMON_CONFIG = {
    "dev": {
        "create_no_cloud_config_url": "http://192.168.1.127:8999/api/v3/modeling/example/scenes/visualization"
    },
    "k8s": {
        "create_no_cloud_config_url": "http://223.2.34.8:31584/api/v3/modeling/example/scenes/visualization"
    },
    "vmod": {
        "create_no_cloud_config_url": "http://172.31.13.21:8999/api/v3/modeling/example/scenes/visualization"
    }
}
MINIO_CONFIG = {
    "dev": {
        "endpoint": "223.2.34.8:30900",
        "access_key": "minioadmin",
        "secret_key": "minioadmin",
        "bucket": "temp-files",
        "dir": "mosaicjson",
    },
    "k8s": {
        "endpoint": "223.2.34.8:30900",
        "access_key": "minioadmin",
        "secret_key": "minioadmin",
        "bucket": "temp-files",
        "dir": "mosaicjson",
    },
    "vmod": {
        "endpoint": "172.31.13.21:9000",
        "access_key": "OGMS",
        "secret_key": "ogms250410",
        "bucket": "temp-files",
        "dir": "mosaicjson",
    },
}

# 改这里
CURRENT_ENV = "dev"  #
minio_config = MINIO_CONFIG[CURRENT_ENV]
common_config = COMMON_CONFIG[CURRENT_ENV]

TRANSPARENT_PNG = os.path.join(os.path.dirname(__file__), 'router', "transparent.png")
with open(TRANSPARENT_PNG, "rb") as f:
    TRANSPARENT_CONTENT = f.read()