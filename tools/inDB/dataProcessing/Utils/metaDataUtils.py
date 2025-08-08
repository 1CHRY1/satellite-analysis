def get_xml_content(file_path):
    # 使用二进制模式打开 XML 文件
    with open(file_path, 'rb') as file:
        content = file.read()
    xml_declaration = b'<?xml version="1.0" encoding="UTF-8"?>'
    xml_content = content[len(xml_declaration):]

    # 为文件内容包装一个虚拟的根元素
    wrapped_content = xml_declaration + b'<Root>' + xml_content + b'</Root>'
    return wrapped_content

def get_bbox_from_xml_node(node):
    top_left_latitude = float(node.find('TopLeftLatitude').text)
    top_left_longitude = float(node.find('TopLeftLongitude').text)
    top_right_latitude = float(node.find('TopRightLatitude').text)
    top_right_longitude = float(node.find('TopRightLongitude').text)
    bottom_right_latitude = float(node.find('BottomRightLatitude').text)
    bottom_right_longitude = float(node.find('BottomRightLongitude').text)
    bottom_left_latitude = float(node.find('BottomLeftLatitude').text)
    bottom_left_longitude = float(node.find('BottomLeftLongitude').text)

    # Find the minimum and maximum latitude values
    min_lat = min(top_left_latitude, top_right_latitude, bottom_right_latitude, bottom_left_latitude)
    max_lat = max(top_left_latitude, top_right_latitude, bottom_right_latitude, bottom_left_latitude)

    # Find the minimum and maximum longitude values
    min_lon = min(top_left_longitude, top_right_longitude, bottom_right_longitude, bottom_left_longitude)
    max_lon = max(top_left_longitude, top_right_longitude, bottom_right_longitude, bottom_left_longitude)
    # 从之前计算的结果中获取值
    left_lng = min_lon
    right_lng = max_lon
    bottom_lat = min_lat
    top_lat = max_lat
    # 按照要求的格式创建wgs84_corners
    wgs84_corners = [(left_lng, top_lat), (right_lng, top_lat), (right_lng, bottom_lat), (left_lng, bottom_lat),
                     (left_lng, top_lat)]
    coords_str = ", ".join([f"{x} {y}" for x, y in wgs84_corners])
    bbox = f"POLYGON(({coords_str}))"
    return bbox

def get_crs_from_xml_nodes(nodes):
    # TODO
    prj = nodes.find('MapProjection').text  # 获取 <MapProjection> 标签
    if prj == 'WGS84':
        crs = 4326
    else:
        crs = None
        print("找不到目标投影")
    return crs

# TODO --0 读取元数据，分波段存储image
# TODO --1 根据文件夹目录读取image和scene的信息,组成scene_info_list以及image_info_list
# TODO --2 遍历文件夹下所有的文件，得到tile的进一步信息，组成tile_info_list
# TODO --3 mc命令上传文件夹下所有的文件
# TODO --4 更新数据库

# TODO 上传景的XML文件到景一级目录下