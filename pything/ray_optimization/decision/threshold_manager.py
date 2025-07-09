"""
动态阈值管理器
根据系统资源和历史性能动态调整决策阈值
"""
import logging
import psutil
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class ThresholdManager:
    """阈值管理器，负责动态计算优化阈值"""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化阈值管理器
        
        Args:
            config: 配置字典，包含基础阈值和调整因子
        """
        config = config or {}
        
        # 基础阈值配置
        self.base_threshold = config.get('base_threshold', 50)
        self.min_threshold = config.get('min_threshold', 30)
        self.max_threshold = config.get('max_threshold', 90)
        
        # 调整因子
        self.factors = {
            'system_load': config.get('system_load_factor', 1.0),
            'memory_pressure': config.get('memory_pressure_factor', 1.0),
            'cpu_pressure': config.get('cpu_pressure_factor', 1.0),
            'history_performance': config.get('history_performance_factor', 1.0),
        }
        
    def get_dynamic_threshold(self, context: Optional[Dict[str, Any]] = None) -> float:
        """
        根据当前系统状态动态计算阈值
        
        Args:
            context: 上下文信息，包含历史性能数据等
            
        Returns:
            动态计算的阈值
        """
        context = context or {}
        
        # 从基础阈值开始
        threshold = self.base_threshold
        
        # 获取系统资源状态
        system_status = self._get_system_status()
        
        # 根据CPU使用率调整
        cpu_percent = system_status['cpu_percent']
        if cpu_percent > 80:
            # CPU使用率高，提高阈值（减少优化任务）
            threshold *= 1.2
            logger.info(f"High CPU usage ({cpu_percent}%), increasing threshold")
        elif cpu_percent < 30:
            # CPU使用率低，降低阈值（增加优化任务）
            threshold *= 0.9
            logger.info(f"Low CPU usage ({cpu_percent}%), decreasing threshold")
            
        # 根据内存使用率调整
        memory_percent = system_status['memory_percent']
        available_memory_mb = system_status['available_memory_mb']
        
        if memory_percent > 85 or available_memory_mb < 1024:
            # 内存紧张，提高阈值
            threshold *= 1.3
            logger.info(f"Memory pressure detected (usage: {memory_percent}%), increasing threshold")
        elif memory_percent < 50 and available_memory_mb > 4096:
            # 内存充足，降低阈值
            threshold *= 0.85
            logger.info(f"Plenty of memory available, decreasing threshold")
            
        # 根据历史性能调整
        if context.get('has_history', False):
            avg_speedup = context.get('avg_speedup', 1.0)
            failure_rate = context.get('failure_rate', 0.0)
            
            if avg_speedup < 1.2:
                # 历史优化效果不佳
                threshold *= 1.4
                logger.info(f"Low historical speedup ({avg_speedup}x), increasing threshold")
            elif avg_speedup > 2.0 and failure_rate < 0.1:
                # 历史优化效果很好
                threshold *= 0.8
                logger.info(f"Good historical performance, decreasing threshold")
                
            if failure_rate > 0.3:
                # 失败率高
                threshold *= 1.5
                logger.warning(f"High failure rate ({failure_rate}), significantly increasing threshold")
                
        # 应用边界限制
        threshold = max(self.min_threshold, min(threshold, self.max_threshold))
        
        logger.debug(f"Dynamic threshold calculated: {threshold:.2f}")
        return threshold
        
    def _get_system_status(self) -> Dict[str, Any]:
        """获取当前系统资源状态"""
        try:
            # CPU使用率（短期平均）
            cpu_percent = psutil.cpu_percent(interval=0.1)
            
            # 内存状态
            memory = psutil.virtual_memory()
            memory_percent = memory.percent
            available_memory_mb = memory.available / (1024 * 1024)
            
            # 系统负载（Unix系统）
            try:
                load_avg = psutil.getloadavg()[0]  # 1分钟平均负载
                cpu_count = psutil.cpu_count()
                normalized_load = load_avg / cpu_count if cpu_count else load_avg
            except AttributeError:
                # Windows系统没有getloadavg
                normalized_load = cpu_percent / 100.0
                
            return {
                'cpu_percent': cpu_percent,
                'memory_percent': memory_percent,
                'available_memory_mb': available_memory_mb,
                'normalized_load': normalized_load,
            }
            
        except Exception as e:
            logger.error(f"Error getting system status: {e}")
            # 返回保守的默认值
            return {
                'cpu_percent': 50,
                'memory_percent': 50,
                'available_memory_mb': 2048,
                'normalized_load': 0.5,
            }
            
    def update_factors(self, new_factors: Dict[str, float]) -> None:
        """
        更新调整因子
        
        Args:
            new_factors: 新的调整因子字典
        """
        self.factors.update(new_factors)
        logger.info(f"Updated threshold factors: {self.factors}") 