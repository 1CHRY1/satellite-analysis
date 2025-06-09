def createApp():
    
    # 😋 If some error about PROJ_DATA, you can set environment variable as follows: 
    # import os
    # os.environ['PROJ_DATA'] = r'D:\env\miniconda\envs\grid\Lib\site-packages\rasterio\proj_data'
    
    from fastapi import FastAPI
    from titiler.core.factory import TilerFactory
    from starlette.middleware.cors import CORSMiddleware

    from router import rgb, terrain, oneband, mosaic

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

    # Add a welcome message for the root endpoint
    @app.get("/")
    def read_index():
        return {"message": "Welcome to TiTiler"}
    
    return app
    
    
app = createApp()