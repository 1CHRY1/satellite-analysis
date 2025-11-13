import os
from urllib.parse import urlparse
import uuid
from typing import Any, Dict, Optional, Union
from pathlib import Path

from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.schemas.command import CmdContext
from dataProcessing.model.methlib.utils.file import FileUtils

class NewFileHandler(ParameterHandler):
    """
    处理 NewFile 类型的参数，用于确定和构建命令行的输出文件路径/名称。
    对标 Java NewFileHandler。
    """

    def supports(self, parameter_type: Any) -> bool:
        """支持参数类型为 dict 且包含 'NewFile' 键的情况。"""
        # 对标 Java: return parameter_type instanceof Map && ((Map<?, ?>) parameter_type).containsKey("NewFile");
        return isinstance(parameter_type, dict) and "NewFile" in parameter_type

    def parse(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext, is_external_call: bool):
        """
        调度内部或外部解析逻辑。对于 NewFile，内部和外部逻辑通常不同。
        """
        # Java: if (is_external_call) parseExternal(...); else parseInternal(...);
        if is_external_call:
            self._parse_external(parameter_type, raw_value, val_index, context)
        else:
            self._parse_internal(parameter_type, raw_value, context)

    def _parse_internal(self, parameter_type: Any, raw_value: Any, context: CmdContext):
        """
        内部调用逻辑 (is_external_call=false)。
        (对标 Java NewFileHandler 中的 parseInternal 方法，此处省略具体逻辑，通常涉及 filePid/fileGroup 处理)
        """
        pass

    def _parse_external(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext):
        """
        外部调用逻辑 (is_external_call=true)：构建输出文件名并添加到命令行。
        """
        
        # 1. 初始化和类型转换
        parameter_typeMap: Dict[str, Any] = parameter_type
        cmdDto = context.cmd_dto
        cmdBuilder = context.cmd_builder
        
        # 2. 确定输出文件的扩展名 (Extension)
        file_type_obj = parameter_typeMap.get("NewFile")
        extension = ""
        
        # Java: if (fileTypeObj instanceof Map) { ... }
        if isinstance(file_type_obj, dict):
            # Vector/RasterAndVector 可能会以 Map 形式出现，通常在 Map 形式下，扩展名由子处理器决定，或默认为空
            # Java: cmdDto.extensionList.add("");
            cmdDto.extension_list.append("")
        
        # Java: else if (fileTypeObj instanceof String) { ... }
        elif isinstance(file_type_obj, str):
            if file_type_obj == "Raster":
                extension = "tiff"
            elif file_type_obj == "Lidar":
                extension = "las"
            elif file_type_obj == "Vector":
                extension = "" # Vector 扩展名可能根据格式变化，此处留空
            else:
                # 普通文件如 html/csv/txt
                extension = file_type_obj.lower()
                # Java: cmdDto.extensionList.add(Objects.toString(fileTypeObj).toLowerCase());
                cmdDto.extension_list.append(extension)

        # 3. 确定最终输出文件名
        cmdDto.has_output = True

        if raw_value is not None:
            # 解析 URL 并取出文件名
            path = urlparse(raw_value).path
            raw_value = os.path.basename(path.rstrip('/'))
        raw_value_str = str(raw_value) if raw_value is not None else None
        
        # 获取不带后缀的名称 (可能来自 raw_value 或生成 UUID)
        # Java: String outputFileRealNameWithoutSuffix = (raw_value != null) ? (FileUtils.getFileNameWithoutSuffix((String) raw_value)) : null;
        output_file_name_base = None
        if raw_value_str:
            # 假设 FileUtils.get_file_name_without_suffix 能够处理 None 或空字符串
            output_file_name_base = FileUtils.get_file_name_without_suffix(raw_value_str)

        # Java: if (outputFileRealNameWithoutSuffix == null) { outputFileRealNameWithoutSuffix = UUID.randomUUID().toString(); }
        if not output_file_name_base:
            output_file_name_base = uuid.uuid4().hex

        # 4. 处理原始值中的后缀 (如果之前确定的 extension 为空)
        # Java: if (extension.equals("") && !FileUtils.getFileSuffix((String) raw_value).isEmpty()) { extension = FileUtils.getFileSuffix((String) raw_value); }
        if not extension and raw_value_str:
            raw_suffix = FileUtils.get_file_suffix(raw_value_str)
            if raw_suffix:
                extension = raw_suffix

        # 5. 组合最终文件名
        output_file_real_name = f"{output_file_name_base}.{extension}" if extension else output_file_name_base

        # 6. 更新 DTO 和命令行
        # Java: cmdDto.outputFileNameList.put("val" + val_index, raw_value);
        cmdDto.output_file_name_list[f"val{val_index}"] = raw_value
        
        # Java: cmdBuilder.append(outputFileRealName).append(" ");
        cmdBuilder.append(output_file_real_name)
        cmdBuilder.append(" ")