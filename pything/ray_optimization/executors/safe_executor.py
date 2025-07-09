"""
安全执行器
提供执行保护和自动降级机制
"""
import logging
from typing import Dict, Any, Optional, Tuple
from .ray_executor import IsolatedRayExecutor
from .normal_executor import NormalExecutor
from ..interfaces.executor import IExecutor

logger = logging.getLogger(__name__)

class SafeExecutor(IExecutor):
    """安全执行器，带有自动降级机制"""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化安全执行器
        
        Args:
            config: 配置字典
        """
        config = config or {}
        self.ray_executor = IsolatedRayExecutor(config.get('ray', {}))
        self.normal_executor = NormalExecutor(config.get('normal', {}))
        self.enable_fallback = config.get('enable_fallback', True)
        self.max_retry = config.get('max_retry', 1)
        
    def execute(self, code: str, globals_dict: Optional[Dict] = None) -> Tuple[Any, bool]:
        """
        安全执行代码，带有降级机制
        
        Args:
            code: 要执行的代码
            globals_dict: 全局变量字典
            
        Returns:
            (执行结果, 是否成功)
        """
        # 首先验证代码
        is_valid, error_msg = self.validate_code(code)
        if not is_valid:
            logger.error(f"Code validation failed: {error_msg}")
            return None, False
            
        # 执行代码
        logger.info("Executing code with SafeExecutor")
        return self._execute_with_fallback(code, globals_dict)
        
    def _execute_with_fallback(self, code: str, 
                               globals_dict: Optional[Dict] = None) -> Tuple[Any, bool]:
        """
        带降级的执行逻辑
        
        Args:
            code: 要执行的代码
            globals_dict: 全局变量字典
            
        Returns:
            (执行结果, 是否成功)
        """
        # 记录执行尝试
        attempts = []
        
        # 尝试使用Ray执行
        for retry in range(self.max_retry + 1):
            try:
                logger.info(f"Attempting Ray execution (attempt {retry + 1})")
                result, success = self.ray_executor.execute(code, globals_dict)
                
                if success:
                    logger.info("Ray execution successful")
                    return result, True
                else:
                    attempts.append(("ray", retry, "execution failed"))
                    
            except Exception as e:
                logger.warning(f"Ray execution error (attempt {retry + 1}): {e}")
                attempts.append(("ray", retry, str(e)))
                
        # Ray执行失败，降级到普通执行
        if self.enable_fallback:
            logger.info("Falling back to normal execution")
            try:
                result, success = self.normal_executor.execute(code, globals_dict)
                
                if success:
                    logger.info("Normal execution successful (after Ray failure)")
                    return result, True
                else:
                    attempts.append(("normal", 0, "execution failed"))
                    
            except Exception as e:
                logger.error(f"Normal execution also failed: {e}")
                attempts.append(("normal", 0, str(e)))
                
        # 所有尝试都失败
        logger.error(f"All execution attempts failed. Attempts: {attempts}")
        return None, False
        
    def execute_optimized(self, original_code: str, 
                         optimized_code: str,
                         globals_dict: Optional[Dict] = None) -> Tuple[Any, bool, str]:
        """
        执行优化后的代码，失败时降级到原始代码
        
        Args:
            original_code: 原始代码
            optimized_code: 优化后的代码
            globals_dict: 全局变量字典
            
        Returns:
            (执行结果, 是否成功, 执行模式)
        """
        # 尝试执行优化代码
        logger.info("Attempting to execute optimized code")
        result, success = self._execute_with_fallback(optimized_code, globals_dict)
        
        if success:
            return result, True, "optimized"
            
        # 优化代码执行失败，尝试原始代码
        if self.enable_fallback:
            logger.warning("Optimized code failed, falling back to original code")
            result, success = self.normal_executor.execute(original_code, globals_dict)
            
            if success:
                return result, True, "original"
                
        return None, False, "failed"
        
    def validate_code(self, code: str) -> Tuple[bool, Optional[str]]:
        """
        验证代码安全性
        
        Args:
            code: 要验证的代码
            
        Returns:
            (是否安全, 错误信息)
        """
        # 使用Ray执行器的验证逻辑（更严格）
        return self.ray_executor.validate_code(code)
        
    def cleanup(self) -> None:
        """清理资源"""
        try:
            self.ray_executor.cleanup()
        except Exception as e:
            logger.error(f"Error cleaning up Ray executor: {e}")
            
        try:
            self.normal_executor.cleanup()
        except Exception as e:
            logger.error(f"Error cleaning up normal executor: {e}") 