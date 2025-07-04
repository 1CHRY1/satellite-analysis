"""
Ray优化器异常定义模块

定义了Ray优化过程中可能出现的各种异常类型
"""


class SecurityError(Exception):
    """
    安全检查异常
    
    当用户代码包含不安全操作时抛出此异常，如：
    - 禁止的函数调用（eval, exec等）
    - 危险的模块导入（os, subprocess等）
    - 超出安全限制的操作
    """
    pass


class OptimizationError(Exception):
    """
    优化过程异常
    
    当代码优化过程中出现错误时抛出此异常，如：
    - 代码语法错误
    - AST解析失败
    - 优化策略应用失败
    """
    pass


class ResourceLimitError(Exception):
    """
    资源限制异常
    
    当执行过程中超出资源限制时抛出此异常，如：
    - 内存使用超限
    - CPU时间超限
    - 执行时间超时
    """
    pass


class RayConnectionError(Exception):
    """
    Ray连接异常
    
    当Ray集群连接或初始化失败时抛出此异常
    """
    pass


class CodeAnalysisError(Exception):
    """
    代码分析异常
    
    当代码分析过程中出现错误时抛出此异常
    """
    pass 