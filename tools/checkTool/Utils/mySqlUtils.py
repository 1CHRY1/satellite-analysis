import pymysql
import uuid
import time
import socket
import os
import random
from datetime import datetime

DB_CONFIG = {}
namespace = uuid.NAMESPACE_DNS


def set_initial_mysql_config(D_CONFIG):
    global DB_CONFIG
    DB_CONFIG = D_CONFIG


def generate_custom_id(prefix, digit):
    remaining_length = digit - len(prefix)  # 计算除去 prefix 后可用的长度
    if remaining_length <= 0:
        return prefix[:digit]  # 如果 prefix 已经超长，则直接截断返回

    # 定义可用字符集 (类似UUID格式: 数字和小写字母)
    characters = "abcdefghijklmnopqrstuvwxyz0123456789"
    # 生成随机字符串
    random_part = ''.join(random.choice(characters) for _ in range(remaining_length))
    # 拼接前缀和随机部分
    custom_id = prefix + random_part
    return custom_id


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
        print(f"\033[91m[ERROR] Error connecting to MySQL: {err}\033[0m")
        if err.errno == pymysql.err.ER_ACCESS_DENIED_ERROR:
            print("[ERROR] Invalid username or password")
        elif err.errno == pymysql.err.ER_BAD_DB_ERROR:
            print("[ERROR] Database does not exist")
        else:
            print(f"\033[91m[ERROR] Error {err.errno}: {err.args[1]}\033[0m")
        return None, None
    except Exception as e:
        print(f"\033[91m[ERROR] Unexpected error: {e}\033[0m")
        return None, None

def get_all_scenes():
    global DB_CONFIG
    connection, cursor = connect_mysql(
        DB_CONFIG["MYSQL_HOST"], 
        DB_CONFIG["MYSQL_RESOURCE_PORT"],
        DB_CONFIG["MYSQL_RESOURCE_DB"], 
        DB_CONFIG["MYSQL_USER"],
        DB_CONFIG["MYSQL_PWD"]
    )
    
    batch_size = 1000  # 每批查询1000条数据
    offset = 0
    all_results = []
    
    try:
        while True:
            select_query = f"SELECT scene_id, scene_name FROM scene_table LIMIT {batch_size} OFFSET {offset}"
            cursor.execute(select_query)
            results = cursor.fetchall()
            
            if not results:
                break
                
            all_results.extend(results)
            offset += batch_size
    finally:
        cursor.close()
        connection.close()
        
    return all_results

def delete_scene_by_id(scene_id):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    delete_query = "DELETE FROM scene_table WHERE scene_id = %s"
    cursor.execute(delete_query, (scene_id,))
    connection.commit()
    cursor.close()
    connection.close()

def delete_image_by_id(image_id):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    delete_query = "DELETE FROM image_table WHERE image_id = %s"
    cursor.execute(delete_query, (image_id,))
    connection.commit()
    cursor.close()
    connection.close()

def get_images_by_scene_id(scene_id):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    select_query = "SELECT image_id, tif_path FROM image_table WHERE scene_id = %s"
    cursor.execute(select_query, (scene_id,))
    result = cursor.fetchall()
    cursor.close()
    connection.close()
    return result

def get_sensor_byName(sensorName):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    select_query = "SELECT sensor_id FROM sensor_table WHERE sensor_name = %s"
    cursor.execute(select_query, (sensorName,))
    result = cursor.fetchone()
    cursor.close()
    connection.close()
    if result:
        sensor_id = result['sensor_id']
        return sensor_id
    else:
        return None


def get_product_byName(sensorName, productName):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    select_query = """
        SELECT p.sensor_id, p.product_id 
        FROM product_table p
        JOIN sensor_table s ON p.sensor_id = s.sensor_id
        WHERE s.sensor_name = %s AND p.product_name = %s
    """
    cursor.execute(select_query, (sensorName, productName,))
    result = cursor.fetchone()
    cursor.close()
    connection.close()
    if result:
        sensor_id = result['sensor_id']
        product_id = result['product_id']
        return sensor_id, product_id
    else:
        return None


def get_scene_byName(sceneName):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    select_query = """
        SELECT s.sensor_id, s.scene_id, s.scene_name
        FROM scene_table s
        WHERE s.scene_name = %s
    """
    cursor.execute(select_query, (sceneName, ))
    result = cursor.fetchone()
    cursor.close()
    connection.close()
    if result:
        scene_id = result['scene_id']
        return scene_id
    else:
        return None


def select_scene_time(sceneId):
    global DB_CONFIG
    # 建立数据库连接
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    # 查询scene_table中的scene_time
    query = f"SELECT scene_time FROM scene_table WHERE scene_id = %s"
    cursor.execute(query, (sceneId,))
    result = cursor.fetchone()
    cursor.close()
    connection.close()
    return result['scene_time']
