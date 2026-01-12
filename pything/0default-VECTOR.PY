from ogms_xfer import OGMS_Xfer as xfer
config_file_path = "config.json"

# 初始化 (如果缺少 psycopg2 包，这里会直接报错，请根据报错信息安装对应依赖)
xfer.initialize(config_file_path)

##################################################
import pandas as pd

# 设置显示格式
pd.set_option('display.max_columns', None)
pd.set_option('display.expand_frame_repr', False)
pd.set_option('display.unicode.ambiguous_as_wide', True)
pd.set_option('display.unicode.east_asian_width', True)

def preview_info(df):
    print("-" * 50)
    if df is not None and not df.empty:
        print(f"查询成功，共 {len(df)} 条记录。")
        # 打印前5条
        print(df.head())
    else:
        print("未查询到数据。")
    print("-" * 50)

# 示例：检索矢量数据
# 注意：此操作需要 python 环境安装 'psycopg2' (连接数据库) 和 'geopandas' (处理几何数据)
print("检索矢量数据表: gis_db.vector_datasets")

try:
    # 1. 指定表名
    table_name = "gis_db.vector_datasets"
    
    # 2. 读取数据 (指定几何列名为 boundary)
    vector_df = xfer.Vector(table_name).read(geom_col="boundary")
    
    # 3. 预览结果
    preview_info(vector_df)
    
except Exception as e:
    print(f"\n执行出错: {e}")
    if "No module named" in str(e):
        print("提示: 您的环境缺少必要的 Python 库 (通常是 psycopg2 或 geopandas)。")
