import config
from dataProcessing.app import create_app

######################################################################
app = create_app()
if __name__ == '__main__':
    from dataProcessing.model.scheduler import init_scheduler

    # 配置gdal
    # configure_minio_access4gdal()
    scheduler = init_scheduler()
    app.run(host="0.0.0.0", port=config.APP_PORT, debug=config.APP_DEBUG)
