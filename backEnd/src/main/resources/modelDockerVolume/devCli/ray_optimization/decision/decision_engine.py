"""
优化决策引擎
根据代码分析结果和系统状态决定是否使用Ray优化
"""
import logging
from typing import Dict, Any, Tuple, Optional
from .threshold_manager import ThresholdManager

logger = logging.getLogger(__name__)

class OptimizationDecisionEngine:
    """决策引擎，决定是否对代码进行优化"""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化决策引擎
        
        Args:
            config: 配置字典
        """
        config = config or {}
        self.threshold_manager = ThresholdManager(config.get('threshold', {}))
        self.enabled = config.get('enabled', True)
        self.min_lines = config.get('min_lines', 5)  # 最小代码行数
        self.max_lines = config.get('max_lines', 10000)  # 最大代码行数
        
    def should_optimize(self, analysis_result: Dict[str, Any], 
                       context: Optional[Dict[str, Any]] = None) -> Tuple[bool, float, float]:
        """
        决定是否应该优化代码
        
        Args:
            analysis_result: 代码分析结果
            context: 执行上下文
            
        Returns:
            (是否优化, 优化分数, 当前阈值)
        """
        # 检查是否启用优化
        if not self.enabled:
            logger.info("Ray optimization is disabled")
            return False, 0, 100
            
        # 检查分析结果
        if 'error' in analysis_result:
            logger.warning(f"Code analysis error: {analysis_result['error']}")
            return False, 0, 100
            
        # 获取优化分数
        optimization_score = self._calculate_optimization_score(analysis_result)
        
        # 获取动态阈值
        threshold = self.threshold_manager.get_dynamic_threshold(context)
        
        # 检查代码规模
        metrics = analysis_result.get('metrics', {})
        total_lines = metrics.get('total_lines', 0)
        
        if total_lines < self.min_lines:
            logger.info(f"Code too small ({total_lines} lines), skipping optimization")
            return False, optimization_score, threshold
            
        if total_lines > self.max_lines:
            logger.warning(f"Code too large ({total_lines} lines), skipping optimization for safety")
            return False, optimization_score, threshold
            
        # 做出决策
        should_optimize = optimization_score > threshold
        
        logger.info(f"Optimization decision: score={optimization_score:.2f}, "
                   f"threshold={threshold:.2f}, optimize={should_optimize}")
        
        return should_optimize, optimization_score, threshold
        
    def _calculate_optimization_score(self, analysis_result: Dict[str, Any]) -> float:
        """
        计算综合优化分数
        
        Args:
            analysis_result: 代码分析结果
            
        Returns:
            优化分数 (0-100)
        """
        # 基础分数来自复杂度分析
        base_score = analysis_result.get('complexity_score', 0)
        
        # 根据检测到的模式调整分数
        metrics = analysis_result.get('metrics', {})
        patterns = analysis_result.get('patterns', [])
        
        # 加分项
        bonus_score = 0
        
        # 有可并行化循环
        if metrics.get('parallelizable_loops', 0) > 0:
            bonus_score += min(metrics['parallelizable_loops'] * 5, 15)
            
        # 有数据处理操作
        if metrics.get('numpy_operations', 0) > 0:
            bonus_score += min(metrics['numpy_operations'] * 3, 10)
            
        if metrics.get('pandas_operations', 0) > 0:
            bonus_score += min(metrics['pandas_operations'] * 4, 10)
            
        # 有I/O密集操作
        if metrics.get('io_operations', 0) > 0:
            bonus_score += min(metrics['io_operations'] * 2, 8)
            
        # 检测到的优化模式
        pattern_bonus = len(patterns) * 3
        bonus_score += min(pattern_bonus, 12)
        
        # 减分项
        penalty_score = 0
        
        # 代码太简单
        if metrics.get('total_lines', 0) < 10:
            penalty_score += 20
            
        # 没有循环或数据处理
        if (metrics.get('loop_count', 0) == 0 and 
            metrics.get('numpy_operations', 0) == 0 and
            metrics.get('pandas_operations', 0) == 0):
            penalty_score += 30
            
        # 计算最终分数
        final_score = base_score + bonus_score - penalty_score
        final_score = max(0, min(100, final_score))
        
        logger.debug(f"Optimization score calculation: base={base_score}, "
                    f"bonus={bonus_score}, penalty={penalty_score}, "
                    f"final={final_score}")
        
        return final_score
        
    def update_history(self, code_hash: str, execution_result: Dict[str, Any]) -> None:
        """
        更新历史执行记录
        
        Args:
            code_hash: 代码哈希值
            execution_result: 执行结果
        """
        # TODO: 实现历史记录存储
        # 这里可以存储到文件或数据库中
        pass 