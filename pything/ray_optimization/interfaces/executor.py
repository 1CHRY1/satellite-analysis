"""
执行器接口定义
定义代码执行的抽象接口
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional, Tuple

class IExecutor(ABC):
    """执行器接口"""
    
    @abstractmethod
    def execute(self, code: str, globals_dict: Optional[Dict] = None) -> Tuple[Any, bool]:
        """
        执行代码
        
        Args:
            code: 要执行的代码
            globals_dict: 全局变量字典
            
        Returns:
            (执行结果, 是否成功)
        """
        pass
    
    @abstractmethod
    def validate_code(self, code: str) -> Tuple[bool, Optional[str]]:
        """
        验证代码安全性
        
        Args:
            code: 要验证的代码
            
        Returns:
            (是否安全, 错误信息)
        """
        pass
    
    @abstractmethod
    def cleanup(self) -> None:
        """清理资源"""
        pass

class IResourceMonitor(ABC):
    """资源监控器接口"""
    
    @abstractmethod
    def check_resources(self) -> Tuple[bool, str]:
        """
        检查系统资源
        
        Returns:
            (资源是否充足, 描述信息)
        """
        pass
    
    @abstractmethod
    def monitor_execution(self, func: callable) -> Any:
        """
        监控函数执行
        
        Args:
            func: 要监控的函数
            
        Returns:
            函数执行结果
        """
        pass 