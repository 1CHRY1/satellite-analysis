"""
Ray优化器策略包

包含各种领域特定的优化策略：
- NumPy优化：并行化数值计算
- Pandas优化：并行化数据处理
- 文件处理优化：批量文件操作
- 图像处理优化：并行图像处理
- 循环优化：通用循环并行化
"""

from .base import BaseOptimizer, OptimizationResult

__all__ = [
    'BaseOptimizer',
    'OptimizationResult'
] 