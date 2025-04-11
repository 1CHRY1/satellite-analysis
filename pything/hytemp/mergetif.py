from osgeo import gdal
import os
import time

def merge_tiles(input_files, output_path):
    # 动态生成内存中的VRT文件路径
    output_vrt_in_memory = "/vsimem/merged.vrt"
    
    # 创建VRT文件并保存在内存中
    vrt_options = gdal.BuildVRTOptions(separate=False)  # 将所有输入文件合并为一个波段
    vrt = gdal.BuildVRT(output_vrt_in_memory, input_files, options=vrt_options)
    if vrt is None:
        raise Exception("Failed to create VRT file in memory")

    # 将VRT文件转换为TIF文件，并保存在内存中
    translate_options = gdal.TranslateOptions(format="GTiff")
    gdal.Translate(output_path, vrt, options=translate_options)

    gdal.Unlink(output_vrt_in_memory)
    print(f"瓦片合并完成...")
    return output_path


def dir_to_tif_list(dir_path):
    tif_list = []
    for file in os.listdir(dir_path):
        if file.endswith(".tif"):
            tif_list.append(os.path.join(dir_path, file))
    print(f"已获取到{len(tif_list)}个瓦片...")
    return tif_list


if __name__ == "__main__":
    input_dir = "D:\\edgedownload\\multi_output\\LT51190382000261BJC00_B3"
    output_path = "D:\\edgedownload\\multi_output\\LT51190382000261BJC00_B3\\merged.tif"
    start_time = time.time()
    merge_tiles( dir_to_tif_list(input_dir), output_path)
    end_time = time.time()
    print(f"合并完成，用时{end_time - start_time}秒")