# import os
# os.environ['PROJ_LIB'] = r'D:\env\tiler\Library\share\proj'
# print(os.environ['PROJ_LIB'])


# from rasterio.crs import CRS
# WEB_MERCATOR_CRS = CRS.from_epsg(3857)
# WGS84_CRS = CRS.from_epsg(4326)


# from fastapi import FastAPI
# from starlette.middleware.cors import CORSMiddleware

# from router import rgb, terrain, oneband, mosaic, no_cloud

import numpy as np
arr = np.array([3.7, 255.9, -1.2, 377])
arr_uint8 = np.uint8(np.floor(arr.clip(0, 255)))
print(arr_uint8)  # 输出: [  3 255   0]