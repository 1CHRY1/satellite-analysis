"""
Copy from OpenGMS Model-Store, QingJun Guo
"""
import os
import numpy as np
import rasterio
from rasterio.warp import reproject, Resampling

# 定义一个函数，用于重采样栅格数据到指定栅格的形状和空间参考系
def reproject_raster(input_raster, input_meta, output_meta):
    output_raster = np.empty((output_meta['height'], output_meta['width']), dtype=np.float32)
    reproject(
        source=input_raster,
        destination=output_raster,
        src_transform=input_meta['transform'],
        src_crs=input_meta['crs'],
        dst_transform=output_meta['transform'],
        dst_crs=output_meta['crs'],
        resampling=Resampling.bilinear
    )
    return output_raster

def landslide_probability_model(inputTifFile1, inputTifFile2, inputTifFile3, Output1, Output2):
    """
    计算滑坡概率
    """
    base_tif = inputTifFile1
    pga_tif = inputTifFile2
    intensity_tif = inputTifFile3
    
    # 打开并读取基准栅格文件
    with rasterio.open(base_tif) as src_base:
        base_raster = src_base.read(1).astype(float)
        base_meta = src_base.meta
        
    # 打开并读取 PGA 栅格文件
    with rasterio.open(pga_tif) as src_pga:
        pga_raster = src_pga.read(1).astype(float)
        pga_meta = src_pga.meta
        pga_transform = src_pga.transform
        pga_crs = src_pga.crs
        pga_nodata = src_pga.nodata
        
    # 重采样基准栅格文件到 PGA 栅格的形状和空间参考系
    base_resampled_to_pga = reproject_raster(base_raster, base_meta, pga_meta)

    # 创建掩膜，排除无数据值
    mask = (pga_raster != pga_nodata) & (base_resampled_to_pga != base_meta['nodata'])

    # 计算 PGA 分布的滑坡概率，仅在有数据值的区域进行计算
    pga_probability = np.full(pga_raster.shape, pga_nodata, dtype=np.float32)
    pga_probability[mask] = 1 / (1 + np.power(2.7182818284, 0 - 0.032031374972481 * pga_raster[mask] - base_resampled_to_pga[mask]))

    # 保存 PGA 滑坡概率结果
    # pga_output = os.path.join(Output1, "outputPGApbtyTif.tif")
    pga_meta.update(dtype=rasterio.float32, nodata=pga_nodata)

    with rasterio.open(Output1, 'w', **pga_meta) as dst:
        dst.write(pga_probability, 1)

    print("PGA landslide probability calculation completed, results saved as:", Output1)
    
    # 打开并读取强度栅格文件
    with rasterio.open(intensity_tif) as src_intensity:
        intensity_raster = src_intensity.read(1).astype(float)
        intensity_meta = src_intensity.meta
        intensity_transform = src_intensity.transform
        intensity_crs = src_intensity.crs
        intensity_nodata = src_intensity.nodata

    # 重采样基准栅格文件到强度栅格的形状和空间参考系
    base_resampled_to_intensity = reproject_raster(base_raster, base_meta, intensity_meta)

    # 掩膜无数据值
    mask = (intensity_raster != intensity_nodata) & (base_resampled_to_intensity != base_meta['nodata'])

    # 计算基于强度分布的滑坡概率，仅在有数据值的区域进行计算
    intensity_probability = np.full(intensity_raster.shape, intensity_nodata, dtype=np.float32)
    intensity_probability[mask] = 1 / (1 + np.power(2.7182818284, 0 - 0.00250165038535077 * np.power(2.7182818284, 0.6931 * intensity_raster[mask]) - base_resampled_to_intensity[mask]))

    # 保存强度滑坡概率结果
    # intensity_output = os.path.join(Output2, "outputIntensitypbtyTif.tif")
    intensity_meta.update(dtype=rasterio.float32, nodata=intensity_nodata)

    with rasterio.open(Output2, 'w', **intensity_meta) as dst:
        dst.write(intensity_probability, 1)

    print("Intensity landslide probability calculation completed, results saved as:", Output2)

# if __name__ == "__main__":
    
#     data_dir = os.path.join(os.path.dirname(__file__), "data")
#     output_dir = os.path.join(os.path.dirname(__file__), "output")
    
#     input_base_tif = os.path.join(data_dir, "base.tif")
#     input_pga_tif = os.path.join(data_dir, "pga.tif")
#     input_intensity_tif = os.path.join(data_dir, "intensity.tif")
    
#     output_pga_tif = os.path.join(output_dir, "outputPGApbtyTif.tif")
#     output_intensity_tif = os.path.join(output_dir, "outputIntensitypbtyTif.tif")
    
#     Execute(input_base_tif, input_pga_tif, input_intensity_tif, output_pga_tif, output_intensity_tif)