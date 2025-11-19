from typing import Any, Optional
from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.schemas.command import CmdContext


class VectorAttributeFieldHandler(ParameterHandler):
    """
    处理参数类型定义中包含 'VectorAttributeField' 键的参数。
    该处理器主要用于支持特定的类型标识符，并直接将原始值传入命令构建器。
    """

    def supports(self, parameter_type: Any) -> bool:
        """
        支持参数类型是一个字典 (Map) 且包含 "VectorAttributeField" 键的情况。
        """
        # 对应 Java: return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("VectorAttributeField");
        return isinstance(parameter_type, dict) and "VectorAttributeField" in parameter_type

    def parse(self,
              parameter_type: Any,
              raw_value: Optional[Any],
              val_index: int,
              context: CmdContext,
              is_external_call: bool):
        # 对应 Java: handleNull(rawValue, context.getCmdBuilder(), context.getParamSpecs());
        # 这里的 parse 方法非常简单，因为它不需要对 raw_value 进行任何自定义校验或转换。
        self.handle_null(raw_value, context.cmd_builder, context.param_specs)