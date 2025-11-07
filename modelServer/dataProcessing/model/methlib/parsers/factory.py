from typing import List, Any
from .base import ParameterHandler


class ParameterHandlerFactory:
    """工厂，用于获取支持指定类型的处理器"""

    def __init__(self, handlers: List[ParameterHandler]):
        self.handlers = handlers

    def get_handler(self, parameter_type: Any) -> ParameterHandler:
        for handler in self.handlers:
            if handler.supports(parameter_type):
                return handler
        raise ValueError(f"No handler found for type: {parameter_type}")
