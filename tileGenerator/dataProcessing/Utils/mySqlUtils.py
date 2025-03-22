import mysql.connector
import uuid
import time
import socket
import os
import random

host='223.2.34.7'
database='satellite'
tile_db = 'tile'
user='root'
password='root'

namespace = uuid.NAMESPACE_DNS

def generate_custom_id(prefix, digit):
    remaining_length = digit - len(prefix)  # 计算除去 prefix 后可用的长度
    if remaining_length <= 0:
        return prefix[:digit]  # 如果 prefix 已经超长，则直接截断返回
    # 生成各部分
    timestamp = str(int(time.time()))[-4:]  # 取时间戳后4位
    machine_id = str(abs(hash(socket.gethostname())) % 100).zfill(2)  # 取 hostname 哈希后两位
    process_id = str(os.getpid() % 100).zfill(2)  # 取进程 ID 后两位
    counter = str(random.randint(10, 99))  # 取随机两位数
    # 拼接各部分
    raw_id = timestamp + machine_id + process_id + counter
    # 根据剩余长度进行截取或填充
    custom_id = prefix + raw_id[:remaining_length]
    return custom_id

def connect_mysql(host, database, user, password):
    global cursor, connection
    connection = mysql.connector.connect(
        host=host,
        database=database,
        user=user,
        password=password
    )
    if connection.is_connected():
        cursor = connection.cursor(dictionary=True)
    return connection, cursor

def create_DB():
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
        `tile_levels` SET('8000', '16000', '40000', '40031', '20016'),
        `coordinate_system` VARCHAR(255),
        `bounding_box` GEOMETRY NOT NULL SRID 4326,
        `description` TEXT(65535),
        `png_path` VARCHAR(255),
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
    connection, curser = connect_mysql(host, database, user, password)

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
    create_tile_table = f"""
    CREATE TABLE `{table_name}` (
        `tile_id` VARCHAR(36) NOT NULL PRIMARY KEY,
        `image_id` VARCHAR(36),
        `tile_level` INT,
        `column_id` INT,
        `row_id` INT,
        `path` VARCHAR(255),
        `bucket` VARCHAR(36),
        `bounding_box` GEOMETRY NOT NULL SRID 4326,
        `cloud` FLOAT,
        UNIQUE (`tile_id`),
        SPATIAL INDEX (`bounding_box`)
    ) ENGINE=InnoDB;
    """
    sql_commands = [
        create_tile_table,
    ]
    connection, curser = connect_mysql(host, tile_db, user, password)

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

def delete_DB():
    table_names = [
        'sensor_table',
        'product_table',
        'scene_table',
        'image_table'
    ]
    foreign_key_map = {
        'image_table':'fk_scene_image',
        'scene_table': 'fk_product_scene',
        'product_table': 'fk_sensor_product',
    }
    connection, cursor = connect_mysql(host, database, user, password)
    for key, value in foreign_key_map.items():
        try:
            drop_table_query = f"ALTER TABLE `{key}` DROP FOREIGN KEY `{value}`"
            cursor.execute(drop_table_query)
            print(f"foreign_key {key}:{value} deleted")
        except Exception as e:
            print(f"Wrong when executing {key}:{value} : {e}")
    for table_name in table_names:
        try:
            # 构建删除表的 SQL 语句
            drop_table_query = f"DROP TABLE IF EXISTS {table_name};"
            cursor.execute(drop_table_query)
            print(f"table {table_name} deleted")
        except Exception as e:
            print(f"Wrong when executing {table_name} : {e}")
    connection.commit()
    cursor.close()
    connection.close()
    print("All table deleted。")

def insert_sensor(sensorName, platformName, description):
    connection, curser = connect_mysql(host, database, user, password)
    insert_query = "INSERT INTO sensor_table (sensor_id, sensor_name, platform_name, description) VALUES (%s, %s, %s, %s)"
    data = (generate_custom_id('SE', 7), sensorName, platformName, description)
    cursor.execute(insert_query, data)

    connection.commit()
    print(cursor.rowcount, f"Sensor {sensorName} inserted!")

    cursor.close()
    connection.close()

def get_sensor_byName(sensorName):
    connection, cursor = connect_mysql(host, database, user, password)
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
    connection, cursor = connect_mysql(host, database, user, password)
    sensorId = get_sensor_byName(sensorName)
    insert_query = "INSERT INTO product_table (product_id, sensor_id, product_name, description, resolution, period) VALUES (%s, %s, %s, %s, %s, %s)"
    data = (generate_custom_id('P', 9), sensorId, productName, description, resolution, period)
    cursor.execute(insert_query, data)

    connection.commit()
    print(cursor.rowcount, f"Product {sensorName} inserted!")

    cursor.close()
    connection.close()

def get_product_byName(sensorName, productName):
    uuidName = "imageName"
    connection, cursor = connect_mysql(host, database, user, password)
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

def insert_scene(sensorName, productName, sceneName, sceneTime, tileLevelNum, tileLevels, pngPath, crs, bbox, description, bands, band_num, bucket, cloud):
    bands_str = ",".join(bands) if bands else None
    connection, cursor = connect_mysql(host, database, user, password)
    sensorId, productId = get_product_byName(sensorName, productName)
    insert_query = ("INSERT INTO scene_table (scene_id, sensor_id, product_id, scene_name, scene_time, tile_level_num, tile_levels, coordinate_system, bounding_box, png_path, description, bands, band_num, bucket, cloud) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s, %s, %s, %s, %s)")
    # sceneId = str(uuid.uuid5(namespace, uuidName))
    sceneId = generate_custom_id('SC', 11)
    data = (sceneId, sensorId, productId, sceneName, sceneTime, tileLevelNum, tileLevels, crs, bbox, pngPath, description, bands_str, band_num, bucket, cloud)
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
    connection, cursor = connect_mysql(host, database, user, password)
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
    except mysql.connector.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()
        return imageId

def insert_tile(tile_table_name, imageId, tileLevel, columnId, rowId, path, bucket, bbox, cloud):
    uuidName = "tileName"
    connection, cursor = connect_mysql(host, tile_db, user, password)
    insert_query = f"INSERT INTO {tile_table_name} (tile_id, image_id, tile_level, column_id, row_id, path, bucket, bounding_box, cloud) VALUES (%s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s)"
    tileId = str(uuid.uuid4())
    data = (tileId, imageId, tileLevel, columnId, rowId, path, bucket, bbox, cloud)
    try:
        # 执行插入操作
        cursor.execute(insert_query, data)
        connection.commit()

        print(cursor.rowcount, f"Tile {tileId} inserted!")
    except mysql.connector.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()

def insert_batch_tile(tile_table_name, imageId, tileLevel, tile_info_list):
    connection, cursor = connect_mysql(host, tile_db, user, password)
    insert_query = f"INSERT INTO {tile_table_name} (tile_id, image_id, tile_level, column_id, row_id, path, bucket, bounding_box, cloud) VALUES (%s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s)"
    data_list = [
        (str(uuid.uuid4()), imageId, tileLevel, tile['column_id'], tile['row_id'], tile['path'], tile['bucket'], tile['bbox'], tile['cloud'])
        for tile in tile_info_list
    ]
    try:
        # 执行插入操作
        cursor.executemany(insert_query, data_list)
        connection.commit()

        print(cursor.rowcount, f"tiles inserted!")
    except mysql.connector.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()

def select_tile_by_ids(tile_table_name, id_list):
    connection, cursor = connect_mysql(host, tile_db, user, password)

    # 构建查询语句，使用占位符为每个 ID
    placeholders = ', '.join(['%s'] * len(id_list))  # 用于 SQL 查询中多个 ID 的占位符
    select_query = f"SELECT * FROM {tile_table_name} WHERE tile_id IN ({placeholders})"

    # 执行查询
    cursor.execute(select_query, tuple(id_list))

    # 获取所有查询结果
    result = cursor.fetchall()

    print(f"Found {len(result)} tiles with the provided IDs.")

    cursor.close()
    connection.close()

    return result
