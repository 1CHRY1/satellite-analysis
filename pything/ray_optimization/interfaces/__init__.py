"""
接口定义包
定义Ray优化模块的所有抽象接口
"""
from .analyzer import ICodeAnalyzer
from .optimizer import ICodeOptimizer
from .executor import IExecutor

__all__ = ['ICodeAnalyzer', 'ICodeOptimizer', 'IExecutor'] 