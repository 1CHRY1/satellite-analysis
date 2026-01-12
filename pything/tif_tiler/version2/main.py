import os

# 1. 开启 HTTP 范围请求合并（减少请求次数）
os.environ["GDAL_HTTP_MERGE_CONSECUTIVE_RANGES"] = "YES"
# 2. 开启 HTTP 多路复用（需要 GDAL 3.2+ 和支持 HTTP/2 的 libcurl）
os.environ["GDAL_HTTP_MULTIPLEX"] = "YES"
# 3. 设置缓存大小（例如设置 1024MB，根据你的服务器内存调整）
os.environ["GDAL_CACHEMAX"] = "1024" 
# 4. 优化 Minio 这种 S3 存储的读取
os.environ["GDAL_DISABLE_READDIR_ON_OPEN"] = "EMPTY_DIR" # 加快打开速度
os.environ["CPL_VSIL_CURL_ALLOWED_EXTENSIONS"] = ".tif,.tiff,.ovr" # 减少无效探测

# 1. 增加头信息预读量（高分影像 IFD 很大，设置到 128KB）
os.environ["GDAL_INGESTED_BYTES_AT_OPEN"] = "131072"
# 2. 开启并行下载（GDAL 3.2+ 关键设置）
os.environ["GDAL_HTTP_MERGE_CONSECUTIVE_RANGES"] = "YES"
os.environ["GDAL_HTTP_MULTIPLEX"] = "YES"
# 3. 强制使用异步连接池
os.environ["CPL_VSIL_CURL_CHUNK_SIZE"] = "1048576" # 1MB

# 4. 在代码中限制并发线程数，避免压垮 Minio
# 在 run_mosaic_sync 中设置 threads=5 到 8 即可，不要太高

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
    from router import rgb, terrain, oneband, mosaic, no_cloud, no_cloud_with_sensorname, image_visualization, on_the_fly_exploration_mosaic, mosaic_single, edge_image_visualization, image_visualization_backup_0801, for_deployment, deployment_with_nodata_question_backup, image_visualization_without_national_scale,image_visualization_without_national_scale_v2, mosaicjson_visualization




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
    # Add a welcome message for the root endpoint
    @app.get("/")
    def read_index():
        return {"message": "Welcome to TiTiler"}
    
    return app

app = createApp()