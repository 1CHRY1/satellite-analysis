"""
代码优化器接口定义
定义代码转换和优化的抽象接口
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional

class ICodeOptimizer(ABC):
    """代码优化器接口"""
    
    @abstractmethod
    def optimize(self, code: str, analysis_result: Dict[str, Any]) -> str:
        """
        根据分析结果优化代码
        
        Args:
            code: 原始代码
            analysis_result: 代码分析结果
            
        Returns:
            优化后的代码
        """
        pass
    
    @abstractmethod
    def can_optimize(self, analysis_result: Dict[str, Any]) -> bool:
        """
        判断代码是否可以优化
        
        Args:
            analysis_result: 代码分析结果
            
        Returns:
            是否可以优化
        """
        pass

class IOptimizationStrategy(ABC):
    """优化策略接口"""
    
    @abstractmethod
    def apply(self, ast_node: Any) -> Any:
        """
        应用优化策略到AST节点
        
        Args:
            ast_node: Python AST节点
            
        Returns:
            优化后的AST节点
        """
        pass
    
    @abstractmethod
    def is_applicable(self, ast_node: Any) -> bool:
        """
        检查策略是否适用于给定的AST节点
        
        Args:
            ast_node: Python AST节点
            
        Returns:
            是否适用
        """
        pass 