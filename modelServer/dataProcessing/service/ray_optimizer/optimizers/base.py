"""
基础优化器类 - 定义所有优化器的通用接口和行为
"""

import re
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Tuple, Optional
from dataclasses import dataclass, field

from ..config import optimization_config


@dataclass
class OptimizationResult:
    """优化结果数据类"""
    
    optimized_code: str
    applied_optimizations: List[str] = field(default_factory=list)
    estimated_speedup: float = 1.0
    ray_features_used: List[str] = field(default_factory=list)
    optimization_details: Dict[str, Any] = field(default_factory=dict)
    
    def add_optimization(self, name: str, speedup_factor: float = 1.0, details: Dict[str, Any] = None):
        """添加优化记录"""
        self.applied_optimizations.append(name)
        self.estimated_speedup *= speedup_factor
        if details:
            self.optimization_details[name] = details


class BaseOptimizer(ABC):
    """优化器基类"""
    
    def __init__(self, name: str):
        self.name = name
        self.config = optimization_config
        self.optimization_patterns = self._init_patterns()
    
    @abstractmethod
    def _init_patterns(self) -> List[Dict[str, Any]]:
        """初始化优化模式，子类必须实现"""
        pass
    
    @abstractmethod
    def can_optimize(self, code: str, analysis_result: Any) -> bool:
        """检查是否可以优化给定代码"""
        pass
    
    @abstractmethod
    def optimize(self, code: str, analysis_result: Any) -> OptimizationResult:
        """执行优化，返回优化结果"""
        pass
    
    def _apply_pattern_replacement(self, code: str, pattern: str, replacement: str) -> str:
        """应用模式替换"""
        return re.sub(pattern, replacement, code)
    
    def _generate_ray_functions(self) -> str:
        """生成Ray远程函数，子类可以重写"""
        return ""
    
    def _estimate_speedup(self, optimization_count: int) -> float:
        """估算加速比"""
        base_speedup = self.config.SPEEDUP_FACTORS.get(self.name.lower(), 2.0)
        return min(base_speedup * (1 + optimization_count * 0.1), 8.0)
    
    def _check_optimization_threshold(self, analysis_result: Any) -> bool:
        """检查是否达到优化阈值"""
        return True  # 默认总是可以优化
    
    def _add_ray_imports(self, code: str) -> str:
        """添加必要的Ray导入"""
        imports = [
            "import ray",
            "import numpy as np",
            "import pandas as pd"
        ]
        
        existing_imports = []
        for import_line in imports:
            if import_line not in code:
                existing_imports.append(import_line)
        
        if existing_imports:
            import_block = "\n".join(existing_imports) + "\n\n"
            return import_block + code
        
        return code 