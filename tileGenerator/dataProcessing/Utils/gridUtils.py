import math
from shapely.geometry import box

column_num = 8000
row_num = 8000

# Get Position(column_id and row_id) by Lat and Lon
def getPositionByPoint(lon, lat):
    column_id = math.floor( (lon + 180) / 360 * column_num )
    row_id = math.floor( (1- (lat + 90) / 180) * row_num )
    return column_id, row_id

# Get Grid BoundingBox by Position(column_id and row_id)
def getBBoxByPosition(column_id, row_id):
    column_unit = 360 / column_num
    row_unit = 180 / row_num
    minx = column_id * column_unit - 180
    maxy = 90 - row_id * row_unit
    maxx = minx + column_unit
    miny = maxy - row_unit
    return box(minx, miny, maxx, maxy)

# Get All Grids BoundingBox by Image BoundingBox
def getGridsByBBox(bbox):
    bbox_list = []
    minx, miny, maxx, maxy = bbox.bounds
    column_left, row_top = getPositionByPoint(minx, maxy)
    column_right, row_bottom = getPositionByPoint(maxx, miny)
    for column in range(column_left, column_right+1):
        for row in range(row_top, row_bottom+1):
            bbox_list.append(getBBoxByPosition(column, row))

    return bbox_list


