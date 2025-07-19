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

    from router import rgb, terrain, oneband, mosaic, no_cloud



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

    # Add a welcome message for the root endpoint
    @app.get("/")
    def read_index():
        return {"message": "Welcome to TiTiler"}
    
    return app

app = createApp()