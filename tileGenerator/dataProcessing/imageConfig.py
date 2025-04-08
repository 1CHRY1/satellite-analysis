import re

# Landsat8配置
LANDSAT8_SYNTHESIS_BANDS                        =       ['B4', 'B3', 'B2']
LANDSAT8_LC08_L2SP_PATTERN                      =       re.compile(r"LC08_L2\w{2}_(\d{3})(\d{3})_(\d{8})_\d{8}_\d{2}_T1(?:_SR)?_B(\d+)\.TIF", re.IGNORECASE)
LANDSAT8_PERIOD                                 =       '16d'

# Landsat7配置
LANDSAT7_SYNTHESIS_BANDS                        =       ['B3', 'B2', 'B1']
LANDSAT7_LE07_L1TP_PATTERN                      =       re.compile(r"LE07_L1\w{2}_(\d{3})(\d{3})_(\d{8})_\d{8}_\d{2}_RT_B(\d+(?:_VCID_\d)?)\.TIF", re.IGNORECASE)
LANDSAT7_PERIOD                                 =       '16d'

# 现在入库产品的配置
CUR_SYNTHESIS_BANDS                             =       LANDSAT8_SYNTHESIS_BANDS
CUR_HAS_QA_PIXEL                                =       True
CUR_PATTERN                                     =       LANDSAT8_LC08_L2SP_PATTERN
CUR_NO_DATA_VALUE                               =       0
CUR_PERIOD                                      =       LANDSAT8_PERIOD
CUR_TILE_LEVEL                                  =       '40031*20016'
