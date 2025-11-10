import os
from typing import Any, List, Dict, Optional, Union
from pathlib import Path
from dataclasses import dataclass
import json

from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.schemas.command import CmdContext
from dataProcessing.model.methlib.utils.file import FileUtils # 用于处理嵌套的 JSON 配置


class FileListHandler(ParameterHandler):
    """
    处理 FileList 类型的参数，用于处理多个已存在于文件系统或云存储中的输入文件。
    对标 Java FileListHandler。
    """

    def __init__(self):
        pass

    def supports(self, parameter_type: Any) -> bool:
        """支持参数类型为 dict 且包含 'FileList' 键的情况。"""
        # 对标 Java: return parameter_type instanceof Map && ((Map<?, ?>) parameter_type).containsKey("FileList");
        return isinstance(parameter_type, dict) and "FileList" in parameter_type

    def parse(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext, is_external_call: bool):
        """
        调度内部或外部解析逻辑。
        """
        # Java: if (is_external_call) parseExternal(parameter_type, raw_value, context);
        if is_external_call:
            self._parse_external(parameter_type, raw_value, context)
        # Java: else parseInternal(parameter_type, raw_value, context);
        else:
            self._parse_internal(parameter_type, raw_value, context)

    def _parse_internal(self, parameter_type: Any, raw_value: Any, context: CmdContext):
        """
        内部调用逻辑 (is_external_call=false)。
        (此实现中保持与 ExistingFileHandler 一致，省略内部逻辑)
        """
        pass

    def _parse_external(self, parameter_type: Any, raw_value: Any, context: CmdContext):
        """
        外部调用逻辑 (is_external_call=true)：需要从外部URL下载文件列表到工作目录。
        严格对标 Java parseExternal 方法。
        """
        
        # 1. 类型转换和初始化
        # Java: List<String> inputList = (List<String>) raw_value;
        if not isinstance(raw_value, list):
             # FileList 期望一个列表，如果不是列表，则尝试包装，或者如果是 None 则跳过
             if raw_value is None: return
             input_list = [str(raw_value)]
        else:
             input_list = raw_value
             
        parameter_typeMap: Dict[str, Any] = parameter_type
        cmdDto = context.cmd_dto
        cmdBuilder = context.cmd_builder

        list_size = len(input_list)
        
        for i, input_value in enumerate(input_list):
            
            # 2. 下载文件
            # Java: String value = Objects.toString(input);
            value = str(input_value) if input_value is not None else ""
            if not value: 
                continue

            # download_url = f"{self.data_server}/{value}"
            download_url = value
            
            try:
                # Java: fileInfoService.downloadFile(data_server + '/' + value, context.getWdFolder().getAbsolutePath(), false);
                downloaded_path: Path = FileUtils.download_file(
                    file_url=download_url,
                    save_dir=context.wd_folder, # context.wd_folder 是 Path
                    is_uuid=False 
                )
            except IOError as e:
                # 捕获下载错误并重新抛出
                raise IOError(f"Failed to download external file from {download_url}: {e}") from e

            # 3. 更新 DTO
            # Java: cmdDto.inputSrcFiles.add(file); cmdDto.inputFileIds.add(value);
            cmdDto.input_src_files.append(downloaded_path)
            cmdDto.input_file_ids.append(value)

            # 4. 文件类型处理 (判断是否为 Multipart/SHP/SGRD)
            file_type_obj = parameter_typeMap.get("FileList")
            
            is_multipart = False
            
            # Java: if (fileTypeObj instanceof Map) { ... }
            if isinstance(file_type_obj, dict):
                fileTypeMap: Dict[str, Any] = file_type_obj
                # Java: if (fileTypeMap.containsKey("Vector") || fileTypeMap.containsKey("RasterAndVector"))
                if "Vector" in fileTypeMap or "RasterAndVector" in fileTypeMap:
                    is_multipart = True
            
            # Java: else if (fileTypeObj instanceof String && fileTypeObj.equals("Grid"))
            elif isinstance(file_type_obj, str) and file_type_obj == "Grid":
                is_multipart = True
                
            # 5. 最终命令构建
            # Java: handleMultipartFile(file, cmdDto, context, i, inputList.size());
            if is_multipart:
                # 针对 SHP/SGRD/ZIP 等类型，调用 handleMultipartFile 来处理解压和路径查找
                self.handle_multipart_file(
                    file_path=downloaded_path.as_posix(),
                    context=context,
                    index=i, 
                    size=list_size
                )
            else:
                # 普通文件: 传给命令行的路径是文件名 (因为文件已在 WD 根目录)
                # Java: handleInput(..., Paths.get(..., file.getName()).toString());
                final_path_for_cmd = downloaded_path.name
                self.handle_input(cmdBuilder, i, list_size, final_path_for_cmd)