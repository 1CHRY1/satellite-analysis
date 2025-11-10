from typing import Any, List, Dict
from pathlib import Path

class CmdDto:
    """
    对应 Java 的 CmdDto。
    所有字段均按照 Java 原始定义完整复现，使用 Pythonic 的 snake_case 命名。
    """

    # 核心命令和路径
    def __init__(self):
        self.cmd: str = ""
        self.output_file_path: str = ""
        self.tmp_file_path: str = ""
        
        # Java List<String> 字段
        self.output_file_real_name_list: List[str] = []
        self.file_front_name_list: List[str] = []
        self.new_file_pid_list: List[str] = []
        self.extension_list: List[str] = []
        self.input_params: List[str] = []
        self.input_file_ids: List[str] = []

        # Java String 字段
        self.input_file_pid: str = "0"
        self.output_file_group: str = ""

        # Java List<File> 字段 (使用 pathlib.Path 保持文件对象语义)
        # 注意：这里使用 Path 是为了模拟 Java File 对象的概念，但在 Python 中 Path 更常用。
        self.input_src_files: List[Path] = []

        # Java Boolean 字段
        self.has_output: bool = False

        # Java Map<String, Object> 字段
        self.output_file_name_list: Dict[str, Any] = {}


class CmdContext:
    """
    对应 Java 的 CmdContextDto，存储命令构建过程中的上下文信息。
    cmd_builder 使用 List[str] 模拟 Java 的 StringBuilder。
    wd_folder 使用 pathlib.Path 模拟 Java 的 File。
    """
    def __init__(self, cmd_builder: List[str], param_specs: Dict[str, Any], wd_folder: Path, cmd_dto: CmdDto, service_base_path: str, service_uuid: str, user_id: int):
        self.cmd_builder = cmd_builder
        self.param_specs = param_specs
        self.wd_folder = wd_folder
        self.cmd_dto = cmd_dto
        self.service_base_path = service_base_path
        self.service_uuid = service_uuid
        self.user_id = user_id