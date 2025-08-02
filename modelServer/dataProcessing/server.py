import os
if "PROJ_LIB" in os.environ:
    del os.environ["PROJ_LIB"]
from dataProcessing.app import create_app
from dataProcessing.config import current_config as CONFIG
import ray

# 初始化ray
def init_ray():
    try:
        ray.init(
            address='auto',
            # 可预留内存：12GB（约60%）
            # 用于任务执行过程中的内存分配
            _memory=CONFIG.RAY_MEMORY,
            
            # 启用资源隔离（推荐）
            enable_resource_isolation=True,
            
            # 系统预留内存：2GB
            # Ray系统进程预留内存，剩余内存约2GB给系统
            system_reserved_memory=CONFIG.RAY_SYSTEM_RESERVED_MEMORY,
            system_reserved_cpu=CONFIG.RAY_SYSTEM_RESERVED_CPU,  # 至少 0.5
        )
        # ray.init(address='auto')
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
    app.run(host="0.0.0.0", port=CONFIG.APP_PORT, debug=CONFIG.APP_DEBUG)
