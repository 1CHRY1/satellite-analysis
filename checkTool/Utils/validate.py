from Utils.mySqlUtils import *
from Utils.minioUtil import *
import json
from glob import glob
import xml.etree.ElementTree as ET
import sys
import os
import re

def load_db_config(db_config_path):
    DB_CONFIG = {}
    print("[INFO] ------------ Load DB Config ------------\r\n")
    print("[INFO] Loading dbConfig.json...\r\n")
    if not os.path.exists(db_config_path):
        exit_with_error(f"[ERROR] DB config file not found: {db_config_path}\r\n")
    json_db_config = json.load(open(db_config_path, 'r',encoding="UTF-8"))
    for key, value in json_db_config.items():
        DB_CONFIG[key] = value
    field_list = ["MYSQL_HOST", "MYSQL_RESOURCE_PORT", "MYSQL_RESOURCE_DB", "MYSQL_USER", "MYSQL_PWD", "MINIO_PORT", "MINIO_IP", "MINIO_ACCESS_KEY", "MINIO_SECRET_KEY", "MINIO_SECURE", "MINIO_IMAGES_BUCKET"]
    for field in field_list:
        if DB_CONFIG.get(field) is None:
            exit_with_error(f"[ERROR] {field} is not set in {db_config_path}\r\n")
    print("[INFO] dbConfig.json loaded\r\n")
    set_initial_mysql_config(DB_CONFIG)
    set_initial_minio_config(DB_CONFIG)
    print("[INFO] ----------------------------------------\r\n\r\n")
    return DB_CONFIG

def verify_db_config(DB_CONFIG):
    print("[INFO] ------------ DB Config Verify ------------\r\n")
    print("[INFO] Verifying dbConfig...\r\n")
    connection, cursor = connect_mysql(DB_CONFIG["MYSQL_HOST"], DB_CONFIG["MYSQL_RESOURCE_PORT"],
                                       DB_CONFIG["MYSQL_RESOURCE_DB"], DB_CONFIG["MYSQL_USER"],
                                       DB_CONFIG["MYSQL_PWD"])
    if cursor is None:
        exit_with_error("[ERROR] Database connection failed\r\n")
    try:
        client = getMinioClient()
        buckets = client.list_buckets()
    except Exception as e:
        exit_with_error(f"[ERROR] MinIO connection failed: {e}\r\n")
    print("[SUCCESS] Verification successful\r\n")
    print("[INFO] ----------------------------------------\r\n\r\n")

def append_to_log(log_path, log_content):
    try:
        # 如果日志文件所在目录不存在，则自动创建
        dir_name = os.path.dirname(log_path)
        if dir_name and not os.path.exists(dir_name):
            os.makedirs(dir_name, exist_ok=True)
        with open(log_path, 'a', encoding="UTF-8") as f:
            f.write(log_content)
    except Exception as e:
        exit_with_error(f"Error appending to log: {e}\r\nLog path: {log_path}\r\n")

def exit_with_error(error_message):
    print(f"\033[91m{error_message}\033[0m")
    sys.exit(1)