from shapely.geometry import box, Point


def ifPointContained(lon, lat, bounds):
    boundary = box(bounds.left, bounds.bottom, bounds.right, bounds.top)
    point = Point(lon, lat)
    return boundary.contains(point)