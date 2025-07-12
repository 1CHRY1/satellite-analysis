"""
Ray执行器
安全地初始化和使用Ray进行代码执行
"""
import os
import logging
import time
from typing import Dict, Any, Optional, Tuple
from ..interfaces.executor import IExecutor

logger = logging.getLogger(__name__)

# Ray导入保护
try:
    import ray
    RAY_AVAILABLE = True
except ImportError:
    RAY_AVAILABLE = False
    logger.warning("Ray is not available")

class IsolatedRayExecutor(IExecutor):
    """隔离的Ray执行器，确保Ray问题不影响系统"""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化Ray执行器
        
        Args:
            config: 配置字典
        """
        config = config or {}
        self.ray_initialized = False
        self.max_workers = config.get('max_workers', 4)
        self.memory_per_worker = config.get('memory_per_worker', 2048)  # MB
        self.total_memory_limit = config.get('total_memory_limit', 8192)  # MB
        self.execution_timeout = config.get('execution_timeout', 300)  # 秒
        
    def init_ray_safely(self) -> bool:
        """安全初始化Ray"""
        if not RAY_AVAILABLE:
            logger.error("Ray is not installed")
            return False
            
        try:
            # 检查Ray是否已经初始化
            if ray.is_initialized():
                logger.info("Ray is already initialized, shutting down first")
                ray.shutdown()
                time.sleep(1)  # 等待关闭完成
                
            # 计算资源限制
            num_cpus = min(self.max_workers, os.cpu_count() or 4)
            object_store_memory = min(
                self.total_memory_limit * 1024 * 1024,  # 转换为字节
                2 * 1024**3  # 最大2GB
            )
            
            logger.info(f"Initializing Ray with {num_cpus} CPUs and "
                       f"{object_store_memory / 1024**3:.1f}GB object store")
            
            # 初始化Ray
            ray.init(
                num_cpus=num_cpus,
                object_store_memory=object_store_memory,
                _system_config={
                    "automatic_object_spilling_enabled": True,
                    "object_spilling_config": {
                        "type": "filesystem",
                        "params": {"directory_path": "/tmp/ray_spill"}
                    },
                    "max_direct_call_object_size": 100 * 1024**2,  # 100MB
                },
                logging_level=logging.WARNING,
                include_dashboard=False,  # 不启动dashboard
                _temp_dir="/tmp/ray_temp"  # 指定临时目录
            )
            
            self.ray_initialized = True
            logger.info("Ray initialized successfully")
            return True
            
        except Exception as e:
            logger.error(f"Ray initialization failed: {e}")
            self.ray_initialized = False
            return False
            
    def shutdown_ray_safely(self) -> None:
        """安全关闭Ray"""
        try:
            if self.ray_initialized and RAY_AVAILABLE and ray.is_initialized():
                ray.shutdown()
                self.ray_initialized = False
                logger.info("Ray shutdown successfully")
        except Exception as e:
            logger.error(f"Error during Ray shutdown: {e}")
            
    def execute(self, code: str, globals_dict: Optional[Dict] = None) -> Tuple[Any, bool]:
        """
        使用Ray执行优化后的代码
        
        Args:
            code: 要执行的代码
            globals_dict: 全局变量字典
            
        Returns:
            (执行结果, 是否成功)
        """
        if not self.ray_initialized:
            if not self.init_ray_safely():
                logger.error("Failed to initialize Ray")
                return None, False
                
        globals_dict = globals_dict or {}
        
        try:
            # 创建执行环境
            exec_globals = self._create_safe_globals(globals_dict)
            
            # 设置超时
            import signal
            
            def timeout_handler(signum, frame):
                raise TimeoutError("Execution timeout")
                
            # 只在Unix系统上使用signal
            if hasattr(signal, 'SIGALRM'):
                signal.signal(signal.SIGALRM, timeout_handler)
                signal.alarm(self.execution_timeout)
                
            try:
                # 执行代码
                exec(code, exec_globals)
                result = exec_globals.get('result', None)
                
                # 取消超时
                if hasattr(signal, 'SIGALRM'):
                    signal.alarm(0)
                    
                return result, True
                
            except TimeoutError:
                logger.error("Code execution timeout")
                return None, False
                
        except Exception as e:
            logger.error(f"Error during Ray execution: {e}")
            return None, False
            
    def validate_code(self, code: str) -> Tuple[bool, Optional[str]]:
        """
        验证代码安全性
        
        Args:
            code: 要验证的代码
            
        Returns:
            (是否安全, 错误信息)
        """
        # 危险的函数和模块
        dangerous_patterns = [
            'exec', 'eval', '__import__', 'compile',
            'globals', 'locals', 'vars', 
            'os.system', 'subprocess', 'commands',
            'open(', 'file(', 'input(', 'raw_input',
        ]
        
        for pattern in dangerous_patterns:
            if pattern in code:
                return False, f"Dangerous pattern detected: {pattern}"
                
        # 检查import语句
        import ast
        try:
            tree = ast.parse(code)
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    for alias in node.names:
                        if alias.name in ['os', 'sys', 'subprocess', 'socket']:
                            return False, f"Dangerous import: {alias.name}"
                            
        except SyntaxError as e:
            return False, f"Syntax error: {e}"
            
        return True, None
        
    def cleanup(self) -> None:
        """清理资源"""
        self.shutdown_ray_safely()
        
    def _create_safe_globals(self, user_globals: Dict[str, Any]) -> Dict[str, Any]:
        """
        创建安全的全局命名空间
        
        Args:
            user_globals: 用户提供的全局变量
            
        Returns:
            安全的全局变量字典
        """
        # 安全的内置函数
        safe_builtins = {
            'print': print,
            'len': len,
            'range': range,
            'enumerate': enumerate,
            'zip': zip,
            'map': map,
            'filter': filter,
            'sum': sum,
            'min': min,
            'max': max,
            'abs': abs,
            'round': round,
            'sorted': sorted,
            'list': list,
            'dict': dict,
            'set': set,
            'tuple': tuple,
            'str': str,
            'int': int,
            'float': float,
            'bool': bool,
            'True': True,
            'False': False,
            'None': None,
            # 添加Ray
            'ray': ray if RAY_AVAILABLE else None,
        }
        
        # 合并用户提供的全局变量
        safe_globals = {
            '__builtins__': safe_builtins,
            '__name__': '__main__',
            '__doc__': None,
        }
        
        # 只添加安全的用户变量
        for key, value in user_globals.items():
            if not key.startswith('__'):
                safe_globals[key] = value
                
        return safe_globals 