import re
from typing import Any, Dict

from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.parsers.handler.existing_file import ExistingFileHandler
from dataProcessing.model.methlib.parsers.handler.primitive_type import PrimitiveTypeHandler
from dataProcessing.model.methlib.schemas.command import CmdContext

class ExistingFileOrFloatHandler(ParameterHandler):
    """
    处理 ExistingFileOrFloat 类型参数。
    如果值是有效的浮点数，则委托给 PrimitiveTypeHandler (Float)。
    否则，将参数类型修改为 ExistingFile，委托给 ExistingFileHandler。
    对标 Java ExistingFileOrFloatHandler。
    """

    def __init__(self, existing_file_handler: ExistingFileHandler, primitive_type_handler: PrimitiveTypeHandler):
        """注入依赖的处理器 (对标 @Autowired)"""
        self.existing_file_handler = existing_file_handler
        self.primitive_type_handler = primitive_type_handler

    def supports(self, parameter_type: Any) -> bool:
        """支持参数类型为 dict 且包含 'ExistingFileOrFloat' 键的情况。"""
        return isinstance(parameter_type, dict) and "ExistingFileOrFloat" in parameter_type

    def parse(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext, is_external_call: bool):
        # 1. 严格复刻 Java 逻辑：转换为字符串并检查是否为数字
        # Java: String value = Objects.toString(raw_value);
        value = str(raw_value) if raw_value is not None else ""
        
        # Java 正则: value.matches("-?\\d+(\\.\\d+)?") -> 匹配整数或小数，带可选的负号
        # 使用 re.fullmatch 确保整个字符串匹配
        is_numeric = re.fullmatch(r"-?\d+(\.\d+)?", value) is not None

        if is_numeric:
            # 2. 如果是数字，作为 Float 交给 PrimitiveTypeHandler 处理
            # Java: primitiveTypeHandler.parse("Float", raw_value, ...);
            self.primitive_type_handler.parse(
                parameter_type="Float", 
                raw_value=raw_value, 
                val_index=val_index, 
                context=context, 
                is_external_call=is_external_call
            )
        else:
            # 3. 如果不是数字，假定为文件路径，转换为 ExistingFile 类型
            if isinstance(parameter_type, dict):
                # Java 逻辑通过引用传递修改 Map，我们直接修改传入的 parameter_type 引用。
                type_map: Dict[str, Any] = parameter_type
                
                # 提取 ExistingFileOrFloat 的值（可能是None，也可能是嵌套的Map/String）
                existing_file_config = type_map.get("ExistingFileOrFloat")
                
                # 将 ExistingFileOrFloat 替换为 ExistingFile
                # Java: typeMap.put("ExistingFile", typeMap.get("ExistingFileOrFloat"));
                type_map["ExistingFile"] = existing_file_config
                
                # 移除 ExistingFileOrFloat
                # Java: typeMap.remove("ExistingFileOrFloat");
                del type_map["ExistingFileOrFloat"]
            
            # 4. 调用 ExistingFileHandler 处理
            # Java: existingFileHandler.parse(parameter_type, raw_value, ...);
            self.existing_file_handler.parse(
                parameter_type=parameter_type, 
                raw_value=raw_value, 
                val_index=val_index, 
                context=context, 
                is_external_call=is_external_call
            )