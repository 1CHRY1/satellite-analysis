import mysql.connector
import uuid
import time
import socket
import os
import random
import dataProcessing.config as config
from datetime import datetime

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
        `tile_levels` SET('8000', '16000', '40000', '40031', '20016', '40031*20016'),
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
    connection, curser = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)

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
    connection, curser = connect_mysql(config.MYSQL_HOST, config.MYSQL_TILE_DB, config.MYSQL_USER, config.MYSQL_PWD)

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
        'image_table': 'fk_scene_image',
        'scene_table': 'fk_product_scene',
        'product_table': 'fk_sensor_product',
    }
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
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
    connection, curser = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
    insert_query = "INSERT INTO sensor_table (sensor_id, sensor_name, platform_name, description) VALUES (%s, %s, %s, %s)"
    data = (generate_custom_id('SE', 7), sensorName, platformName, description)
    cursor.execute(insert_query, data)

    connection.commit()
    print(cursor.rowcount, f"Sensor {sensorName} inserted!")

    cursor.close()
    connection.close()


def get_sensor_byName(sensorName):
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
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
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
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
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
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


def insert_scene(sensorName, productName, sceneName, sceneTime, tileLevelNum, tileLevels, pngPath, crs, bbox,
                 description, bands, band_num, bucket, cloud):
    bands_str = ",".join(bands) if bands else None
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
    sensorId, productId = get_product_byName(sensorName, productName)
    insert_query = (
        "INSERT INTO scene_table (scene_id, sensor_id, product_id, scene_name, scene_time, tile_level_num, tile_levels, coordinate_system, bounding_box, png_path, description, bands, band_num, bucket, cloud) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s, %s, %s, %s, %s)")
    # sceneId = str(uuid.uuid5(namespace, uuidName))
    sceneId = generate_custom_id('SC', 11)
    data = (
    sceneId, sensorId, productId, sceneName, sceneTime, tileLevelNum, tileLevels, crs, bbox, pngPath, description,
    bands_str, band_num, bucket, cloud)
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
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER,
                                       config.MYSQL_PWD)
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


def insert_tile(tile_table_name, image_id, tileLevel, columnId, rowId, path, bucket, bbox, cloud, band):
    uuidName = "tileName"
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_TILE_DB, config.MYSQL_USER, config.MYSQL_PWD)
    insert_query = f"INSERT INTO {tile_table_name} (tile_id, image_id, tile_level, column_id, row_id, path, bucket, bounding_box, cloud, band) VALUES (%s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326, 'axis-order=long-lat'), %s, %s)"
    tileId = str(uuid.uuid4())
    data = (tileId, image_id, tileLevel, columnId, rowId, path, bucket, bbox, cloud, band)
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


def insert_batch_tile(tile_table_name, image_id, tileLevel, tile_info_list, band):
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_TILE_DB, config.MYSQL_USER, config.MYSQL_PWD)
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
    except mysql.connector.Error as err:
        print(f"Error: {err}")
        connection.rollback()
    finally:
        # 确保关闭游标和连接
        cursor.close()
        connection.close()


def select_tile_by_ids(tile_table_name, id_list):
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_TILE_DB, config.MYSQL_USER, config.MYSQL_PWD)

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


def select_tile_by_column_and_row(tile_table_name, tiles, bands):
    """
    根据 columnId, rowId 和 bands 查询数据库中的瓦片信息，确保 columnId 和 rowId 成对匹配，按 band 分组返回结果。

    :param tile_table_name: str, 数据表名
    :param tiles: list, 包含多个 {"columnId": X, "rowId": Y} 的字典
    :param bands: list, 需要的波段列表
    :return: dict, 按 band 分组的查询结果
    """
    from collections import defaultdict

    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_TILE_DB, config.MYSQL_USER, config.MYSQL_PWD)

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

    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_TILE_DB, config.MYSQL_USER, config.MYSQL_PWD)

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
    connection, cursor = connect_mysql(config.MYSQL_HOST, config.MYSQL_SATELLITE_DB, config.MYSQL_USER, config.MYSQL_PWD)
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
