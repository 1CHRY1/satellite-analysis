import os

# 获取指定目录下所有子文件夹
def get_folders(directory):
    return [f.path for f in os.scandir(directory) if f.is_dir()]


# 获取指定文件夹下的所有 .tif 文件
def get_tif_files(folder):
    return [f.path for f in os.scandir(folder) if f.is_file() and f.name.lower().endswith('.tif')]
