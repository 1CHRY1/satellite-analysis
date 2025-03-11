import mysql.connector
import uuid
import time
import socket
import os
import random

host='localhost'
database='satellite'
user='root'
password='123456'

namespace = uuid.NAMESPACE_DNS

def generate_custom_id():
    timestamp = str(int(time.time()))[-4:]
    machine_id = str(hash(socket.gethostname()))[-2:]
    process_id = str(os.getpid())[-2:]
    counter = str(random.randint(10, 99))
    custom_id = timestamp + machine_id + process_id + counter
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
        cursor = connection.cursor()
    return connection, cursor

def create_DB():
    create_sensor_table = """
    CREATE TABLE `sensor_table` (
        `sensor_id` CHAR(36) PRIMARY KEY,
        `sensor_name` VARCHAR(255),
        `platform_name` VARCHAR(255),
        `description` TEXT,
        UNIQUE (`sensor_id`)
    );
    """

    create_product_table = """
    CREATE TABLE `product_table` (
        `product_id` CHAR(36) PRIMARY KEY,
        `sensor_id` CHAR(36),
        `product_name` VARCHAR(255),
        `description` TEXT,
        `basicInfo` JSON,
        UNIQUE (`product_id`)
    );
    """

    create_image_table = """
    CREATE TABLE `image_table` (
        `image_id` CHAR(36) PRIMARY KEY,
        `product_id` CHAR(36),
        `sensor_id` CHAR(36),
        `image_time` DATETIME,
        `tile_level_min` INT,
        `tile_level_max` INT,
        `image_path` VARCHAR(255),
        `crs` VARCHAR(255),
        `description` TEXT,
        `bbox` GEOMETRY,
        `band` VARCHAR(255),
        UNIQUE (`image_id`)
    );
    """

    create_tile_table = """
    CREATE TABLE `tile_table` (
        `tile_id` CHAR(36) PRIMARY KEY,
        `image_id` CHAR(36),
        `tile_level` INT,
        `bbox` GEOMETRY,
        `tile_path` VARCHAR(255),
        UNIQUE (`tile_id`)
    );
    """

    add_fk_image_tile = """
    ALTER TABLE `tile_table`
    ADD CONSTRAINT `fk_image_tile`
    FOREIGN KEY (`image_id`)
    REFERENCES `image_table` (`image_id`)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    add_fk_product_image = """
    ALTER TABLE `image_table`
    ADD CONSTRAINT `fk_product_image`
    FOREIGN KEY (`product_id`)
    REFERENCES `product_table` (`product_id`)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    add_fk_sensor_image = """
        ALTER TABLE `image_table`
        ADD CONSTRAINT `fk_sensor_image`
        FOREIGN KEY (`sensor_id`)
        REFERENCES `sensor_table` (`image_id`)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
        """

    add_fk_product_sensor = """
    ALTER TABLE `product_table`
    ADD CONSTRAINT `fk_product_sensor`
    FOREIGN KEY (`sensor_id`)
    REFERENCES `sensor_table` (`sensor_id`)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;
    """

    sql_commands = [
        create_sensor_table,
        create_product_table,
        create_image_table,
        create_tile_table,
        add_fk_image_tile,
        add_fk_product_image,
        add_fk_product_sensor
    ]
    connection, curser = connect_mysql(host, database, user, password)

    for command in sql_commands:
        try:
            cursor.execute(command)
            print(f"Excuted: {command}")
        except Exception as e:
            print(f"Wrong when excuting {command.splitlines()[0]} : {e}")
    connection.commit()
    cursor.close()
    connection.close()
    print("All SQL Executed")

def delete_DB():
    table_names = [
        'sensor_table',
        'product_table',
        'image_table',
        'tile_table'
    ]
    foreign_key_map = {
        'tile_table':'fk_image_tile',
        'image_table':'fk_product_image',
        'product_table':'fk_product_sensor',
        'sensor_table':'fk_sensor_image'
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
    data = (generate_custom_id(), sensorName, platformName, description)
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
        sensor_id = result[0]
        return sensor_id
    else:
        return None

def insert_product(sensorName, productName, description):
    uuidName = "productName"
    connection, cursor = connect_mysql(host, database, user, password)
    sensorId = get_sensor_byName(sensorName)
    insert_query = "INSERT INTO product_table (product_id, sensor_id, product_name, description) VALUES (%s, %s, %s, %s)"
    data = (str(uuid.uuid5(namespace, uuidName)), sensorId, productName, description)
    cursor.execute(insert_query, data)

    connection.commit()
    print(cursor.rowcount, f"Product {sensorName} inserted!")

    cursor.close()
    connection.close()

def get_product_byName(sensorName, productName):
    uuidName = "imageName"
    connection, cursor = connect_mysql(host, database, user, password)
    sensorId = get_sensor_byName(sensorName)
    select_query = "SELECT sensor_id, product_id FROM sensor_table WHERE sensor_name = %s and product_name = %s"
    cursor.execute(select_query, (sensorId, productName,))
    result = cursor.fetchone()
    cursor.close()
    connection.close()
    if result:
        product_id = result[0]
        return sensorId, product_id
    else:
        return None

def insert_image(sensorName, productName, imageTime, tileLevelMin, tileLevelMax, imagePath, crs, band, bbox, description):
    uuidName = "productName"
    connection, cursor = connect_mysql(host, database, user, password)
    sensorId, productId = get_product_byName(sensorName,productName)
    insert_query = ("INSERT INTO image_table (image_id, sensor_id, product_id, image_time, tile_level_min, tile_level_max, crs, band, bbox, image_path, description) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s), %s, %s)")
    imageId = str(uuid.uuid5(namespace, uuidName))
    data = (imageId, sensorId, productId, imageTime, tileLevelMin, tileLevelMax, crs, band, bbox, imagePath, description)
    cursor.execute(insert_query, data)
    connection.commit()

    print(cursor.rowcount, f"Image {imageId} inserted!")
    cursor.close()
    connection.close()

def insert_tile(imageId, tileLevel, bbox, tilePath):
    uuidName = "tileName"
    connection, cursor = connect_mysql(host, database, user, password)
    insert_query = "INSERT INTO tile_table (tile_id, image_id, tile_level, bbox, tile_path) VALUES (%s, %s, %s, %s, %s)"
    tileId = str(uuid.uuid5(namespace, uuidName))
    data = (tileId, imageId, tileLevel, bbox, tilePath)
    cursor.execute(insert_query, data)
    connection.commit()

    print(cursor.rowcount, f"Tile {tileId} inserted!")
    cursor.close()
    connection.close()
