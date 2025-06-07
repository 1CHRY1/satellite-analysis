import pymysql
import uuid
import time
import socket
import os
import random
from dataProcessing.config import current_config as CONFIG
from datetime import datetime

namespace = uuid.NAMESPACE_DNS


def connect_mysql(host, port, database, user, password):
    global cursor, connection
    try:
        connection = pymysql.connect(
            host=host,
            port=port,
            database=database,
            user=user,
            password=password,
            cursorclass=pymysql.cursors.DictCursor
        )
        cursor = connection.cursor()
        return connection, cursor
    except pymysql.Error as err:
        print(f"Error connecting to MySQL: {err}")
        if err.errno == pymysql.err.ER_ACCESS_DENIED_ERROR:
            print("Invalid username or password")
        elif err.errno == pymysql.err.ER_BAD_DB_ERROR:
            print("Database does not exist")
        else:
            print(f"Error {err.errno}: {err.args[1]}")
        return None, None
    except Exception as e:
        print(f"Unexpected error: {e}")
        return None, None


def select_tile_by_column_and_row(tile_table_name, tiles, bands):
    """
    根据 columnId, rowId 和 bands 查询数据库中的瓦片信息，确保 columnId 和 rowId 成对匹配，按 band 分组返回结果。

    :param tile_table_name: str, 数据表名
    :param tiles: list, 包含多个 {"columnId": X, "rowId": Y} 的字典
    :param bands: list, 需要的波段列表
    :return: dict, 按 band 分组的查询结果
    """
    from collections import defaultdict

    connection, cursor = connect_mysql(CONFIG.MYSQL_HOST, CONFIG.MYSQL_TILE_PORT, CONFIG.MYSQL_TILE_DB, CONFIG.MYSQL_USER, CONFIG.MYSQL_PWD)

    # 生成 WHERE 条件，确保 columnId 和 rowId 必须是成对匹配的
    tile_conditions = " OR ".join(["(column_id = %s AND row_id = %s)"] * len(tiles))
    band_placeholders = ', '.join(['%s'] * len(bands))

    select_query = f"""
        SELECT * FROM {tile_table_name}
        WHERE ({tile_conditions}) 
        AND band IN ({band_placeholders})
    """

    # 构建参数列表
    tile_params = []
    for tile in tiles:
        tile_params.extend([tile["columnId"], tile["rowId"]])

    query_params = tuple(tile_params + bands)

    # 执行查询
    cursor.execute(select_query, query_params)

    # 获取所有查询结果
    result = cursor.fetchall()

    # 将结果按 band 分组
    grouped_result = defaultdict(list)
    for tile in result:
        grouped_result[tile["band"]].append(tile)

    print(f"Found {len(result)} tiles matching the provided columnId, rowId pairs, and bands. Grouped by band.")

    cursor.close()
    connection.close()

    return dict(grouped_result)


def select_tile_by_column_and_row_v2(tiles, bands):
    """
    根据 columnId, rowId 和 bands 查询数据库中的瓦片信息，确保 columnId 和 rowId 成对匹配，按 band 分组返回结果。

    :param tiles: list, 包含多个 {"columnId": X, "rowId": Y, "sceneId": Z} 的字典
    :param bands: list, 需要的波段列表
    :return: dict, 按 band 分组的查询结果
    """
    from collections import defaultdict

    connection, cursor = connect_mysql(CONFIG.MYSQL_HOST, CONFIG.MYSQL_TILE_PORT, CONFIG.MYSQL_TILE_DB, CONFIG.MYSQL_USER, CONFIG.MYSQL_PWD)

    # 按照 sceneId 分组查询，每个 sceneId 对应一个查询
    grouped_result = defaultdict(list)
    # tiles = filter_tiles(tiles)
    for tile in tiles:
        sceneId = tile.get("sceneId", "").lower()
        tile_table_name = sceneId  # 根据 sceneId 确定表名

        # 生成 WHERE 条件，确保 columnId 和 rowId 必须是成对匹配的
        tile_conditions = " OR ".join(["(column_id = %s AND row_id = %s)"])
        band_placeholders = ', '.join(['%s'] * len(bands))

        select_query = f"""
            SELECT * FROM {tile_table_name}
            WHERE ({tile_conditions}) 
            AND band IN ({band_placeholders})
        """

        # 构建参数列表
        tile_params = []
        tile_params.extend([tile["columnId"], tile["rowId"]])

        query_params = tuple(tile_params + bands)

        # 执行查询
        cursor.execute(select_query, query_params)

        # 获取所有查询结果
        result = cursor.fetchall()

        # 将结果按 band 分组
        for tile in result:
            grouped_result[tile["band"]].append(tile)

        print(f"Found {len(result)} tiles for sceneId '{sceneId}' matching the provided columnId, rowId pairs, and bands. Grouped by band.")

    cursor.close()
    connection.close()

    return dict(grouped_result)


def select_scene_time(sceneId):
    # 建立数据库连接
    connection, cursor = connect_mysql(CONFIG.MYSQL_HOST, CONFIG.MYSQL_RESOURCE_PORT, CONFIG.MYSQL_RESOURCE_DB, CONFIG.MYSQL_USER, CONFIG.MYSQL_PWD)
    # 查询scene_table中的scene_time
    query = f"SELECT scene_time FROM scene_table WHERE scene_id = %s"
    cursor.execute(query, (sceneId,))
    result = cursor.fetchone()
    cursor.close()
    connection.close()
    return result['scene_time']


def filter_tiles(tiles):
    # 用来存储过滤后的结果
    final_tiles = []

    # 临时字典存储按 columnId 和 rowId 分组的 tile
    tile_groups = {}

    # 根据 columnId 和 rowId 进行分组
    for tile in tiles:
        columnId = tile.get("columnId")
        rowId = tile.get("rowId")
        key = (columnId, rowId)  # 创建一个由 columnId 和 rowId 组成的唯一键

        if key not in tile_groups:
            tile_groups[key] = []
        tile_groups[key].append(tile)

    # 对每一组相同 columnId 和 rowId 的 tiles，进行 scene_time 比较，保留最接近的 tile
    for key, group in tile_groups.items():
        # 获取所有 tile 的 sceneId
        sceneIds = [tile["sceneId"] for tile in group]

        # 查询每个 sceneId 对应的 scene_time
        scene_times = {}
        for sceneId in sceneIds:
            scene_time = select_scene_time(sceneId)
            if scene_time:
                scene_times[sceneId] = scene_time

        # 按照 scene_time 排序，选择时间最接近的 tile
        current_time = datetime.now()
        group_sorted = sorted(group, key=lambda tile: abs(current_time - scene_times.get(tile["sceneId"], current_time)))
        final_tiles.append(group_sorted[0])  # 保留最接近的那个

    return final_tiles
