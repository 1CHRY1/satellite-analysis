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


def create_DB():
    global DB_CONFIG
    create_sensor_table = """
    CREATE TABLE `sensor_table` (
        `sensor_id` VARCHAR(36) NOT NULL UNIQUE,
	    `sensor_name` VARCHAR(255),
	    `platform_name` VARCHAR(255),
	    `description` TEXT,
	    PRIMARY KEY(`sensor_id`)
    );
    """

    create_scene_table = """
    CREATE TABLE `scene_table` (
        `scene_id` VARCHAR(36) NOT NULL UNIQUE,
        `product_id` VARCHAR(36),
        `scene_name` VARCHAR(255),
        `scene_time` DATETIME,
        `sensor_id` VARCHAR(36),
        `tile_level_num` INT,
        `tile_levels` SET('8000', '16000', '40000', '40031*20016'),
        `coordinate_system` VARCHAR(255),
        `bounding_box` GEOMETRY NOT NULL SRID 4326,
        `description` TEXT(65535),
        `cloud_path` VARCHAR(255),
        `bands` SET('1', '2', '3', '4', '5', '6', '7'),
        `band_num` INT,
        `bucket` VARCHAR(36),
        `cloud` FLOAT,
        PRIMARY KEY(`scene_id`),
        SPATIAL INDEX (`bounding_box`)
    ) ENGINE=InnoDB;
    """

    create_product_table = """
    CREATE TABLE `product_table` (
        `product_id` VARCHAR(36) NOT NULL UNIQUE,
        `sensor_id` VARCHAR(36),
        `product_name` VARCHAR(255),
        `description` TEXT(65535),
        `resolution` VARCHAR(255),
        `period` VARCHAR(255),
        PRIMARY KEY(`product_id`)
    );
    """

    create_image_table = """
    CREATE TABLE `image_table` (
        `image_id` VARCHAR(36) NOT NULL PRIMARY KEY,
        `scene_id` VARCHAR(36),
        `tif_path` VARCHAR(255),
        `band` VARCHAR(255),
        `bucket` VARCHAR(36),
        `cloud` FLOAT,
        UNIQUE (`image_id`)
    );
    """

    add_fk_sensor_product = """
    ALTER TABLE `product_table`
    ADD CONSTRAINT `fk_sensor_product`
    FOREIGN KEY(`sensor_id`)
    REFERENCES `sensor_table`(`sensor_id`)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    add_fk_product_scene = """
    ALTER TABLE `scene_table`
    ADD CONSTRAINT `fk_product_scene`
    FOREIGN KEY(`product_id`)
    REFERENCES `product_table`(`product_id`)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    add_fk_scene_image = """
    ALTER TABLE `image_table`
    ADD CONSTRAINT `fk_scene_image`
    FOREIGN KEY (`scene_id`)
    REFERENCES `scene_table` (`scene_id`)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    sql_commands = [
        create_sensor_table,
        create_product_table,
        create_scene_table,
        create_image_table,
        add_fk_sensor_product,
        add_fk_product_scene,
        add_fk_scene_image,
    ]
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])

    for command in sql_commands:
        try:
            cursor.execute(command)
            print(f"Executed: {command}")
        except Exception as e:
            print(f"Wrong when executing {command.splitlines()[0]} : {e}")
    connection.commit()
    cursor.close()
    connection.close()
    print("All SQL Executed")


def create_tile_table(table_name):
    global DB_CONFIG
    table_name = table_name.lower()
    create_tile_table = f"""
    CREATE TABLE `{table_name}` (
        `tile_id` VARCHAR(36) NOT NULL,
        `image_id` VARCHAR(15) NOT NULL,
        `tile_level` VARCHAR(36),
        `column_id` INT,
        `row_id` INT,
        `path` VARCHAR(255),
        `bucket` VARCHAR(36),
        `bounding_box` GEOMETRY NOT NULL SRID 4326,
        `cloud` FLOAT,
        `band` INT NOT NULL,
        UNIQUE (`tile_id`),
        SPATIAL INDEX (`bounding_box`)
    ) ENGINE=InnoDB;
    """
    # 添加联合索引
    create_idx_band_tile_level = f"""
    CREATE INDEX idx_band_tile_level ON {table_name} (band, tile_level);
    """
    create_idx_column_row = f"""
    CREATE INDEX idx_column_row_band ON {table_name} (column_id, row_id, band);
    """
    sql_commands = [
        create_tile_table,
        create_idx_band_tile_level,
        create_idx_column_row
    ]
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_TILE_PORT"],
                                       DB_CONFIG["MYSQL_TILE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])

    for command in sql_commands:
        try:
            cursor.execute(command)
            print(f"Executed: {command}")
        except Exception as e:
            print(f"Wrong when executing {command.splitlines()[0]} : {e}")
    connection.commit()
    cursor.close()
    connection.close()
    print("All SQL Executed")


def insert_sensor(sensorName, platformName, description):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    insert_query = "INSERT INTO sensor_table (sensor_id, sensor_name, platform_name, description) VALUES (%s, %s, %s, %s)"
    data = (generate_custom_id('SE', 7), sensorName, platformName, description)
    cursor.execute(insert_query, data)

    connection.commit()
    print(cursor.rowcount, f"Sensor {sensorName} inserted!")

    cursor.close()
    connection.close()


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


def insert_product(sensorName, productName, description, resolution, period):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])

    sensorId = get_sensor_byName(sensorName)
    insert_query = "INSERT INTO product_table (product_id, sensor_id, product_name, description, resolution, period) VALUES (%s, %s, %s, %s, %s, %s)"
    data = (generate_custom_id('P', 9), sensorId, productName, description, resolution, period)
    cursor.execute(insert_query, data)

    connection.commit()
    print(cursor.rowcount, f"Product {sensorName} inserted!")

    cursor.close()
    connection.close()


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


def insert_scene(sensorName, productName, sceneName, sceneTime, tileLevelNum, tileLevels, cloudPath, crs, bbox,
                 description, bands, band_num, bucket, cloud, tags):
    global DB_CONFIG
    bands_str = ",".join([str(band) for band in bands or []]) if bands is not None else ""
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    sensorId, productId = get_product_byName(sensorName, productName)
    insert_query = (
        "INSERT INTO scene_table (scene_id, sensor_id, product_id, scene_name, scene_time, tile_level_num, tile_levels, coordinate_system, bounding_box, cloud_path, description, bands, band_num, bucket, cloud, tags) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s, %s, %s, %s, %s, %s)")
    # sceneId = str(uuid.uuid5(namespace, uuidName))
    sceneId = generate_custom_id('SC', 11)
    data = (
        sceneId, sensorId, productId, sceneName, sceneTime, tileLevelNum, tileLevels, crs, bbox, cloudPath, description,
        bands_str, band_num, bucket, cloud, tags)
    try:
        # 执行插入操作
        cursor.execute(insert_query, data)
        connection.commit()

        print(cursor.rowcount, f"Scene {sceneId} inserted!")
    except Exception as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()
        return sceneId


def insert_image(sceneId, tifPath, band, bucket, cloud):
    global DB_CONFIG
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    insert_query = ("INSERT INTO image_table (image_id, scene_id, tif_path, band, bucket, cloud) "
                    "VALUES (%s, %s, %s, %s, %s, %s)")
    # imageId = str(uuid.uuid5(namespace, uuidName))
    imageId = generate_custom_id('I', 11)
    data = (imageId, sceneId, tifPath, band, bucket, cloud)
    try:
        # 执行插入操作
        cursor.execute(insert_query, data)
        connection.commit()

        print(cursor.rowcount, f"Image {imageId} inserted!")
    except pymysql.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()
        return imageId


def insert_tile(tile_table_name, image_id, tileLevel, columnId, rowId, path, bucket, bbox, cloud, band):
    global DB_CONFIG
    tile_table_name = tile_table_name.lower()
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_TILE_PORT"],
                                       DB_CONFIG["MYSQL_TILE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    insert_query = f"INSERT INTO {tile_table_name} (tile_id, image_id, tile_level, column_id, row_id, path, bucket, bounding_box, cloud, band) VALUES (%s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s)"
    tileId = str(uuid.uuid4())
    data = (tileId, image_id, tileLevel, columnId, rowId, path, bucket, bbox, cloud, band)
    try:
        # 执行插入操作
        cursor.execute(insert_query, data)
        connection.commit()

        print(cursor.rowcount, f"Tile {tileId} inserted!")
    except pymysql.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()


def insert_batch_tile(tile_table_name, image_id, tileLevel, tile_info_list, band):
    global DB_CONFIG
    tile_table_name = tile_table_name.lower()
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_TILE_PORT"],
                                       DB_CONFIG["MYSQL_TILE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    insert_query = f"INSERT INTO {tile_table_name} (tile_id, image_id, tile_level, column_id, row_id, path, bucket, bounding_box, cloud, band) VALUES (%s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s)"
    data_list = [
        (str(uuid.uuid4()), image_id, tileLevel, tile['column_id'], tile['row_id'], tile['path'], tile['bucket'],
         tile['bbox'], tile['cloud'], band)
        for tile in tile_info_list
    ]
    try:
        # 执行插入操作
        cursor.executemany(insert_query, data_list)
        connection.commit()

        print(cursor.rowcount, f"tiles inserted!")
    except pymysql.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()


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
