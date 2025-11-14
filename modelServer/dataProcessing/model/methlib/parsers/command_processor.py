# 全局工厂初始化 (模拟 dataProcessing.model.methlib.__init__.py)
from datetime import datetime
import json
from pathlib import Path
from typing import Any, Dict, List
import uuid
from dataProcessing.model.methlib.parsers.factory import ParameterHandlerFactory
from dataProcessing.model.methlib.schemas.command import CmdContext, CmdDto
from dataProcessing.model.methlib.schemas.method import MethodEntity
from dataProcessing.model.methlib import init_handler_factory
from dataProcessing.config import current_config as CONFIG


class CommandProcessor:
    """核心命令处理器：负责根据方法和请求参数构建最终的命令行字符串"""
    def __init__(self):
        # 模拟 @Autowired 注入
        self.parameter_handler_factory = init_handler_factory()

        # 模拟 @Value 配置
        self.method_wd = CONFIG.METHOD_WD
        self.data_fd = CONFIG.DATA_FD
        self.method_pd = CONFIG.METHOD_PD

    def get_cmd(self, user_id: str, method: MethodEntity, service_base_path: str, service_uuid: str, req_params: Dict[str, Any], start_time: datetime, is_external_call: bool) -> CmdDto | None:
        
        # 1. 解析参数规格
        param_specs_list = method.params

        cmd_builder: List[str] = []
        is_success = True
        
        # 2. 创建工作目录 (wdFolder)
        wd_folder_path = Path(self.method_wd) / str(uuid.uuid4())
        try:
            wd_folder_path.mkdir(parents=True, exist_ok=True)
        except OSError as e:
            print(f"Error creating working directory: {e}")
            return None 

        # 3. 构建基础命令前缀
        if method.id > 478:
            # 非Whitebox方法: 添加 cd 命令
            cmd_builder.append(f"cd /d \"{wd_folder_path.absolute().as_posix()}\" && ")
        
        cmd_builder.append(self._get_tool_cmd(method, method.uuid))
        
        # 针对 Whitebox, 添加方法名和临时工作目录
        if method.id < 479:
            cmd_builder.append(f"-r={method.name} ")
            cmd_builder.append(f"--wd=\"{wd_folder_path.absolute().as_posix()}\" ")
            
        # 针对 Saga 方法，添加 toolIndex
        if 581 < method.id < 1342:
            cmd_builder.append(f"{method.category} ")

        # 4. 初始化上下文
        cmd_dto = CmdDto()
        cmd_context = CmdContext(
            cmd_builder=cmd_builder, 
            param_specs={},
            wd_folder=wd_folder_path, 
            cmd_dto=cmd_dto, 
            service_base_path=service_base_path, 
            service_uuid=service_uuid, 
            user_id=user_id
        )

        # 5. 参数处理核心循环
        try:
            for i, param_specs in enumerate(param_specs_list):
                val_key = f"val{i}"
                if val_key not in req_params:
                    # 用户没有操作此项参数
                    continue
                
                # 5.1 处理 Flags (添加参数标识符)
                self._process_flags(cmd_builder, param_specs)

                # 5.2 更新上下文的 paramSpecs
                cmd_context.param_specs = param_specs

                # 5.3 获取处理器并解析
                parameter_type_obj = param_specs.get("parameter_type")
                
                handler = self.parameter_handler_factory.get_handler(parameter_type_obj)
                
                handler.parse(
                    parameter_type=parameter_type_obj,
                    raw_value=req_params.get(val_key),
                    val_index=i,
                    context=cmd_context,
                    is_external_call=is_external_call
                )

        except Exception as e:
            is_success = False
            print(f"Parameters Error occurred: {e}")
            
        if not is_success:
            return None

        # 6. 整理 CmdDto 输出
        cmd_dto = cmd_context.cmd_dto
        cmd_dto.cmd = "".join(cmd_context.cmd_builder).strip()
        
        # 路径设置
        if not is_external_call:
            pass
            # cmd_dto.output_file_path = str(Path(self.data_fd) / str(user_id))
        
        cmd_dto.tmp_file_path = str(wd_folder_path.absolute().as_posix())
        
        return cmd_dto

    def _process_flags(self, cmd_builder: List[str], param_specs: Dict[str, Any]):
        """
        处理参数输入标识 flags。
        严格复刻 Java processFlags 逻辑：只处理 List 类型，空 List 也要加一个空格。
        """
        flags_obj = param_specs.get("Flags")
        if isinstance(flags_obj, list):
            if flags_obj:
                cmd_builder.append(f"{flags_obj[0]} ")
            else:
                # 严格匹配 Java 行为：空列表也加一个空格
                cmd_builder.append(" ")
        # 如果不是 list (包括 None)，则不操作，匹配 Java 行为。

    def _get_tool_cmd(self, method: MethodEntity, uuid: str) -> str:
        """获取工具的可执行地址和前缀命令，匹配 Java getToolCmd 逻辑"""
        
        path_prefix = Path(self.method_pd) / uuid

        # 特殊处理 Whitebox 方法 (id < 479L)，直接写死 whitebox_tools 路径
        if method.id < 479:
            # 构造完整可执行路径
            tool_path = (path_prefix / "whitebox_tools").absolute().as_posix()
            # 注意这里不加引号，因为原逻辑 EXE 模式不加引号
            return f"{tool_path} "

        # === 下面保持原逻辑 ===
        path = ""
        extension_map = {
            "exe": [("*.exe", "", " ")],
            "jar": [("*.jar", "java -jar ", " ")],
        }

        execution_type = method.execution

        if execution_type == "py":
            tool_info = extension_map.get("py")[0]
        elif execution_type == "exe":
            tool_info = extension_map.get("exe")[0]
        elif execution_type == "jar":
            tool_info = extension_map.get("jar")[0]
        else:
            return ""

        glob_pattern, prefix, suffix = tool_info
        tool_files = list(path_prefix.glob(glob_pattern))
        
        if tool_files:
            tool_path = tool_files[0].absolute().as_posix()
            if execution_type == "py" or execution_type == "jar":
                 path = f"{prefix}\"{tool_path}\"{suffix}"
            else:
                 path = f"{prefix}{tool_path}{suffix}"
        
        return path

