from typing import Any, Dict, List, Optional
from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.schemas.command import CmdContext


class OptionListHandler(ParameterHandler):
    """
    处理参数类型定义中包含 'OptionList' 键的参数。
    它确保传入的值 (rawValue) 必须是选项列表中的一个有效值。
    """

    def supports(self, parameter_type: Any) -> bool:
        """
        支持参数类型是一个字典 (Map) 且包含 "OptionList" 键的情况。
        """
        # 对应 Java: return parameterType instanceof Map && ((Map<?, ?>) parameterType).containsKey("OptionList");
        return isinstance(parameter_type, dict) and "OptionList" in parameter_type

    def parse(self,
              parameter_type: Any,
              raw_value: Optional[Any],
              val_index: int,
              context: CmdContext,
              is_external_call: bool):
        
        # 对应 Java: Map<?, ?> parameterTypeMap = (Map<?, ?>) parameterType;
        parameter_type_map: Dict[str, Any] = parameter_type
        
        # 对应 Java: List<?> options = (List<?>) parameterTypeMap.get("OptionList");
        options: List[Any] = parameter_type_map.get("OptionList", [])
        
        cmd_builder = context.cmd_builder

        # 1. 值校验
        # 对应 Java: if (rawValue != null) { ... }
        if raw_value is not None:
            raw_value_str = str(raw_value)
            
            # 对应 Java: if (!options.contains(rawValue.toString())) { throw new IllegalArgumentException(...); }
            if raw_value_str not in [str(opt) for opt in options]:
                # 抛出异常，阻止无效值被使用
                raise ValueError(
                    f"Value '{raw_value_str}' is not in allowed options: {options}"
                )

        # 2. 类型处理和调用 handleNull
        # 对应 Java: if (parameterTypeMap.get("OptionList") instanceof List) { ... }
        option_list_definition = parameter_type_map.get("OptionList")
        if isinstance(option_list_definition, List):
            # 虽然 Python 不像 Java 需要显式向下转型，但这里确保它是 List 类型
            # 对应 Java: List<String> optionList = (List<String>) parameterTypeMap.get("OptionList");
            
            # 对应 Java: handleNull(rawValue, cmdBuilder, context.getParamSpecs());
            self.handle_null(raw_value, cmd_builder, context.param_specs)
        else:
            # 对应 Java: // TODO OptionList里面不是option?
            # 原始 Java 代码的 TODO 部分，这里可以根据需要添加日志或更详细的异常处理
            print(f"Warning: 'OptionList' value is not a list in parameter type: {parameter_type_map}")
