from flask import Flask
from flask_cors import CORS

from dataProcessing.Utils.cronFuncUtils import delete_old_objects, reset_scheduler
from dataProcessing.app.routes import bp
from apscheduler.schedulers.background import BackgroundScheduler

from dataProcessing.model.methlib import init_handler_factory

cron_scheduler = BackgroundScheduler()


def create_app(profile=None):
    """
    创建Flask应用
    :param profile: 配置环境 ('k8s', 'prod')
    """

    # --------- Create Flask App -------------------------------
    app = Flask('Satellite Processing Service')
    
    # --------- Set Global Config ------------------------------
    app.register_blueprint(bp)
    CORS(app)
    create_cron_scheduler()
    init_handler_factory()

    return app


def create_cron_scheduler():
    # --------- Create Cron Jobs -------------------------------
    global cron_scheduler
    if not cron_scheduler.running:
        # 暂时屏蔽以下任务
        # cron_scheduler.add_job(delete_old_objects, 'cron', hour=0, minute=0)  # 每天 00:00 运行
        # cron_scheduler.add_job(reset_scheduler, 'cron', minute='*/30')
        cron_scheduler.start()
