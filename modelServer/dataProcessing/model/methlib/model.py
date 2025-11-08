import json
from datetime import datetime
from typing import Dict, Any, List
from pathlib import Path
import os
import uuid

from dataProcessing.model.methlib.parsers.command_processor import CommandProcessor
from dataProcessing.model.methlib.schemas.command import CmdDto
from dataProcessing.model.methlib.schemas.method import MethodEntity
from dataProcessing.model.methlib.services.invoke import invoke
from dataProcessing.model.task import Task

class MethLib(Task):
    """
    MethLib 是一个任务执行器，负责从 CommandProcessor 获取命令行，
    并执行后续的调用（如调用 Whitebox 或 Saga 接口）。
    """
    
    def run(self):
        # ----------- 1. 获取方法定义 和 参数 -----------
        data = self.args[0]
        req_params = data.get('params', {}) # 前端传入的参数 (Dict)
        method_data = data.get('method', {}) # 方法实体 (MethodEntity 或 Dict)

        print("--- 接收到的输入数据 ---")
        print(f"请求参数: {req_params}")
        print(f"方法实体: {method_data.name if isinstance(method_data, MethodEntity) else method_data.get('name')}")
        
        # 兼容 dict 和 MethodEntity 对象
        self.req_params = req_params
        if isinstance(method_data, dict):
            try:
                # 使用字典解包将所有字段映射到 MethodEntity
                method_data = MethodEntity(**method_data)
                print(">>> 成功将传入的字典转换为 MethodEntity 实例。")
            except TypeError as e:
                # 捕获因字段不匹配导致的错误
                print(f"错误: 无法将方法数据字典转换为 MethodEntity 实例: {e}")
                return {"error": "Invalid Method Entity structure"}

        if not isinstance(method_data, MethodEntity):
            print("错误: method_data 既不是 MethodEntity 实例也无法转换为 MethodEntity 字典。")
            return {"error": "Invalid Method Entity"}

        # ----------- 2. 核心业务逻辑：调用 CommandProcessor 构建命令 -----------
        
        # 实例化 CommandProcessor (它会自动通过 init_handler_factory 初始化依赖)
        processor = CommandProcessor()

        # 模拟其他必要的上下文信息
        user_id = 'USRyM76hPQaVXhAM6MG3'
        service_base_path = "/srv/api/v1/" # 没用
        service_uuid = "svc-ext-2024" # 没用
        start_time = datetime.now()
        is_external_call = True # 默认标记为外部调用，影响 CmdDto 的 output_file_path

        cmd_dto: CmdDto | None = processor.get_cmd(
            user_id=user_id,
            method=method_data,
            service_base_path=service_base_path, # 针对云盘的
            service_uuid=service_uuid, # 针对云盘的
            req_params=req_params,
            start_time=start_time,
            is_external_call=is_external_call
        )

        if not cmd_dto:
            return {"error": "Failed to build command"}

        print("\n--- CommandProcessor 构建结果 ---")
        print(f"最终命令行: {cmd_dto.cmd}")
        print(f"临时工作路径: {cmd_dto.tmp_file_path}")



        # ----------- 3. 调用并返回结果 -----------
        result = invoke(cmdDto=cmd_dto)
        # 执行成功
        return result