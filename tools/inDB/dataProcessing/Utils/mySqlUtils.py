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


# def create_DB():
#     global DB_CONFIG
#     create_sensor_table = """
#     CREATE TABLE `sensor_table` (
#         `sensor_id` VARCHAR(36) NOT NULL UNIQUE,
# 	    `sensor_name` VARCHAR(255),
# 	    `platform_name` VARCHAR(255),
# 	    `description` TEXT,
# 	    PRIMARY KEY(`sensor_id`)
#     );
#     """

#     create_scene_table = """
#     CREATE TABLE `scene_table` (
#         `scene_id` VARCHAR(36) NOT NULL UNIQUE,
#         `product_id` VARCHAR(36),
#         `scene_name` VARCHAR(255),
#         `scene_time` DATETIME,
#         `sensor_id` VARCHAR(36),
#         `tile_level_num` INT,
#         `tile_levels` SET('8000', '16000', '40000', '40031*20016'),
#         `coordinate_system` VARCHAR(255),
#         `bounding_box` GEOMETRY NOT NULL SRID 4326,
#         `description` TEXT(65535),
#         `cloud_path` VARCHAR(255),
#         `bands` SET('1', '2', '3', '4', '5', '6', '7'),
#         `band_num` INT,
#         `bucket` VARCHAR(36),
#         `cloud` FLOAT,
#         PRIMARY KEY(`scene_id`),
#         SPATIAL INDEX (`bounding_box`)
#     ) ENGINE=InnoDB;
#     """

#     create_product_table = """
#     CREATE TABLE `product_table` (
#         `product_id` VARCHAR(36) NOT NULL UNIQUE,
#         `sensor_id` VARCHAR(36),
#         `product_name` VARCHAR(255),
#         `description` TEXT(65535),
#         `resolution` VARCHAR(255),
#         `period` VARCHAR(255),
#         PRIMARY KEY(`product_id`)
#     );
#     """

#     create_image_table = """
#     CREATE TABLE `image_table` (
#         `image_id` VARCHAR(36) NOT NULL PRIMARY KEY,
#         `scene_id` VARCHAR(36),
#         `tif_path` VARCHAR(255),
#         `band` VARCHAR(255),
#         `bucket` VARCHAR(36),
#         `cloud` FLOAT,
#         UNIQUE (`image_id`)
#     );
#     """

#     add_fk_sensor_product = """
#     ALTER TABLE `product_table`
#     ADD CONSTRAINT `fk_sensor_product`
#     FOREIGN KEY(`sensor_id`)
#     REFERENCES `sensor_table`(`sensor_id`)
#     ON UPDATE NO ACTION
#     ON DELETE NO ACTION;
#     """

#     add_fk_product_scene = """
#     ALTER TABLE `scene_table`
#     ADD CONSTRAINT `fk_product_scene`
#     FOREIGN KEY(`product_id`)
#     REFERENCES `product_table`(`product_id`)
#     ON UPDATE NO ACTION
#     ON DELETE NO ACTION;
#     """

#     add_fk_scene_image = """
#     ALTER TABLE `image_table`
#     ADD CONSTRAINT `fk_scene_image`
#     FOREIGN KEY (`scene_id`)
#     REFERENCES `scene_table` (`scene_id`)
#     ON UPDATE NO ACTION
#     ON DELETE NO ACTION;
#     """

#     sql_commands = [
#         create_sensor_table,
#         create_product_table,
#         create_scene_table,
#         create_image_table,
#         add_fk_sensor_product,
#         add_fk_product_scene,
#         add_fk_scene_image,
#     ]
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])

#     for command in sql_commands:
#         try:
#             cursor.execute(command)
#             print(f"[INFO] Executed: {command}")
#         except Exception as e:
#             print(f"\033[91m[ERROR] Wrong when executing {command.splitlines()[0]} : {e}\033[0m")
#     connection.commit()
#     cursor.close()
#     connection.close()
#     print("[SUCCESS] All SQL Executed")


# def insert_sensor(sensorName, platformName, description):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     insert_query = "INSERT INTO sensor_table (sensor_id, sensor_name, platform_name, description) VALUES (%s, %s, %s, %s)"
#     data = (generate_custom_id('SE', 7), sensorName, platformName, description)
#     cursor.execute(insert_query, data)
#     try:
#         connection.commit()
#         print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Sensor {sensorName} inserted!")
#         return True
#     except Exception as e:
#         print(f"\033[91m[ERROR] Error: {e}\033[0m")
#         connection.rollback()
#         return False
#     finally:
#         cursor.close()
#         connection.close()


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


# def insert_product(sensorName, productName, description, resolution, period):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])

#     sensorId = get_sensor_byName(sensorName)
#     insert_query = "INSERT INTO product_table (product_id, sensor_id, product_name, description, resolution, period) VALUES (%s, %s, %s, %s, %s, %s)"
#     data = (generate_custom_id('P', 9), sensorId, productName, description, resolution, period)
#     cursor.execute(insert_query, data)

#     try:
#         connection.commit()
#         print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Product {sensorName} inserted!")
#         return True
#     except Exception as e:
#         print(f"\033[91m[ERROR] Error: {e}\033[0m")
#         connection.rollback()
#         return False
#     finally:
#         cursor.close()
#         connection.close()


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


# def insert_scene(sensorName, productName, sceneName, sceneTime, tileLevelNum, tileLevels, cloudPath, crs, bbox,
#                  description, bands, band_num, bucket, cloud, tags, no_data):
#     global DB_CONFIG
#     bands_str = ",".join([str(band) for band in bands or []]) if bands is not None else ""
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     sensorId, productId = get_product_byName(sensorName, productName)
#     sceneId = get_scene_byName(sceneName)
#     if sceneId is not None:
#         print(f"\033[91m[WARNING] Scene {sceneName} already exists. Skipping insertion.\033[0m")
#         return None
#     insert_query = (
#         "INSERT INTO scene_table (scene_id, sensor_id, product_id, scene_name, scene_time, tile_level_num, tile_levels, coordinate_system, bounding_box, cloud_path, description, bands, band_num, bucket, cloud, tags, no_data) "
#         "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s, %s, %s, %s, %s, %s, %s)")
#     # sceneId = str(uuid.uuid5(namespace, uuidName))
#     sceneId = generate_custom_id('SC', 11)
#     data = (
#         sceneId, sensorId, productId, sceneName, sceneTime, tileLevelNum, tileLevels, crs, bbox, cloudPath, description,
#         bands_str, band_num, bucket, cloud, tags, no_data)
#     try:
#         # 执行插入操作
#         cursor.execute(insert_query, data)
#         connection.commit()

#         print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Scene {sceneId} inserted!")
#         return sceneId
#     except Exception as err:
#         print(f"\033[91m[ERROR] Error: {err}\033[0m")
#         connection.rollback()
#         return None
#     finally:
#         # 确保关闭游标和连接
#         cursor.close()
#         connection.close()


# def insert_image(sceneId, tifPath, band, bucket, cloud):
#     global DB_CONFIG
#     connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
#                                        DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
#                                        DB_CONFIG["MYSQL_PWD"])
#     insert_query = ("INSERT INTO image_table (image_id, scene_id, tif_path, band, bucket, cloud) "
#                     "VALUES (%s, %s, %s, %s, %s, %s)")
#     # imageId = str(uuid.uuid5(namespace, uuidName))
#     imageId = generate_custom_id('I', 11)
#     data = (imageId, sceneId, tifPath, band, bucket, cloud)
#     try:
#         # 执行插入操作
#         cursor.execute(insert_query, data)
#         connection.commit()

#         print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Image {imageId} inserted!")
#         return imageId
#     except pymysql.Error as err:
#         print(f"\033[91m[ERROR] Error: {err}\033[0m")
#         connection.rollback()
#         return None
#     finally:
#         # 确保关闭游标和连接
#         cursor.close()
#         connection.close()

import psycopg2
import psycopg2.extras
import uuid
import time
import socket
import os
import random
from datetime import datetime

# 保持 DB_CONFIG 结构
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
    return prefix + random_part

def connect_postgres(host, port, database, user, password):
    global cursor, connection
    try:
        connection = psycopg2.connect(
            host=host,
            port=port,
            dbname=database,
            user=user,
            password=password,
            # === 核心修改：指定 Schema ===
            options="-c search_path=ard_satellite,public",
            cursor_factory=psycopg2.extras.RealDictCursor
        )
        cursor = connection.cursor()
        return connection, cursor
    except psycopg2.OperationalError as err:
        print(f"\033[91m[ERROR] Error connecting to Postgres: {err}\033[0m")
        return None, None
    except Exception as e:
        print(f"\033[91m[ERROR] Unexpected error: {e}\033[0m")
        return None, None

def create_DB():
    """
    在 Postgres 中创建表结构
    注意：PostGIS 必须先安装扩展 (CREATE EXTENSION postgis;)
    """
    global DB_CONFIG
    
    # 1. Sensor Table
    create_sensor_table = """
    CREATE TABLE IF NOT EXISTS sensor_table (
        sensor_id VARCHAR(36) NOT NULL UNIQUE,
        sensor_name VARCHAR(255),
        platform_name VARCHAR(255),
        description TEXT,
        PRIMARY KEY(sensor_id)
    );
    """

    # 2. Scene Table
    # 修改点：
    # - DATETIME -> TIMESTAMP
    # - SET(...) -> TEXT (PG无原生SET类型，通常存为逗号分隔字符串)
    # - GEOMETRY ... SRID 4326 -> geometry(Geometry, 4326)
    # - 移除 ENGINE=InnoDB
    # - 移除 SPATIAL INDEX (PG需单独创建)
    create_scene_table = """
    CREATE TABLE IF NOT EXISTS scene_table (
        scene_id VARCHAR(36) NOT NULL UNIQUE,
        product_id VARCHAR(36),
        scene_name VARCHAR(255),
        scene_time TIMESTAMP,
        sensor_id VARCHAR(36),
        tile_level_num INT,
        tile_levels TEXT, 
        coordinate_system VARCHAR(255),
        bounding_box geometry(Geometry, 4326) NOT NULL, 
        description TEXT,
        cloud_path VARCHAR(255),
        bands TEXT,
        band_num INT,
        bucket VARCHAR(36),
        cloud FLOAT,
        tags TEXT, 
        no_data FLOAT,
        PRIMARY KEY(scene_id)
    );
    """
    
    # 3. Product Table
    create_product_table = """
    CREATE TABLE IF NOT EXISTS product_table (
        product_id VARCHAR(36) NOT NULL UNIQUE,
        sensor_id VARCHAR(36),
        product_name VARCHAR(255),
        description TEXT,
        resolution VARCHAR(255),
        period VARCHAR(255),
        PRIMARY KEY(product_id)
    );
    """

    # 4. Image Table
    create_image_table = """
    CREATE TABLE IF NOT EXISTS image_table (
        image_id VARCHAR(36) NOT NULL PRIMARY KEY,
        scene_id VARCHAR(36),
        tif_path VARCHAR(255),
        band VARCHAR(255),
        bucket VARCHAR(36),
        cloud FLOAT,
        UNIQUE (image_id)
    );
    """

    # 外键约束
    add_fk_sensor_product = """
    ALTER TABLE product_table
    ADD CONSTRAINT fk_sensor_product
    FOREIGN KEY(sensor_id)
    REFERENCES sensor_table(sensor_id)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    add_fk_product_scene = """
    ALTER TABLE scene_table
    ADD CONSTRAINT fk_product_scene
    FOREIGN KEY(product_id)
    REFERENCES product_table(product_id)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    add_fk_scene_image = """
    ALTER TABLE image_table
    ADD CONSTRAINT fk_scene_image
    FOREIGN KEY (scene_id)
    REFERENCES scene_table (scene_id)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """
    
    # PG 特有的空间索引创建方式
    create_spatial_index = """
    CREATE INDEX IF NOT EXISTS idx_scene_bbox ON scene_table USING GIST (bounding_box);
    """

    # 命令列表 (注意：如果表已存在，添加外键可能会报错，建议在空库运行或加 try-except)
    sql_commands = [
        create_sensor_table,
        create_product_table,
        create_scene_table,
        create_image_table,
        create_spatial_index
        # 外键建议单独手动执行或确保顺序，这里为了脚本完整性保留，但需注意如果表已存在且有数据，外键可能失败
    ]
    
    # 连接 PG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    
    if not connection:
        return

    # 先尝试执行建表
    for command in sql_commands:
        try:
            cursor.execute(command)
            connection.commit() # PG DDL 需要提交
            print(f"[INFO] Executed table/index creation.")
        except Exception as e:
            connection.rollback()
            print(f"\033[91m[ERROR] SQL Error: {e}\033[0m")

    # 单独处理外键，防止报错影响后续
    fk_commands = [add_fk_sensor_product, add_fk_product_scene, add_fk_scene_image]
    for command in fk_commands:
        try:
            cursor.execute(command)
            connection.commit()
            print(f"[INFO] Executed FK constraint.")
        except Exception as e:
            connection.rollback()
            # 外键已存在是很常见的错误，可以忽略
            if "already exists" in str(e):
                pass
            else:
                print(f"[WARN] FK Error (might already exist): {e}")

    cursor.close()
    connection.close()
    print("[SUCCESS] All SQL Executed")

def insert_sensor(sensorName, platformName, description):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return False
    
    insert_query = "INSERT INTO sensor_table (sensor_id, sensor_name, platform_name, description) VALUES (%s, %s, %s, %s)"
    data = (generate_custom_id('SE', 7), sensorName, platformName, description)
    
    try:
        cursor.execute(insert_query, data)
        connection.commit()
        print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Sensor {sensorName} inserted!")
        return True
    except Exception as e:
        print(f"\033[91m[ERROR] Error: {e}\033[0m")
        connection.rollback()
        return False
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

def insert_product(sensorName, productName, description, resolution, period):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return False

    try:
        sensorId = get_sensor_byName(sensorName)
        if not sensorId:
            print(f"[ERROR] Sensor {sensorName} not found.")
            return False

        insert_query = "INSERT INTO product_table (product_id, sensor_id, product_name, description, resolution, period) VALUES (%s, %s, %s, %s, %s, %s)"
        data = (generate_custom_id('P', 9), sensorId, productName, description, resolution, period)
        cursor.execute(insert_query, data)
        connection.commit()
        print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Product {productName} inserted!")
        return True
    except Exception as e:
        print(f"\033[91m[ERROR] Error: {e}\033[0m")
        connection.rollback()
        return False
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

def insert_scene(sensorName, productName, sceneName, sceneTime, tileLevelNum, tileLevels, cloudPath, crs, bbox,
                 description, bands, band_num, bucket, cloud, tags, no_data):
    global DB_CONFIG
    # PG 没有 SET 类型，直接存为字符串即可
    bands_str = ",".join([str(band) for band in bands or []]) if bands is not None else ""
    
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return None

    try:
        ids = get_product_byName(sensorName, productName)
        if not ids or not ids[1]:
            print(f"[ERROR] Product {productName} not found.")
            return None
        sensorId, productId = ids

        sceneId = get_scene_byName(sceneName)
        if sceneId is not None:
            print(f"\033[91m[WARNING] Scene {sceneName} already exists. Skipping insertion.\033[0m")
            return None

        # 注意：PostGIS ST_GeomFromText 不需要 axis-order 参数
        insert_query = (
            "INSERT INTO scene_table (scene_id, sensor_id, product_id, scene_name, scene_time, tile_level_num, tile_levels, coordinate_system, bounding_box, cloud_path, description, bands, band_num, bucket, cloud, tags, no_data) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326), %s, %s, %s, %s, %s, %s, %s, %s)"
        )
        
        sceneId = generate_custom_id('SC', 11)
        tileLevels = None
        data = (
            sceneId, sensorId, productId, sceneName, sceneTime, tileLevelNum, tileLevels, crs, bbox, cloudPath, description,
            bands_str, band_num, bucket, cloud, tags, no_data)
        
        cursor.execute(insert_query, data)
        connection.commit()

        print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Scene {sceneId} inserted!")
        return sceneId
    except Exception as err:
        print(f"\033[91m[ERROR] Error: {err}\033[0m")
        connection.rollback()
        return None
    finally:
        cursor.close()
        connection.close()

def insert_image(sceneId, tifPath, band, bucket, cloud):
    global DB_CONFIG
    connection, cursor = connect_postgres(
        DB_CONFIG["PG_HOST"], DB_CONFIG["PG_RESOURCE_PORT"],
        DB_CONFIG["PG_RESOURCE_DB"], DB_CONFIG["PG_USER"],
        DB_CONFIG["PG_PWD"]
    )
    if not connection: return None

    insert_query = ("INSERT INTO image_table (image_id, scene_id, tif_path, band, bucket, cloud) "
                    "VALUES (%s, %s, %s, %s, %s, %s)")
    imageId = generate_custom_id('I', 11)
    data = (imageId, sceneId, tifPath, band, bucket, cloud)
    
    try:
        cursor.execute(insert_query, data)
        connection.commit()
        print(f"[SUCCESS] {cursor.rowcount} row(s) affected. Image {imageId} inserted!")
        return imageId
    except Exception as err:
        print(f"\033[91m[ERROR] Error: {err}\033[0m")
        connection.rollback()
        return None
    finally:
        cursor.close()
        connection.close()