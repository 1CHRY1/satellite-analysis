import os
import uuid
from typing import Any, List, Dict, Optional
from pathlib import Path
from pathlib import Path

from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.schemas.command import CmdContext
from dataProcessing.model.methlib.utils.file import FileUtils


# -----------------------------------------------------------
# --- Mocks for Services and Utils (DirectoryHandler 依赖项) ---
# -----------------------------------------------------------

class MockFileInfoService:
    """
    模拟 IFileInfoService，用于获取用户目录下的文件列表。
    在真实系统中，这会调用数据库或文件存储服务。
    """
    def get_files_by_pid(self, user_id: int, file_pid: str) -> List[Path]:
        """
        根据父目录ID (filePid) 获取文件列表。
        返回模拟的文件路径列表。
        """
        # 模拟文件存在于用户目录下
        print(f"[Mock] 正在为用户 {user_id} 从父目录 {file_pid} 获取文件...")
        
        # 假设获取到两个文件，注意：这里的路径是模拟的源路径
        mock_data_dir = Path(f"/data/user_{user_id}/{file_pid}")
        return [
            mock_data_dir / "dir_file_a.txt",
            mock_data_dir / "dir_file_b.tif",
        ]


# -----------------------------------------------------------
# --- DirectoryHandler Implementation ---
# -----------------------------------------------------------

class DirectoryHandler(ParameterHandler):
    """
    目录参数处理器：处理 Directory 类型的参数。
    严格对标 Java DirectoryHandler 逻辑。
    """
    def __init__(self):
        # 模拟 @Autowired 注入
        self.file_info_service = MockFileInfoService()

    def supports(self, parameter_type: Any) -> bool:
        """检查是否支持 Directory 类型。"""
        if isinstance(parameter_type, str):
            return parameter_type == "Directory"
        return False

    def parse(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext, is_external_call: bool):
        """解析目录参数并构建命令行片段。"""
        cmd_builder = context.cmd_builder
        cmd_dto = context.cmd_dto
        param_type_spec = context.param_specs.get("Type")

        # 检查参数是输入目录还是输出目录
        if param_type_spec == "DataInput":
            # -------------------- DataInput 逻辑 --------------------
            
            # 1. 获取输入目录的 filePid
            file_parent: Dict[str, Any] | None = raw_value if isinstance(raw_value, dict) else None
            if not file_parent or 'filePid' not in file_parent:
                print("[WARN] DirectoryHandler: DataInput rawValue is missing or invalid dictionary or missing filePid.")
                return # 无法处理则跳过或抛出异常
            
            file_pid = str(file_parent.get("filePid"))
            
            # 2. 获取源文件并复制到工作目录 (wdFolder)
            src_files = self.file_info_service.get_files_by_pid(context.user_id, file_pid)
            
            # 模拟 Java FileUtils.copyFilesToDirectory
            copied_files = FileUtils.copy_files_to_directory(src_files, str(context.wd_folder.absolute()))
            
            # 3. 更新 CmdDto
            cmd_dto.input_src_files.extend(copied_files)
            
            # Java 逻辑: cmdDto.inputFileIds.add(filePid);
            cmd_dto.input_file_ids.append(file_pid) 
            
            # 存储第一个输入目录的 PID，作为默认输出路径的参考
            if not cmd_dto.input_file_pid:
                cmd_dto.input_file_pid = file_pid
            
            # 4. 构建命令行 (传递工作目录的绝对路径，并加引号)
            # Java 逻辑: cmdBuilder.append("\"").append(context.getWdFolder().getAbsolutePath()).append("\" ");
            cmd_builder.append(f'"{context.wd_folder.absolute().as_posix()}" ')

        elif param_type_spec == "DataOutput":
            # -------------------- DataOutput 逻辑 --------------------
            cmd_dto.has_output = True
            # 设置输出文件组 UUID
            cmd_dto.output_file_group = str(uuid.uuid4())
            
            # 1. 确定目标文件父目录 (newFilePid)
            directory_value: str
            if raw_value is None:
                # 默认保存与第一个输入路径相同 (使用 input_file_pid)
                # Java 逻辑: directoryValue = cmdDto.inputFilePid;
                directory_value = cmd_dto.input_file_pid or "default-output-pid" 
            else:
                file_parent: Dict[str, Any] | None = raw_value if isinstance(raw_value, dict) else None
                if not file_parent or 'filePid' not in file_parent:
                    print("[WARN] DirectoryHandler: DataOutput rawValue is missing or invalid dictionary or missing filePid. Using default.")
                    directory_value = "default-output-pid"
                else:
                    # Java 逻辑: directoryValue = Objects.toString(fileParent.get("filePid"));
                    directory_value = str(file_parent.get("filePid"))
            
            # 2. 更新 CmdDto 的输出元数据列表
            output_file_real_name = "DirectoryOutputFlagInfo" # 对于 Directory, 这个字符串通常用作占位符
            
            cmd_dto.output_file_real_name_list.append(output_file_real_name)
            cmd_dto.file_front_name_list.append(output_file_real_name)
            cmd_dto.new_file_pid_list.append(directory_value)
            cmd_dto.extension_list.append("") # Directory 没有扩展名
            
            # 3. 构建命令行 (传递工作目录的绝对路径，并加引号)
            # 此处要写绝对路径，exe要求指明路径
            # Java 逻辑: cmdBuilder.append("\"").append(context.getWdFolder().getAbsolutePath()).append("\" ");
            cmd_builder.append(f'"{context.wd_folder.absolute().as_posix()}" ')
        
        else:
            print(f"[WARN] DirectoryHandler: Unrecognized Type: {param_type_spec}. Skipping.")