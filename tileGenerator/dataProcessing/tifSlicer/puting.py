###############################################################
import math

EarthRadius = 6371008.8
EarthCircumference = 2 * math.pi * EarthRadius
Resolution = 1000.0 # 1km

tile_bucket = "tile-bucket"
cloud = 0.0
object_prefix = "object-prefix"
world_grid_num = [math.ceil(EarthCircumference / Resolution), math.ceil(EarthCircumference / 2.0 / Resolution)]

# 导入Shapely库用于WKT验证
from shapely import wkt
from shapely.geometry import Polygon

def get_tile_info(tile_path):
    
    # 从路径中提取grid_id_x, grid_id_y
    grid_id_x = int(tile_path.split("/")[-1].split("_")[1])
    grid_id_y = int(tile_path.split("/")[-1].split("_")[2].split(".")[0])
    scene_name = tile_path.split("/")[-3]
    band_name = tile_path.split("/")[-2]
    
    # 根据tile_path, return tile_info
    tile_info = {}
    left_lng, top_lat = grid2lnglat(grid_id_x, grid_id_y, world_grid_num)
    right_lng, bottom_lat = grid2lnglat(grid_id_x + 1, grid_id_y + 1, world_grid_num)
    wgs84_corners = [(left_lng, top_lat), (right_lng, top_lat), (right_lng, bottom_lat), (left_lng, bottom_lat),
                     (left_lng, top_lat)]
    coords_str = ", ".join([f"{x} {y}" for x, y in wgs84_corners])
    wkt_polygon = f"POLYGON(({coords_str}))"
    
    tile_info['bbox'] = wkt_polygon
    tile_info['bucket'] = tile_bucket
    tile_info['cloud'] = cloud
    tile_info['path'] = f"{object_prefix}/{scene_name}/{band_name}/tile_{grid_id_x}_{grid_id_y}-{tile_info['cloud']:.2f}.tif"
    tile_info['column_id'] = grid_id_x
    tile_info['row_id'] = grid_id_y
    return tile_info
    # 这个里面可以不用写上传的




##### Helper Functions #####
def grid2lnglat(grid_x, grid_y, world_grid_num):
    """return the left-top geo position of the grid"""
    lng = grid_x / world_grid_num[0] * 360.0 - 180.0
    lat = 90.0 - grid_y / world_grid_num[1] * 180.0
    return lng, lat # left top (lng, lat)


def validate_wkt_polygon(wkt_str):
    """验证WKT多边形字符串是否有效"""
    try:
        # 尝试解析WKT字符串
        geom = wkt.loads(wkt_str)
        # 检查是否为多边形
        if not isinstance(geom, Polygon):
            return False, f"不是多边形，而是{type(geom).__name__}"
        # 检查多边形是否有效
        if not geom.is_valid:
            return False, f"多边形无效: {geom.explain_validity()}"
        return True, "多边形有效"
    except Exception as e:
        return False, f"解析错误: {str(e)}"
  
    
def test_wkt_validation():
    """测试WKT多边形验证"""
    # 测试有效的多边形
    valid_polygon = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))"
    is_valid, message = validate_wkt_polygon(valid_polygon)
    print(f"有效多边形测试: {message}")
    
    # 测试无效的多边形（不闭合）
    invalid_polygon = "POLYGON((0 0, 0 1, 1 1, 1 0))"
    is_valid, message = validate_wkt_polygon(invalid_polygon)
    print(f"无效多边形测试: {message}")
    
    # 测试get_tile_info生成的WKT
    test_tile = get_tile_info(1, 1, [500, 250])
    print(f"生成的WKT: {test_tile['bbox']}")
    is_valid, message = validate_wkt_polygon(test_tile['bbox'])
    print(f"生成的WKT验证: {message}")


if __name__ == "__main__":
    test_tile_path = "xxx/GF1XXXX/band_1/tile_33257_6550.tif"
    tile_info = get_tile_info(test_tile_path)
    print(tile_info)
    
    