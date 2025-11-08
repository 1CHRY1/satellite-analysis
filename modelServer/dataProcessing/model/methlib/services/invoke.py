import shutil
import subprocess
import time
from typing import Any, Dict, List
from pathlib import Path
import logging
import sys
import ray
from dataProcessing.config import current_config as CONFIG
from dataProcessing.model.methlib.parsers.command_processor import CommandProcessor
from dataProcessing.model.methlib.schemas.command import CmdDto
from dataProcessing.model.methlib.schemas.method import MethodEntity
from dataProcessing.model.methlib.utils.file import FileUtils

# =========================================================================
logging.basicConfig(
    level=logging.INFO, # 设置全局最低级别为 INFO
    format='%(asctime)s | %(levelname)s | %(filename)s:%(lineno)d | %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    stream=sys.stderr 
)
logger = logging.getLogger(__name__) 
# =========================================================================


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
        return {
            "status": 'ERROR',
            "result": msg
        }

@ray.remote(num_cpus=CONFIG.RAY_NUM_CPUS, memory=CONFIG.RAY_MEMORY_PER_TASK)
def invoke(cmdDto: CmdDto) -> Dict[str, Any]:
    """
    Python 同步版本命令调度方法。
    忽略了事务和异步并发逻辑，使用同步进程执行。
    """
    
    # 1. 获取命令 DTO
    if cmdDto is None:
        error_msg = "CmdDto 获取失败：缺少必要参数或文件下载失败。"
        logger.error(error_msg)
        return R.error("Failed to download file or missing necessary parameters.")

    # 初始化结果变量
    info = ""
    all_file_id: List[str] = []
    fileIdMap: Dict[str, List[str]] = {}
    tmp_path = Path(cmdDto.tmp_file_path)

    try:
        # 2. 启动并等待进程执行 (使用 subprocess.run 简化同步执行和I/O捕获)
        # shell=True 允许执行完整的命令字符串，cwd 设置工作目录
        logger.info(f"正在执行命令: {cmdDto.cmd}")
        process_result = subprocess.run(
            cmdDto.cmd, 
            shell=True,
            cwd=str(tmp_path), # 在临时工作目录下执行命令
            capture_output=True, 
            text=True, 
            encoding='utf-8',
            # 建议：可以加上 timeout=N 秒，防止外部进程挂起
        )

        # 3. 收集输出信息 (核心日志和错误暴露改进点)
        info_stdout = process_result.stdout.strip()
        info_stderr = process_result.stderr.strip()
        
        # 将标准输出记录为 INFO 级别
        if info_stdout:
            pass
            # logger.info(f"进程标准输出:\n{info_stdout}")
        
        # 将标准错误（潜在的隐藏错误日志）记录为 WARNING 级别
        if info_stderr:
            pass
            # logger.warning(f"进程标准错误 (可能包含隐藏错误):\n{info_stderr}")
            
        # 合并所有输出到 info 变量，用于返回结果
        info = info_stdout
        if info_stderr:
             # 如果 stderr 不为空，在 info 中将其与 stdout 分隔开
             info += f"\r\n--- Standard Error ---\r\n{info_stderr}"
            
        logger.info(f"命令执行完成。返回码: {process_result.returncode}")
        
        # 4. 清理输入文件
        # 注意：FileUtils.delete_files_from_directory 内部也应该使用 logger
        FileUtils.delete_files_from_directory(cmdDto.input_src_files)

        if process_result.returncode != 0:
            logger.error(f"进程执行失败。返回码: {process_result.returncode}")
            info += "\r\n Process finished with errors."
            return R.error(info)
        else:
            # 5. 处理输出文件上传
            if cmdDto.has_output:
                for key, value in cmdDto.output_file_name_list.items():
                    fileIdList: List[str] = []
                    
                    if value == "DirectoryOutputFlagInfo":
                        files_to_upload = FileUtils.get_all_files_from_directory(str(tmp_path))
                    else:
                        target_stem = FileUtils.get_file_name_without_suffix(value)
                        
                        files_to_upload: List[Path] = []
                        for file in tmp_path.iterdir():
                            if file.is_file():
                                file_stem = file.stem
                                if file_stem.lower() == target_stem.lower():
                                    files_to_upload.append(file)
                        
                        if not files_to_upload and value == target_stem:
                            for file in tmp_path.iterdir():
                                if file.is_file() and file.name.lower() == value.lower():
                                    files_to_upload.append(file)
                        
                    # 上传匹配到的文件
                    for file in files_to_upload:
                        try:
                            # 假设 FileUtils.upload_file_to_server 存在
                            fileId = FileUtils.upload_file_to_server(file_path=file, wd=Path(tmp_path).name)
                            fileIdList.append(fileId)
                            all_file_id.append(fileId)
                        except Exception as e:
                            logger.error(f"文件上传失败: {file} - {e}")
                            return R.error(f"File upload failed: {e}")

                    fileIdMap[key] = fileIdList

            else:
                # 6. 无输出（纯输入）：上传原文件
                logger.info("配置为无输出，上传所有原始输入文件。")
                files_to_upload = FileUtils.get_all_files_from_directory(str(tmp_path))
                fileIdList: List[str] = []
                for file in files_to_upload:
                    try:
                        fileId = FileUtils.upload_file_to_server(file_path=file, wd=cmdDto.tmp_file_path)
                        fileIdList.append(fileId)
                        all_file_id.append(fileId)
                    except Exception as e:
                        logger.error(f"文件上传失败: {file} - {e}")
                        return R.error(f"File upload failed: {e}")
                
                fileIdMap["Origin"] = fileIdList

    except FileNotFoundError:
        # 外部命令未找到异常
        error_msg = f"Error: Command '{cmdDto.cmd.split()[0]}' not found."
        logger.error(error_msg)
        info += f"\r\n{error_msg}"
        return R.error(info)
    except Exception as e:
        # 捕获其他执行异常
        logger.exception("执行命令时发生内部异常") 
        info += f"\r\nError executing command: {type(e).__name__}: {e}"
        return R.error(info)
    finally:
        # 清理临时工作目录 (无论成功失败都尝试清理)
        if tmp_path.exists():
              shutil.rmtree(str(tmp_path), ignore_errors=True)
              logger.info(f"临时目录已清理: {str(tmp_path)}")


    # 7. 构建最终结果
    result = {
        "info": info,
        "output": fileIdMap,
    }
    
    # 8. 返回成功响应
    return R.ok(result)