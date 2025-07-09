"""
普通执行器
不使用Ray优化的普通代码执行器
"""
import logging
import subprocess
import sys
from typing import Dict, Any, Optional, Tuple
from ..interfaces.executor import IExecutor

logger = logging.getLogger(__name__)

class NormalExecutor(IExecutor):
    """普通执行器，直接执行原始代码"""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化普通执行器
        
        Args:
            config: 配置字典
        """
        config = config or {}
        self.timeout = config.get('timeout', 300)  # 秒
        
    def execute(self, code: str, globals_dict: Optional[Dict] = None) -> Tuple[Any, bool]:
        """
        执行代码
        
        Args:
            code: 要执行的代码
            globals_dict: 全局变量字典
            
        Returns:
            (执行结果, 是否成功)
        """
        globals_dict = globals_dict or {}
        
        try:
            # 创建执行环境
            exec_globals = self._create_safe_globals(globals_dict)
            
            # 执行代码
            exec(code, exec_globals)
            
            # 获取结果
            result = exec_globals.get('result', None)
            
            return result, True
            
        except Exception as e:
            logger.error(f"Error during normal execution: {e}")
            import traceback
            traceback.print_exc()
            return None, False
            
    def execute_file(self, script_path: str) -> int:
        """
        执行Python脚本文件
        
        Args:
            script_path: 脚本文件路径
            
        Returns:
            进程返回码
        """
        try:
            # 使用subprocess执行脚本
            result = subprocess.run(
                [sys.executable, script_path],
                capture_output=True,
                text=True,
                timeout=self.timeout
            )
            
            # 输出结果
            if result.stdout:
                print(result.stdout)
            if result.stderr:
                print(result.stderr, file=sys.stderr)
                
            return result.returncode
            
        except subprocess.TimeoutExpired:
            logger.error(f"Script execution timeout: {script_path}")
            return -1
        except Exception as e:
            logger.error(f"Error executing script {script_path}: {e}")
            return -1
            
    def validate_code(self, code: str) -> Tuple[bool, Optional[str]]:
        """
        验证代码安全性（基础验证）
        
        Args:
            code: 要验证的代码
            
        Returns:
            (是否安全, 错误信息)
        """
        # 编译检查
        try:
            compile(code, '<string>', 'exec')
            return True, None
        except SyntaxError as e:
            return False, f"Syntax error: {e}"
        except Exception as e:
            return False, f"Compilation error: {e}"
            
    def cleanup(self) -> None:
        """清理资源（普通执行器无需清理）"""
        pass
        
    def _create_safe_globals(self, user_globals: Dict[str, Any]) -> Dict[str, Any]:
        """
        创建安全的全局命名空间
        
        Args:
            user_globals: 用户提供的全局变量
            
        Returns:
            全局变量字典
        """
        # 导入常用模块
        import numpy as np
        import pandas as pd
        
        # 基础全局变量
        base_globals = {
            '__name__': '__main__',
            '__doc__': None,
            # 添加常用模块
            'np': np,
            'numpy': np,
            'pd': pd,
            'pandas': pd,
        }
        
        # 合并用户变量
        base_globals.update(user_globals)
        
        return base_globals 