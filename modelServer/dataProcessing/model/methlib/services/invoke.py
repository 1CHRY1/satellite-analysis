import shutil
import subprocess
import time
from typing import Any, Dict, List
from pathlib import Path

from dataProcessing.model.methlib.parsers.command_processor import CommandProcessor
from dataProcessing.model.methlib.schemas.command import CmdDto
from dataProcessing.model.methlib.schemas.method import MethodEntity
from dataProcessing.model.methlib.utils.file import FileUtils

class R:
    """
    对应 Java 的 CmdDto。
    所有字段均按照 Java 原始定义完整复现，使用 Pythonic 的 snake_case 命名。
    """
    def ok(info: Any):
        return {
            "status": 'SUCCESS',
            "result": info
        }
    
    def error(msg: str):
        print(msg)
        return {
            "status": 'ERROR',
            "result": msg
        }


def invoke(cmdDto: CmdDto) -> Dict[str, Any]:
    """
    Python 同步版本命令调度方法。
    忽略了事务和异步并发逻辑，使用同步进程执行。
    """
    
    # 1. 获取命令 DTO
    if cmdDto is None:
        print("CmdDto 获取失败：缺少必要参数或文件下载失败。")
        return R.error("Failed to download file or missing necessary parameters.")

    # 初始化结果变量
    info = ""
    all_file_id: List[str] = []
    fileIdMap: Dict[str, List[str]] = {}
    # tmp_path = cmdDto.tmp_file_path
    tmp_path = Path(cmdDto.tmp_file_path)

    try:
        # 2. 启动并等待进程执行 (使用 subprocess.run 简化同步执行和I/O捕获)
        # shell=True 允许执行完整的命令字符串，cwd 设置工作目录
        print(f"[EXEC] 正在执行命令: {cmdDto.cmd}")
        process_result = subprocess.run(
            cmdDto.cmd, 
            shell=True,
            cwd=str(tmp_path), # 在临时工作目录下执行命令
            capture_output=True, 
            text=True, 
            encoding='utf-8'
        )

        # 3. 收集输出信息
        # 标准输出和标准错误会被 process_result 捕获
        info += process_result.stdout
        info += process_result.stderr
        
        print(f"[INFO] 命令执行完成。返回码: {process_result.returncode}")
        
        # 4. 清理输入文件
        FileUtils.delete_files_from_directory(cmdDto.input_src_files)

        if process_result.returncode != 0:
            print("[ERROR] 进程执行失败。")
            info += "\r\n Process finished with errors."
            return R.error(info)
        else:
            # 5. 处理输出文件上传
            if cmdDto.has_output:
                # 遍历用户定义的输出文件列表
                for key, value in cmdDto.output_file_name_list.items():
                    fileIdList: List[str] = []
                    
                    if value == "DirectoryOutputFlagInfo":
                        # a) 目录输出：上传临时路径下的所有文件 (包括子目录中的文件)
                        files_to_upload = FileUtils.get_all_files_from_directory(str(tmp_path))
                    else:
                        # b) 文件输出：根据文件名匹配
                        # 查找工作目录下文件名（不含后缀）与 value 匹配的文件
                        target_stem = FileUtils.get_file_name_without_suffix(value)
                        
                        files_to_upload: List[Path] = []
                        # 遍历工作目录下的所有文件
                        for file in tmp_path.iterdir():
                            if file.is_file():
                                file_stem = file.stem
                                # 原始 Java 逻辑是忽略后缀，用equalsIgnoreCase比较文件名
                                if file_stem.lower() == target_stem.lower():
                                    files_to_upload.append(file)
                        
                        # 特殊处理：如果 value 包含了后缀，也需要匹配完整文件名
                        if not files_to_upload and value == target_stem:
                             # 尝试匹配完整文件名 (无后缀的文件)
                            for file in tmp_path.iterdir():
                                if file.is_file() and file.name.lower() == value.lower():
                                    files_to_upload.append(file)
                        
                    # 上传匹配到的文件
                    for file in files_to_upload:
                        try:
                            fileId = FileUtils.upload_file_to_server(file_path=file, wd=Path(tmp_path).name)
                            fileIdList.append(fileId)
                            all_file_id.append(fileId)
                        except Exception as e:
                            print(f"[ERROR] 文件上传失败: {e}")
                            return R.error(f"File upload failed: {e}")

                    # 记录结果：key为用户定义名，value为文件ID列表
                    fileIdMap[key] = fileIdList

            else:
                # 6. 无输出（纯输入）：上传原文件
                print("[INFO] 配置为无输出，上传所有原始输入文件。")
                files_to_upload = FileUtils.get_all_files_from_directory(str(tmp_path))
                fileIdList: List[str] = []
                for file in files_to_upload:
                    try:
                        fileId = FileUtils.upload_file_to_server(file_path=file, wd=cmdDto.tmp_file_path)
                        fileIdList.append(fileId)
                        all_file_id.append(fileId)
                    except Exception as e:
                        print(f"[ERROR] 文件上传失败: {e}")
                        return R.error(f"File upload failed: {e}")
                
                fileIdMap["Origin"] = fileIdList

    except FileNotFoundError:
        info += f"\r\nError: Command '{cmdDto.cmd.split()[0]}' not found."
        return R.error(info)
    except Exception as e:
        # 捕获其他执行异常
        print(f"[ERROR] 执行命令时发生异常: {e}")
        info += f"\r\nError executing command: {e}"
        return R.error(info)
    finally:
        # 清理临时工作目录 (无论成功失败都尝试清理)
        if tmp_path.exists():
             shutil.rmtree(str(tmp_path), ignore_errors=True)
             print(f"[CLEANUP] 临时目录已清理: {str(tmp_path)}")


    # 7. 构建最终结果
    result = {
        "info": info,
        "output": fileIdMap,
    }
    
    # 8. 返回成功响应
    return R.ok(result)