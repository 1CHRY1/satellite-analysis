from dataProcessing import config
from dataProcessing.app import create_app

######################################################################
app = create_app()
if __name__ == '__main__':
    from dataProcessing.model.scheduler import init_scheduler

    scheduler = init_scheduler()
    app.run(host="0.0.0.0", port=config.APP_PORT, debug=False)
