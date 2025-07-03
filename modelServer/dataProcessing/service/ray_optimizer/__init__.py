"""
Ray优化器包 - 为用户Python代码提供智能Ray分布式计算优化

主要功能：
- 代码安全检查和资源限制
- 智能代码分析和模式识别  
- 领域特定的优化策略（NumPy、Pandas、文件处理等）
- Ray分布式执行包装
- 详细的优化报告和性能监控

使用示例：
    from service.ray_optimizer import optimize_user_code
    
    optimized_code, report = optimize_user_code(
        code="import numpy as np; result = np.sum([1,2,3])",
        user_id="user123",
        project_id="project456"
    )
"""

from .main import optimize_user_code, optimize_user_code_advanced, create_optimized_script
from .exceptions import SecurityError, OptimizationError, ResourceLimitError
from .config import RayOptimizerConfig

__version__ = "1.0.0"
__author__ = "Satellite Analysis Team"

# 主要入口函数
__all__ = [
    'optimize_user_code',
    'optimize_user_code_advanced', 
    'create_optimized_script',
    'SecurityError',
    'OptimizationError', 
    'ResourceLimitError',
    'RayOptimizerConfig'
] 