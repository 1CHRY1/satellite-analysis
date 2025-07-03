"""
Ray优化器配置管理模块

统一管理所有配置参数，支持环境变量覆盖和动态配置
"""

import os
from typing import Set, Dict, Any
from dataclasses import dataclass


@dataclass
class RayOptimizerConfig:
    """Ray优化器配置类"""
    
    # 安全限制配置
    MAX_EXECUTION_TIME: int = int(os.getenv('RAY_MAX_EXECUTION_TIME', 300))  # 5分钟
    MAX_MEMORY_MB: int = int(os.getenv('RAY_MAX_MEMORY_MB', 2048))          # 2GB
    MAX_FILE_SIZE: int = int(os.getenv('RAY_MAX_FILE_SIZE', 10 * 1024 * 1024))  # 10MB
    
    # 代码执行超时配置
    CODE_TIMEOUT_SECONDS: int = int(os.getenv('RAY_CODE_TIMEOUT', 30))
    
    # Ray集群配置
    RAY_CONFIG: Dict[str, Any] = None
    
    # 允许的模块导入白名单
    ALLOWED_IMPORTS: Set[str] = None
    
    # 禁止的操作黑名单
    FORBIDDEN_OPERATIONS: Set[str] = None
    
    # 优化策略配置
    OPTIMIZATION_ENABLED: bool = os.getenv('RAY_OPTIMIZATION_ENABLED', 'true').lower() == 'true'
    
    # 缓存配置
    CACHE_ENABLED: bool = os.getenv('RAY_CACHE_ENABLED', 'true').lower() == 'true'
    CACHE_TTL_MINUTES: int = int(os.getenv('RAY_CACHE_TTL', 10))
    
    def __post_init__(self):
        """初始化后处理，设置默认值"""
        if self.RAY_CONFIG is None:
            self.RAY_CONFIG = {
                'num_cpus': None,  # 自动检测
                'memory_limit': '4GB',
                'object_store_memory': '1GB',
                'ignore_reinit_error': True
            }
        
        if self.ALLOWED_IMPORTS is None:
            self.ALLOWED_IMPORTS = {
                # 科学计算库
                'numpy', 'pandas', 'scipy', 'sklearn', 'statsmodels',
                
                # 可视化库
                'matplotlib', 'seaborn', 'plotly', 'folium', 'bokeh',
                
                # 图像处理库
                'cv2', 'PIL', 'skimage', 'imageio',
                
                # 地理空间库
                'rasterio', 'geopandas', 'shapely', 'fiona', 'pyproj',
                
                # 数据处理库
                'xarray', 'dask', 'h5py', 'netCDF4',
                
                # 标准库
                'math', 'random', 'json', 'csv', 'datetime', 'time',
                'collections', 're', 'itertools', 'functools', 'operator',
                'statistics', 'bisect', 'heapq', 'copy', 'pickle',
                'urllib', 'base64', 'hashlib', 'uuid', 'logging',
                
                # 数值和字符串处理
                'decimal', 'fractions', 'string', 'textwrap',
                
                # 并发处理（受限）
                'threading', 'multiprocessing', 'concurrent',
                
                # 测试库
                'unittest', 'pytest', 'nose'
            }
        
        if self.FORBIDDEN_OPERATIONS is None:
            self.FORBIDDEN_OPERATIONS = {
                # 系统调用
                'os.system', 'os.popen', 'os.spawn', 'os.exec',
                'subprocess.call', 'subprocess.run', 'subprocess.Popen',
                
                # 动态执行
                'eval', 'exec', 'compile', '__import__',
                'execfile', 'reload', 'importlib.import_module',
                
                # 文件系统操作（限制）
                'open', 'file', 'input', 'raw_input',
                
                # 网络操作
                'socket', 'urllib.request', 'requests.get', 'requests.post',
                'http.client', 'ftplib', 'smtplib',
                
                # 危险导入
                'import os', 'import subprocess', 'import sys',
                'from os import', 'from subprocess import', 'from sys import'
            }


# 全局配置实例
config = RayOptimizerConfig()


def get_config() -> RayOptimizerConfig:
    """获取全局配置实例"""
    return config


def update_config(**kwargs) -> None:
    """动态更新配置"""
    global config
    for key, value in kwargs.items():
        if hasattr(config, key):
            setattr(config, key, value)
        else:
            raise ValueError(f"Unknown config parameter: {key}")


def reset_config() -> None:
    """重置配置到默认值"""
    global config
    config = RayOptimizerConfig()


# 优化策略配置
class OptimizationConfig:
    """优化策略配置"""
    
    # NumPy优化配置
    NUMPY_PARALLEL_THRESHOLD = 1000  # 数组大小阈值
    NUMPY_CHUNK_SIZE = 4  # 并行块数
    
    # Pandas优化配置  
    PANDAS_CHUNK_SIZE = 10000  # DataFrame分块大小
    PANDAS_PARALLEL_THRESHOLD = 5000  # 并行化阈值
    
    # 文件处理配置
    FILE_BATCH_SIZE = 10  # 批处理文件数量
    
    # 循环优化配置
    LOOP_PARALLEL_THRESHOLD = 100  # 循环并行化阈值
    
    # 性能估算参数
    SPEEDUP_FACTORS = {
        'numpy': 2.5,
        'pandas': 3.0,
        'file_processing': 4.0,
        'image_processing': 3.5,
        'generic_loops': 2.0
    }


# 优化策略配置实例
optimization_config = OptimizationConfig() 