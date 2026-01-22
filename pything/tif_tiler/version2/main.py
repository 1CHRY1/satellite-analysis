import os

# --- 1. 网络连接优化 (安全，强烈推荐) ---
# 合并相邻的读取请求，减少 HTTP 请求数 (MinIO 杀手级优化)
os.environ["GDAL_HTTP_MERGE_CONSECUTIVE_RANGES"] = "YES"

# 启用 HTTP/2 多路复用 (如果 MinIO 支持，速度起飞)
os.environ["GDAL_HTTP_MULTIPLEX"] = "YES"

# 增大 HTTP 超时容错 (防止网络波动导致 500 错误)
os.environ["GDAL_HTTP_TIMEOUT"] = "30"
os.environ["GDAL_HTTP_MAX_RETRY"] = "3"

# --- 2. 读取性能优化 ---
# 打开文件时预读 128KB，对于高分影像(GF)这种头文件较大的 COG 很有用
# 能减少一次额外的 HTTP GET
os.environ["GDAL_INGESTED_BYTES_AT_OPEN"] = "131072"

# 强制使用较大的块下载，减少碎片化 IO
os.environ["CPL_VSIL_CURL_CHUNK_SIZE"] = "1048576" # 1MB

# --- 3. 内存缓存 (根据机器配置调整) ---
# 注意：这是单进程缓存。如果你有 8 个 worker，总量是 256*8 = 2GB
os.environ["GDAL_CACHEMAX"] = "256"

def createApp():
    
    # 😋 If some error about PROJ_DATA, you can set environment variable as follows: 
    # import os
    # proj_lib_path = os.path.abspath(os.path.join(os.path.dirname(__file__)))
    # os.environ['PROJ_LIB'] = proj_lib_path
    # print(f"PROJ_LIB path: {os.environ['PROJ_LIB']}")

    # 😋 wtm直接屏蔽
    import os
    if "PROJ_LIB" in os.environ:
        del os.environ["PROJ_LIB"]

    from fastapi import FastAPI
    from titiler.core.factory import TilerFactory
    from starlette.middleware.cors import CORSMiddleware
    from router import rgb, terrain, oneband, mosaic, no_cloud, no_cloud_with_sensorname, image_visualization, on_the_fly_exploration_mosaic, mosaic_single, edge_image_visualization, image_visualization_backup_0801, for_deployment, deployment_with_nodata_question_backup, image_visualization_without_national_scale,image_visualization_without_national_scale_v2, mosaicjson_visualization, gif




    app = FastAPI()

    # Add CORS middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],  # Allows all origins (for development - be more specific in production)
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Create a TilerFactory for Cloud-Optimized GeoTIFFs
    cog = TilerFactory()

    # Register all the COG endpoints automatically
    app.include_router(cog.router, tags=["Cloud Optimized GeoTIFF"])
    app.include_router(rgb.router, tags=["RGB Composite Tiler"], prefix="/rgb")
    app.include_router(terrain.router, tags=["Terrain RGB Tiler"], prefix="/terrain")
    app.include_router(oneband.router, tags=["One Colorful Band Tiler"], prefix="/oneband")
    app.include_router(mosaic.router, tags=["Mosaic Tiler"], prefix="/mosaic")
    app.include_router(no_cloud.router, tags=["No Cloud Tiler"], prefix="/no_cloud")
    app.include_router(no_cloud_with_sensorname.router, tags=["No Cloud Tiler with SensorName"], prefix="/no_cloud_with_sensorname")
    app.include_router(image_visualization_without_national_scale_v2.router, tags=["Image Visualization Tiler"], prefix="/image_visualization")
    app.include_router(on_the_fly_exploration_mosaic.router, tags=["On the Fly Exploration Mosaic Tiler"], prefix="/on_the_fly_exploration_mosaic")
    app.include_router(mosaic_single.router, tags=["Mosaic Single Tiler"], prefix="/mosaic2")
    app.include_router(edge_image_visualization.router, tags=["Edge Image Visualization Tiler"], prefix="/edge_image_visualization")
    app.include_router(image_visualization_backup_0801.router, tags=["Image Visualization Backup 0801 Tiler"], prefix="/image_visualization_backup_0801")
    app.include_router(for_deployment.router, tags=["Deployment Tiler"], prefix="/for_deployment")
    # app.include_router(deployment_with_nodata_question.router, tags=["Deployment with Nodata Question Tiler"], prefix="/deployment_with_nodata_question")
    app.include_router(deployment_with_nodata_question_backup.router, tags=["Deployment with Nodata Question Backup Tiler"], prefix="/deployment_with_nodata_question_backup")
    # app.include_router(image_visualization_without_national_scale.router, tags=["Deployment without national scale loading"], prefix="/image_visualization_without_national_scale")
    app.include_router(mosaicjson_visualization.router, tags=["MosaicJSON Visualization"], prefix="/mosaicjson_visualization")
    app.include_router(gif.router, tags=["GIF Frame Tiler"], prefix="/gif")
    # Add a welcome message for the root endpoint
    @app.get("/")
    def read_index():
        return {"message": "Welcome to TiTiler"}
    
    return app

app = createApp()