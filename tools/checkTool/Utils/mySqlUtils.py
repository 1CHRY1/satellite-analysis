# import pymysql
# import uuid
# import time
# import socket
# import os
# import random
# from datetime import datetime

# DB_CONFIG = {}
# namespace = uuid.NAMESPACE_DNS


# def set_initial_mysql_config(D_CONFIG):
#     global DB_CONFIG
#     DB_CONFIG = D_CONFIG


# def generate_custom_id(prefix, digit):
#     remaining_length = digit - len(prefix)  # 计算除去 prefix 后可用的长度
#     if remaining_length <= 0:
#         return prefix[:digit]  # 如果 prefix 已经超长，则直接截断返回

#     # 定义可用字符集 (类似UUID格式: 数字和小写字母)
#     characters = "abcdefghijklmnopqrstuvwxyz0123456789"
#     # 生成随机字符串
#     random_part = ''.join(random.choice(characters) for _ in range(remaining_length))
#     # 拼接前缀和随机部分
#     custom_id = prefix + random_part
#     return custom_id


# def connect_mysql(host, port, database, user, password):
#     global cursor, connection
#     try:
#         connection = pymysql.connect(
#             host=host,
#             port=port,
#             database=database,
#             user=user,
#             password=password,
#             cursorclass=pymysql.cursors.DictCursor
#         )
#         cursor = connection.cursor()
#         return connection, cursor
#     except pymysql.Error as err:
#         print(f"\033[91m[ERROR] Error connecting to MySQL: {err}\033[0m")
#         if err.errno == pymysql.err.ER_ACCESS_DENIED_ERROR:
#             print("[ERROR] Invalid username or password")
#         elif err.errno == pymysql.err.ER_BAD_DB_ERROR:
#             print("[ERROR] Database does not exist")
#         else:
#             print(f"\033[91m[ERROR] Error {err.errno}: {err.args[1]}\033[0m")
#         return None, None
#     except Exception as e:
#         print(f"\033[91m[ERROR] Unexpected error: {e}\033[0m")
#         return None, None

# def get_all_scenes():
#     global DB_CONFIG
#     connection, cursor = connect_mysql(
#         DB_CONFIG["MYSQL_HOST"], 
#         DB_CONFIG["MYSQL_RESOURCE_PORT"],
#         DB_CONFIG["MYSQL_RESOURCE_DB"], 
#         DB_CONFIG["MYSQL_USER"],
#         DB_CONFIG["MYSQL_PWD"]
#     )
    
#     batch_size = 1000  # 每批查询1000条数据
#     offset = 0
#     all_results = []
    
#     try:
#         while True:
#             select_query = f"SELECT *, ST_AsText(bounding_box, 'axis-order=long-lat') as bounding_box_wkt FROM scene_table ORDER BY scene_id ASC LIMIT {batch_size} OFFSET {offset}"
#             cursor.execute(select_query)
#             results = cursor.fetchall()
            
#             if not results:
#                 break
                
#             all_results.extend(results)
#             offset += batch_size
#     finally:
#         cursor.close()
#         connection.close()
        
#     return all_results

# def get_scenes_by_range(start_idx, end_idx):
#     """
#     根据传入的起止序号获取数据
#     start_idx: 起始序号 (从1开始)
#     end_idx: 结束序号
#     """
#     global DB_CONFIG
#     # 计算需要跳过多少条 (offset) 和 取多少条 (limit)
#     offset = start_idx - 1
#     limit = end_idx - start_idx + 1
    
#     connection, cursor = connect_mysql(
#         DB_CONFIG["MYSQL_HOST"], 
#         DB_CONFIG["MYSQL_RESOURCE_PORT"],
#         DB_CONFIG["MYSQL_RESOURCE_DB"], 
#         DB_CONFIG["MYSQL_USER"],
#         DB_CONFIG["MYSQL_PWD"]
#     )
    
#     results = []
#     try:
#         # 使用 SQL 的 LIMIT 和 OFFSET 直接筛选
#         select_query = f"""
#             SELECT *, ST_AsText(bounding_box, 'axis-order=long-lat') as bounding_box_wkt 
#             FROM scene_table 
#             ORDER BY scene_id ASC
#             LIMIT {limit} OFFSET {offset}
#         """
#         cursor.execute(select_query)
#         results = cursor.fetchall()
#     finally:
#         cursor.close()
#         connection.close()
        
#     return results

# def delete_scene_by_id(scene_id):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     delete_query = "DELETE FROM scene_table WHERE scene_id = %s"
#     cursor.execute(delete_query, (scene_id,))
#     connection.commit()
#     cursor.close()
#     connection.close()

# def update_scene_by_id(scene_id, new_wkt):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     sql = """
#         UPDATE scene_table
#         SET bounding_box = ST_GeomFromText(%s, 4326, 'axis-order=long-lat')
#         WHERE scene_id = %s
#     """
#     cursor.execute(sql, (new_wkt, scene_id))
#     connection.commit()
#     cursor.close()
#     connection.close()

# def delete_image_by_id(image_id):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     delete_query = "DELETE FROM image_table WHERE image_id = %s"
#     cursor.execute(delete_query, (image_id,))
#     connection.commit()
#     cursor.close()
#     connection.close()

# def get_images_by_scene_id(scene_id):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     select_query = "SELECT image_id, bucket, tif_path FROM image_table WHERE scene_id = %s"
#     cursor.execute(select_query, (scene_id,))
#     result = cursor.fetchall()
#     cursor.close()
#     connection.close()
#     return result

# def get_sensor_byName(sensorName):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     select_query = "SELECT sensor_id FROM sensor_table WHERE sensor_name = %s"
#     cursor.execute(select_query, (sensorName,))
#     result = cursor.fetchone()
#     cursor.close()
#     connection.close()
#     if result:
#         sensor_id = result['sensor_id']
#         return sensor_id
#     else:
#         return None


# def get_product_byName(sensorName, productName):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     select_query = """
#         SELECT p.sensor_id, p.product_id 
#         FROM product_table p
#         JOIN sensor_table s ON p.sensor_id = s.sensor_id
#         WHERE s.sensor_name = %s AND p.product_name = %s
#     """
#     cursor.execute(select_query, (sensorName, productName,))
#     result = cursor.fetchone()
#     cursor.close()
#     connection.close()
#     if result:
#         sensor_id = result['sensor_id']
#         product_id = result['product_id']
#         return sensor_id, product_id
#     else:
#         return None


# def get_scene_byName(sceneName):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     select_query = """
#         SELECT s.sensor_id, s.scene_id, s.scene_name
#         FROM scene_table s
#         WHERE s.scene_name = %s
#     """
#     cursor.execute(select_query, (sceneName, ))
#     result = cursor.fetchone()
#     cursor.close()
#     connection.close()
#     if result:
#         scene_id = result['scene_id']
#         return scene_id
#     else:
#         return None


# def select_scene_time(sceneId):
#     global DB_CONFIG
#     # 建立数据库连接
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     # 查询scene_table中的scene_time
#     query = f"SELECT scene_time FROM scene_table WHERE scene_id = %s"
#     cursor.execute(query, (sceneId,))
#     result = cursor.fetchone()
#     cursor.close()
#     connection.close()
#     return result['scene_time']


import psycopg2
import psycopg2.extras  # 必须显式导入 extras 以使用 DictCursor
import uuid
import time
import socket
import os
import random
from datetime import datetime

# 假设你的配置文件键名保持不变（仍叫 MYSQL_...），在连接函数里做了映射
DB_CONFIG = {}
namespace = uuid.NAMESPACE_DNS

def set_initial_db_config(D_CONFIG):
    global DB_CONFIG
    DB_CONFIG = D_CONFIG

def generate_custom_id(prefix, digit):
    remaining_length = digit - len(prefix)
    if remaining_length <= 0:
        return prefix[:digit]

    characters = "abcdefghijklmnopqrstuvwxyz0123456789"
    random_part = ''.join(random.choice(characters) for _ in range(remaining_length))
    custom_id = prefix + random_part
    return custom_id

def connect_postgres(host, port, database, user, password):
    """
    连接 PostgreSQL 数据库
    """
    global cursor, connection
    try:
        connection = psycopg2.connect(
            host=host,
            port=port,
            dbname=database,  # PG 参数名为 dbname
            user=user,
            password=password,
            options="-c search_path=ard_satellite,public",
            cursor_factory=psycopg2.extras.RealDictCursor  # 使用字典游标
        )
        cursor = connection.cursor()
        return connection, cursor
    except psycopg2.OperationalError as err:
        # 捕获连接层面的错误
        print(f"\033[91m[ERROR] Error connecting to Postgres: {err}\033[0m")
        return None, None
    except Exception as e:
        print(f"\033[91m[ERROR] Unexpected error: {e}\033[0m")
        return None, None

def get_all_scenes():
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], 
        DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], 
        DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    
    if not connection:
        return []

    batch_size = 1000
    offset = 0
    all_results = []
    
    try:
        while True:
            # PostGIS: 移除 'axis-order=long-lat'，默认即可
            select_query = f"""
                SELECT *, ST_AsText(bounding_box) as bounding_box_wkt 
                FROM scene_table 
                ORDER BY scene_id ASC 
                LIMIT {batch_size} OFFSET {offset}
            """
            cursor.execute(select_query)
            results = cursor.fetchall()
            
            if not results:
                break
                
            all_results.extend(results)
            offset += batch_size
    finally:
        if cursor: cursor.close()
        if connection: connection.close()
        
    return all_results

def get_scenes_by_range(start_idx, end_idx):
    global DB_CONFIG
    offset = start_idx - 1
    limit = end_idx - start_idx + 1
    
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], 
        DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], 
        DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    
    results = []
    if not connection:
        return results

    try:
        # PostGIS: ST_AsText 不需要 axis-order 参数
        select_query = f"""
            SELECT *, ST_AsText(bounding_box) as bounding_box_wkt 
            FROM scene_table 
            ORDER BY scene_id ASC
            LIMIT {limit} OFFSET {offset}
        """
        cursor.execute(select_query)
        results = cursor.fetchall()
    finally:
        if cursor: cursor.close()
        if connection: connection.close()
        
    return results

def delete_scene_by_id(scene_id):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return

    try:
        delete_query = "DELETE FROM scene_table WHERE scene_id = %s"
        cursor.execute(delete_query, (scene_id,))
        connection.commit()
    finally:
        cursor.close()
        connection.close()

def update_scene_by_id(scene_id, new_wkt):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return

    try:
        # PostGIS: ST_GeomFromText(wkt, srid) 
        # 注意：PostGIS 严格区分 geometry 类型，如果你的 new_wkt 是 MULTIPOLYGON 但表里是 POLYGON，可能会报错
        sql = """
            UPDATE scene_table
            SET bounding_box = ST_GeomFromText(%s, 4326)
            WHERE scene_id = %s
        """
        cursor.execute(sql, (new_wkt, scene_id))
        connection.commit()
    finally:
        cursor.close()
        connection.close()

def delete_image_by_id(image_id):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return

    try:
        delete_query = "DELETE FROM image_table WHERE image_id = %s"
        cursor.execute(delete_query, (image_id,))
        connection.commit()
    finally:
        cursor.close()
        connection.close()

def get_images_by_scene_id(scene_id):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return []

    try:
        select_query = "SELECT image_id, bucket, tif_path FROM image_table WHERE scene_id = %s"
        cursor.execute(select_query, (scene_id,))
        result = cursor.fetchall()
        return result
    finally:
        cursor.close()
        connection.close()

def get_sensor_byName(sensorName):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return None

    try:
        select_query = "SELECT sensor_id FROM sensor_table WHERE sensor_name = %s"
        cursor.execute(select_query, (sensorName,))
        result = cursor.fetchone()
        if result:
            return result['sensor_id']
        else:
            return None
    finally:
        cursor.close()
        connection.close()

def get_product_byName(sensorName, productName):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return None

    try:
        select_query = """
            SELECT p.sensor_id, p.product_id 
            FROM product_table p
            JOIN sensor_table s ON p.sensor_id = s.sensor_id
            WHERE s.sensor_name = %s AND p.product_name = %s
        """
        cursor.execute(select_query, (sensorName, productName,))
        result = cursor.fetchone()
        if result:
            return result['sensor_id'], result['product_id']
        else:
            return None
    finally:
        cursor.close()
        connection.close()

def get_scene_byName(sceneName):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return None

    try:
        select_query = """
            SELECT s.sensor_id, s.scene_id, s.scene_name
            FROM scene_table s
            WHERE s.scene_name = %s
        """
        cursor.execute(select_query, (sceneName, ))
        result = cursor.fetchone()
        if result:
            return result['scene_id']
        else:
            return None
    finally:
        cursor.close()
        connection.close()

def select_scene_time(sceneId):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return None

    try:
        query = "SELECT scene_time FROM scene_table WHERE scene_id = %s"
        cursor.execute(query, (sceneId,))
        result = cursor.fetchone()
        if result:
            return result['scene_time']
        return None
    finally:
        cursor.close()
        connection.close()