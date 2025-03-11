from Utils.mySqlUtils import *
from Utils.tifUtils import *
from Utils.geometryUtils import *
from Utils.osUtils import *

path = "D:\\1study\\Work\\2025_03_05_satellite\\tileGenerator"

def getTilePath(band):
    return f'{path}\\tiles\\{band}'

# insert_sensor("landsat7","Landsat7卫星","")
# insert_product("landsat7", "landsat7_product", "")

band1 = "B1"
band2 = "B2"
band3 = "B3"
def getTifPath(band):
    return f'{path}\\landset\\LE07_L1TP_122039_20210212_20210212_01_RT_{band}.TIF'

tif_buffer, tif_dataLen = generateColorfulTile(getTifPath(band3), getTifPath(band2), getTifPath(band3))
uploadFileToMinio(tif_buffer, tif_dataLen, "landsat7", "landsat7_origin.TIF")
png_buffer, png_dataLen = tif2Png(getTifPath("Origin"))
uploadFileToMinio(png_buffer, png_dataLen, "landsat7", "landsat7_origin.png")

getFileFromMinio("landsat7", "landsat7_origin.TIF", "E:/DownLoads/landsat7.TIF")