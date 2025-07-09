"""
智能优化器
整合分析、决策、优化和执行的主要入口
"""
import os
import json
import logging
from typing import Dict, Any, Optional, Tuple

from .analyzers.complexity_analyzer import ComplexityAnalyzer
from .decision.decision_engine import OptimizationDecisionEngine
from .optimizers.ray_optimizer import RayOptimizer
from .executors.safe_executor import SafeExecutor
from .executors.normal_executor import NormalExecutor

logger = logging.getLogger(__name__)

class SmartOptimizer:
    """智能优化器，自动决策并应用Ray优化"""
    
    def __init__(self, config_path: Optional[str] = None, test_mode: bool = False):
        """
        初始化智能优化器
        
        Args:
            config_path: 配置文件路径
            test_mode: 测试模式，跳过某些初始化
        """
        self.test_mode = test_mode
        self.config = self._load_config(config_path)
        
        # 初始化组件
        self.analyzer = ComplexityAnalyzer()
        self.decision_engine = OptimizationDecisionEngine(
            self.config.get('decision', {})
        )
        self.optimizer = RayOptimizer(self.config.get('optimizer', {}))
        self.safe_executor = SafeExecutor(self.config.get('executor', {}))
        self.normal_executor = NormalExecutor()
        
        # 执行上下文
        self.context = {}
        
    def run(self, script_path: str) -> None:
        """
        运行脚本的主入口
        
        Args:
            script_path: 脚本文件路径
        """
        try:
            # 读取代码
            with open(script_path, 'r', encoding='utf-8') as f:
                original_code = f.read()
                
            # 分析和执行
            self.analyze_and_execute(original_code)
            
        except Exception as e:
            logger.error(f"Error in smart optimizer: {e}")
            # 降级执行
            self._fallback_execution(script_path)
            
    def analyze_and_execute(self, original_code: str) -> Any:
        """
        分析代码并决定执行策略
        
        Args:
            original_code: 原始代码
            
        Returns:
            执行结果
        """
        # 1. 分析代码
        logger.info("Analyzing code...")
        analysis_result = self.analyzer.analyze(original_code)
        
        # 2. 决策是否优化
        should_optimize, score, threshold = self.decision_engine.should_optimize(
            analysis_result, self.context
        )
        
        print(f"=== Ray Optimization Analysis ===")
        print(f"Complexity Score: {score:.2f}")
        print(f"Dynamic Threshold: {threshold:.2f}")
        print(f"Decision: {'OPTIMIZE' if should_optimize else 'NORMAL EXECUTION'}")
        
        # 3. 执行
        if should_optimize:
            return self._execute_with_optimization(original_code, analysis_result)
        else:
            return self._execute_normal(original_code)
            
    def _execute_with_optimization(self, original_code: str, 
                                   analysis_result: Dict[str, Any]) -> Any:
        """
        使用优化执行代码
        
        Args:
            original_code: 原始代码
            analysis_result: 分析结果
            
        Returns:
            执行结果
        """
        try:
            print("\n=== Using Ray Optimization ===")
            
            # 优化代码
            optimized_code = self.optimizer.optimize(original_code, analysis_result)
            
            if optimized_code == original_code:
                print("No optimization applied, executing original code...")
                return self._execute_normal(original_code)
                
            # 执行优化后的代码
            result, success, mode = self.safe_executor.execute_optimized(
                original_code, optimized_code
            )
            
            if success:
                print(f"Execution successful (mode: {mode})")
                # 更新历史记录
                self._update_execution_history(True, mode)
            else:
                print("Execution failed")
                self._update_execution_history(False, mode)
                
            return result
            
        except Exception as e:
            logger.error(f"Error in optimized execution: {e}")
            print("\nFalling back to normal execution...")
            return self._execute_normal(original_code)
            
    def _execute_normal(self, code: str) -> Any:
        """
        普通执行代码
        
        Args:
            code: 代码
            
        Returns:
            执行结果
        """
        print("\n=== Normal Execution ===")
        result, success = self.normal_executor.execute(code)
        
        if success:
            print("Execution successful")
        else:
            print("Execution failed")
            
        return result
        
    def _fallback_execution(self, script_path: str) -> None:
        """
        降级执行脚本
        
        Args:
            script_path: 脚本路径
        """
        print("\n=== Fallback Execution ===")
        return_code = self.normal_executor.execute_file(script_path)
        
        if return_code == 0:
            print("Fallback execution successful")
        else:
            print(f"Fallback execution failed with code: {return_code}")
            
    def _load_config(self, config_path: Optional[str]) -> Dict[str, Any]:
        """
        加载配置
        
        Args:
            config_path: 配置文件路径
            
        Returns:
            配置字典
        """
        # 默认配置
        default_config = {
            'decision': {
                'enabled': True,
                'base_threshold': 50,
                'min_threshold': 30,
                'max_threshold': 90,
            },
            'optimizer': {
                'optimization_level': 1,
            },
            'executor': {
                'enable_fallback': True,
                'max_retry': 1,
                'ray': {
                    'max_workers': 4,
                    'memory_per_worker': 2048,
                    'total_memory_limit': 8192,
                    'execution_timeout': 300,
                },
                'normal': {
                    'timeout': 300,
                }
            }
        }
        
        # 尝试加载用户配置
        if config_path and os.path.exists(config_path):
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                    # 合并配置
                    self._merge_config(default_config, user_config)
            except Exception as e:
                logger.warning(f"Failed to load config from {config_path}: {e}")
                
        return default_config
        
    def _merge_config(self, base: Dict, update: Dict) -> None:
        """
        递归合并配置
        
        Args:
            base: 基础配置
            update: 更新配置
        """
        for key, value in update.items():
            if key in base and isinstance(base[key], dict) and isinstance(value, dict):
                self._merge_config(base[key], value)
            else:
                base[key] = value
                
    def _update_execution_history(self, success: bool, mode: str) -> None:
        """
        更新执行历史
        
        Args:
            success: 是否成功
            mode: 执行模式
        """
        # TODO: 实现历史记录持久化
        # 这里可以记录到文件或数据库
        if 'history' not in self.context:
            self.context['history'] = []
            
        self.context['history'].append({
            'success': success,
            'mode': mode,
        })
        
        # 计算历史统计
        history = self.context['history'][-10:]  # 最近10次
        if len(history) > 0:
            success_count = sum(1 for h in history if h['success'])
            self.context['has_history'] = True
            self.context['avg_speedup'] = 1.5  # TODO: 实际计算
            self.context['failure_rate'] = 1 - (success_count / len(history)) 