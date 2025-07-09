"""
Ray优化器
协调代码分析、转换和优化的主要组件
"""
import logging
from typing import Dict, Any, Optional
from ..interfaces.optimizer import ICodeOptimizer
from ..analyzers.pattern_detector import PatternDetector
from .code_transformer import CodeTransformer

logger = logging.getLogger(__name__)

class RayOptimizer(ICodeOptimizer):
    """Ray优化器实现"""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化优化器
        
        Args:
            config: 配置字典
        """
        config = config or {}
        self.pattern_detector = PatternDetector()
        self.code_transformer = CodeTransformer()
        self.optimization_level = config.get('optimization_level', 1)  # 1-3
        
    def optimize(self, code: str, analysis_result: Dict[str, Any]) -> str:
        """
        根据分析结果优化代码
        
        Args:
            code: 原始代码
            analysis_result: 代码分析结果
            
        Returns:
            优化后的代码
        """
        # 检查是否可以优化
        if not self.can_optimize(analysis_result):
            logger.info("Code cannot be optimized, returning original")
            return code
            
        try:
            # 获取AST
            ast_tree = analysis_result.get('ast_tree')
            if not ast_tree:
                logger.warning("No AST tree in analysis result")
                return code
                
            # 检测可优化模式
            patterns = self.pattern_detector.detect_patterns(ast_tree)
            
            if not patterns:
                logger.info("No optimizable patterns detected")
                return code
                
            logger.info(f"Detected {len(patterns)} optimizable patterns")
            
            # 根据优化级别过滤模式
            patterns = self._filter_patterns_by_level(patterns)
            
            # 应用代码转换
            optimized_code = self.code_transformer.transform(code, patterns)
            
            # 添加Ray初始化代码
            optimized_code = self._add_ray_initialization(optimized_code)
            
            return optimized_code
            
        except Exception as e:
            logger.error(f"Error during code optimization: {e}")
            return code
            
    def can_optimize(self, analysis_result: Dict[str, Any]) -> bool:
        """
        判断代码是否可以优化
        
        Args:
            analysis_result: 代码分析结果
            
        Returns:
            是否可以优化
        """
        # 检查是否有错误
        if 'error' in analysis_result:
            return False
            
        # 检查是否有可并行化的内容
        metrics = analysis_result.get('metrics', {})
        
        # 至少需要有循环或数据处理操作
        has_loops = metrics.get('loop_count', 0) > 0
        has_data_ops = (metrics.get('numpy_operations', 0) > 0 or
                       metrics.get('pandas_operations', 0) > 0)
        has_list_comp = metrics.get('list_comprehensions', 0) > 0
        
        return has_loops or has_data_ops or has_list_comp
        
    def _filter_patterns_by_level(self, patterns: list) -> list:
        """
        根据优化级别过滤模式
        
        Args:
            patterns: 检测到的模式列表
            
        Returns:
            过滤后的模式列表
        """
        if self.optimization_level == 1:
            # 保守优化：只优化简单的map模式
            return [p for p in patterns if p['type'] == 'map_pattern']
            
        elif self.optimization_level == 2:
            # 中等优化：包括map、reduce和列表推导式
            allowed_types = {'map_pattern', 'accumulation_pattern', 
                           'list_comprehension'}
            return [p for p in patterns if p['type'] in allowed_types]
            
        else:  # level 3
            # 激进优化：所有模式
            return patterns
            
    def _add_ray_initialization(self, code: str) -> str:
        """
        添加Ray初始化代码
        
        Args:
            code: 优化后的代码
            
        Returns:
            包含Ray初始化的代码
        """
        # 检查是否已经有Ray初始化
        if 'ray.init' in code:
            return code
            
        # 添加Ray初始化
        init_code = """
# Ray initialization (added by optimizer)
import ray
if not ray.is_initialized():
    ray.init(num_cpus=4, ignore_reinit_error=True)

"""
        
        # 找到合适的插入位置（在导入语句之后）
        lines = code.split('\n')
        insert_pos = 0
        
        for i, line in enumerate(lines):
            if line.strip() and not line.startswith(('import', 'from')):
                insert_pos = i
                break
                
        # 插入初始化代码
        lines.insert(insert_pos, init_code)
        
        return '\n'.join(lines) 