import os
if "PROJ_LIB" in os.environ:
    del os.environ["PROJ_LIB"]
from dataProcessing.app import create_app
from dataProcessing.config import current_config as CONFIG
import ray

# 初始化ray
def init_ray():
    if ray.is_initialized():
        print("Ray已经初始化，跳过重复初始化")
        return
    
    try:    
        ray.init(
            address='auto',
            # 可预留内存：12GB（约60%）
            # 用于任务执行过程中的内存分配
            # _memory=CONFIG.RAY_MEMORY, # 集群不需要，因为已经在ray start中声明
            
            # 启用资源隔离（推荐）
            # enable_resource_isolation=True,
            
            # 系统预留内存：2GB
            # Ray系统进程预留内存，剩余内存约2GB给系统
            # system_reserved_memory=CONFIG.RAY_SYSTEM_RESERVED_MEMORY,
            # system_reserved_cpu=CONFIG.RAY_SYSTEM_RESERVED_CPU,  # 至少 0.5
            runtime_env={
                "env_vars": {
                    "GDAL_DISABLE_READDIR_ON_OPEN": "EMPTY_DIR",
                    "GDAL_HTTP_MERGE_CONSECUTIVE_RANGES": "YES",
                    # "GDAL_NUM_THREADS": "ALL_CPUS",
                    "VSI_CACHE": "TRUE",
                    "VSI_CACHE_SIZE": "50000000",
                    "CPL_VSIL_CURL_ALLOWED_EXTENSIONS": ".tif",
                }
            }
        )
        # ray.init(address='auto')
        for node in ray.nodes():
            print(node)
        print("Connected to existing Ray cluster")
    except ConnectionError:
        ray.init(num_cpus=CONFIG.RAY_NUM_CPUS, ignore_reinit_error=True)
        print(f"Started new Ray Head Node with {CONFIG.RAY_NUM_CPUS} CPUs")

######################################################################
app = create_app()
if __name__ == '__main__':
    from dataProcessing.model.scheduler import init_scheduler
    init_ray()
    scheduler = init_scheduler()
    app.run(host="0.0.0.0", port=CONFIG.APP_PORT, debug=CONFIG.APP_DEBUG)
    # app.run(host="0.0.0.0", port=5001, debug=CONFIG.APP_DEBUG)
