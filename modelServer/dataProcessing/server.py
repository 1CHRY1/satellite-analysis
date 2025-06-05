from dataProcessing import config
from dataProcessing.app import create_app
import ray

# 初始化ray
def init_ray():
    try:
        ray.init(address='auto')
        for node in ray.nodes():
            print(node)
        print("Connected to existing Ray cluster")
    except ConnectionError:
        ray.init()
        print("Started new Ray Head Node")

######################################################################
app = create_app()
if __name__ == '__main__':
    from dataProcessing.model.scheduler import init_scheduler
    init_ray()
    scheduler = init_scheduler()
    app.run(host="0.0.0.0", port=config.APP_PORT, debug=False)
