from shapely.geometry import shape
def geojson_to_wkt(geojson_dict):
    # 仅支持单个多边形
    geom = shape(geojson_dict.get('features')[0].get('geometry'))
    return geom.wkt