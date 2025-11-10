import os
import uuid
import re
from typing import Any, List, Dict, Optional, Union
from pathlib import Path
from dataclasses import dataclass

from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.schemas.command import CmdContext
from dataProcessing.model.methlib.utils.file import FileUtils

# ----------------------------------------------------
# 现有文件处理器 (ExistingFileHandler)
# ----------------------------------------------------

class ExistingFileHandler(ParameterHandler):
    """
    处理 ExistingFile 类型的参数，用于处理已存在于文件系统或云存储中的输入文件。
    对标 Java ExistingFileHandler。
    """

    def __init__(self):
        pass

    def supports(self, parameter_type: Any) -> bool:
        """支持参数类型为 dict 且包含 'ExistingFile' 键的情况。"""
        # 对标 Java: return parameter_type instanceof Map && ((Map<?, ?>) parameter_type).containsKey("ExistingFile");
        return isinstance(parameter_type, dict) and "ExistingFile" in parameter_type

    def parse(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext, is_external_call: bool):
        """
        调度内部或外部解析逻辑。
        对标 Java parse(Object, Object, Integer, CmdContextDto, Boolean)。
        """
        if is_external_call:
            # 外部调用 (从 URL 下载到 WD)
            self._parse_external(parameter_type, raw_value, context)
        else:
            pass
            # self._parse_internal(parameter_type, raw_value, context)

    def _parse_internal(self, parameter_type: Any, raw_value: Any, context: CmdContext):
        """
        内部调用逻辑 (is_external_call=false)。
        (根据用户要求，此方法不实现逻辑，留空占位)
        """
        pass # Java parseInternal 逻辑已省略


    def _parse_external(self, parameter_type: Any, raw_value: Any, context: CmdContext):
        """
        外部调用逻辑 (is_external_call=true)：需要从外部URL下载文件到工作目录。
        严格对标 Java parseExternal 方法。
        """
        # 1. 处理原始值：兼容单元素数组
        value: str
        if isinstance(raw_value, list):
            # 兼容 Java 逻辑: 如果是空数组则取空字符串，否则取第一个元素
            value = str(raw_value[0]) if raw_value else ""
        else:
            value = str(raw_value)
        
        if not value: return

        # 2. 下载文件到工作目录
        # download_url = f"{self.data_server}/{value}"
        download_url = value
        
        try:
            # 使用 FileUtils.download_file (Java fileInfoService.downloadFile 的 Python 等价实现)
            downloaded_path: Path = FileUtils.download_file(
                file_url=download_url,
                save_dir=context.wd_folder, # context.wd_folder 是 Path
                is_uuid=False 
            )
        except IOError as e:
            # 捕获下载错误并重新抛出
            raise IOError(f"Failed to download external file from {download_url}: {e}") from e

        # 3. 更新 DTO
        context.cmd_dto.input_src_files.append(downloaded_path) # inputSrcFiles.add(file)
        context.cmd_dto.input_file_ids.append(value)             # inputFileIds.add(value)

        # 4. 文件类型处理 (判断是否为 Multipart/SHP/SGRD)
        parameter_typeMap = parameter_type
        file_type_obj = parameter_typeMap.get("ExistingFile") #????
        
        is_multipart = False
        
        # 检查是否为 Vector 或 RasterAndVector (Map)
        if isinstance(file_type_obj, dict):
            if "Vector" in file_type_obj or "RasterAndVector" in file_type_obj:
                is_multipart = True
        
        # 检查是否为 Grid (String)
        elif isinstance(file_type_obj, str) and file_type_obj == "Grid":
            is_multipart = True
            
        # 5. 最终命令构建 (路径是相对于工作目录的)
        if is_multipart:
            # 针对 SHP/SGRD/ZIP 等类型，调用 handleMultipartFile 来处理解压和路径查找
            # Java: handleMultipartFile(file, cmdDto, context, 0, 1);
            self.handle_multipart_file(
                file_path=downloaded_path.as_posix(),
                context=context,
                index=0, 
                size=1
            )
        else:
            # 普通文件: 传给命令行的路径是文件名 (因为文件已在 WD 根目录)
            # Java: handleInput(..., Paths.get(..., file.getName()).toString());
            final_path_for_cmd = downloaded_path.name 
            self.handle_input(context.cmd_builder, 0, 1, final_path_for_cmd)