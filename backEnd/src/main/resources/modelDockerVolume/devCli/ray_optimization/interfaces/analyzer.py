"""
代码分析器接口定义
符合接口隔离原则(ISP)，定义最小化的分析器接口
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Tuple

class ICodeAnalyzer(ABC):
    """代码分析器接口"""
    
    @abstractmethod
    def analyze(self, code: str) -> Dict[str, Any]:
        """
        分析代码并返回分析结果
        
        Args:
            code: 要分析的Python代码字符串
            
        Returns:
            包含分析结果的字典，至少包含:
            - complexity_score: 复杂度分数(0-100)
            - parallelizable: 是否可并行化
            - metrics: 详细的度量指标
        """
        pass
    
    @abstractmethod
    def calculate_complexity_score(self, metrics: Dict[str, Any]) -> float:
        """
        根据度量指标计算复杂度分数
        
        Args:
            metrics: 分析得到的度量指标
            
        Returns:
            复杂度分数(0-100)
        """
        pass

class IPatternDetector(ABC):
    """代码模式检测器接口"""
    
    @abstractmethod
    def detect_patterns(self, ast_node: Any) -> List[Dict[str, Any]]:
        """
        检测代码中的可优化模式
        
        Args:
            ast_node: Python AST节点
            
        Returns:
            检测到的模式列表
        """
        pass 