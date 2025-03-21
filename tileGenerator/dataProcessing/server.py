import config
from dataProcessing.Utils.osUtils import configure_minio_access4gdal, uploadFileToMinio, uploadLocalFile
from dataProcessing.app import create_app

######################################################################

if __name__ == '__main__':
    # 配置gdal
    configure_minio_access4gdal()
    app = create_app()
    app.run(host = "0.0.0.0", port = config.APP_PORT, debug = config.APP_DEBUG)
