from abc import ABC, abstractmethod
from typing import Any
import os
import zipfile
from typing import Any, List

from dataProcessing.model.methlib.schemas.command import CmdContext


class ParameterHandler(ABC):
    """参数处理器抽象类"""

    @abstractmethod
    def supports(self, parameter_type: Any) -> bool:
        """判断是否支持某种类型"""
        pass

    @abstractmethod
    def parse(self, parameter_type: Any, raw_value: Any, val_index: int, context: CmdContext, is_external_call: bool):
        """解析参数"""
        pass

    # --------------------- 默认工具方法 ---------------------
    def handle_null(self, raw_value: Any, cmd_builder: list, param_specs: dict):
        """处理空值"""
        if raw_value is None:
            default = param_specs.get("default_value")
            if default is None:
                # 如果没有默认值，删除 flag（模拟原来的操作）
                if cmd_builder:
                    # 删除最后一个空格前的最后一段
                    last_space = "".join(cmd_builder).rfind(" ")
                    if last_space != -1:
                        cmd_builder[-1] = "".join(cmd_builder)[last_space + 1:]
            else:
                cmd_builder.append(f"{default} ")
        else:
            value = str(raw_value)
            if value in ("true", "false") or value.replace('.', '', 1).isdigit() or (value.startswith('-') and value[1:].replace('.', '', 1).isdigit()):
                cmd_builder.append(f"{value} ")
            else:
                cmd_builder.append(f'"{value}" ')

    def handle_input(self, cmd_builder: list, index: int, size: int, file_path: str):
        """处理多文件输入"""
        if index == 0 and size > 1:
            cmd_builder.append(f'"{file_path};')
        elif index == 0 and size == 1:
            cmd_builder.append(f'"{file_path}" ')
        elif index != size - 1:
            cmd_builder.append(f'{file_path};')
        else:
            cmd_builder.append(f'{file_path}" ')

    # --------------- 复现 Java handleMultipartFile ----------------

    def handle_multipart_file(self, file_path: str, context: CmdContext, index: int, size: int):
        """
        完整复现 Java handleMultipartFile(File file, CmdDto cmdDto, CmdContextDto context...)
        """
        filename = os.path.basename(file_path).lower()

        # --- 如果是 ZIP：解压 ---
        if filename.endswith(".zip"):
            extract_dir = context.wd_folder  # 解压到工作目录
            unzipped_files = self._unzip(file_path, extract_dir)

            # 记录解压出的文件（对应 cmdDto.inputSrcFiles.addAll）
            context.cmd_dto.input_src_files.extend(unzipped_files)

            # 寻找 shp 主文件
            root_file = None
            for f in unzipped_files:
                if f.lower().endswith(".shp"):
                    root_file = f
                    break

            # 如果找到了 shp 用 shp，否则原始 zip 路径即可
            final_input = root_file if root_file else file_path

            self.handle_input(context.cmd_builder, index, size, final_input)
        else:
            # 不是 ZIP，不需要展开 → 直接让 exe 自己报错
            self.handle_input(context.cmd_builder, index, size, file_path)

    @staticmethod
    def _unzip(zip_file, extract_dir) -> List[str]:
        """对标 Java FileUtils.unzip → 返回解压出文件路径列表"""
        extracted_files = []
        with zipfile.ZipFile(zip_file, 'r') as z:
            z.extractall(extract_dir)
            for name in z.namelist():
                extracted_files.append(os.path.join(extract_dir, name))
        return extracted_files
