from ogms_xfer import OGMS_Xfer as xfer
import pandas as pd
import os
import sys

# 设置 pandas 显示选项
pd.set_option('display.max_columns', None)
pd.set_option('display.expand_frame_repr', False)

# 1. 初始化
config_file_path = "config.json"
if not os.path.exists(config_file_path):
    print(f"配置文件不存在: {os.path.abspath(config_file_path)}")
    sys.exit(1)

try:
    print(f"正在使用配置文件初始化: {config_file_path}")
    xfer.initialize(config_file_path)
except Exception as e:
    print(f"初始化期间发生错误: {e}")
    # 这里可能会因为 vector_database 配置导致连接失败，如果不需要 postgres，可以尝试忽略 vector 相关报错
    # 但 ogms_xfer 内部没有很好的容错，这里只能尽量继续

print("-" * 50)
print("示例：查询影像元数据 (使用 MySQL)")
print("-" * 50)

# 使用 pandas 直接查询 MySQL 中的 scene 表
# 这样可以避开 PostGIS/GeoPandas 的依赖问题
try:
    # 获取数据库连接引擎 (SQLAlchemy Engine, underlying driver is pymysql or mysql-connector)
    # 假设 ogms_xfer 初始化成功创建了 satellite-database 的 client
    from ogms_xfer.application.provider import Singleton
    db_client = Singleton.get_instance("satellite-database")
    
    if not db_client:
        print("错误: 无法获取 satellite-database 客户端实例。")
        sys.exit(1)
        
    print("数据库连接成功，正在执行 SQL 查询...")
    
    # 简单的 SQL 查询，获取最近的 5 条影像记录
    sql_query = "SELECT scene_id, scene_name, product_id, scene_time, cloud FROM scene ORDER BY scene_time DESC LIMIT 5"
    
    # 使用 pandas 读取 SQL
    df = pd.read_sql(sql_query, db_client.engine)
    
    if not df.empty:
        print(f"查询成功，找到 {len(df)} 条记录：")
        print(df)
    else:
        print("查询成功，但未找到记录。")

except ImportError as e:
    print(f"导入错误: {e}")
except Exception as e:
    print(f"发生错误: {e}")
    if "psycopg2" in str(e):
        print("提示: 似乎依然尝试了连接 PostgreSQL，请检查配置或代码依赖。")

print("-" * 50)
print("示例结束")
