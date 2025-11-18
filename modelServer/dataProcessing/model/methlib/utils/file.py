import json
import os
import uuid
from typing import Dict, Any, List, Optional, Union
from pathlib import Path
from datetime import datetime
from abc import ABC, abstractmethod
import rasterio
import requests # 引入 requests 库用于 HTTP 请求
from urllib.parse import urlparse, unquote # 引入用于 URL 解析和解码的库
import re
from dataProcessing.config import current_config as CONFIG
import io
import rasterio
from rasterio.io import MemoryFile
from rio_cogeo.cogeo import cog_translate
from rio_cogeo.profiles import cog_profiles
from dataProcessing.Utils.osUtils import getMinioClient, uploadLocalFile # 引入正则表达式库

# ----------------------------------------------------
# 模拟文件操作工具类 (替换 Java FileUtils)
# ----------------------------------------------------
class FileUtils:

    @staticmethod
    def get_file_name_without_suffix(file_name: str) -> str:
        """
        获取不带后缀的文件名（即移除最后一个点及其之后的部分）。
        如果文件名中不包含点，则返回原始文件名。
        
        Args:
            file_name: 完整的文件名字符串。
            
        Returns:
            不包含后缀的文件名。
        """
        if not file_name:
            return ""
            
        # 查找最后一个点的位置
        last_dot_index = file_name.rfind('.')
        
        # 如果没有点 (last_dot_index == -1)，返回原始文件名
        # 否则，返回从开头到最后一个点前一位的子串
        # 对标 Java: (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
        return file_name if last_dot_index == -1 else file_name[:last_dot_index]

    @staticmethod
    def get_file_suffix(file_name: Optional[str]) -> str:
        """
        获取文件的后缀名（即最后一个点之后的部分）。
        如果输入为 None、空字符串或文件名中不包含点，则返回空字符串。
        
        Args:
            file_name: 完整的文件名字符串，可以是 None。
            
        Returns:
            文件的扩展名（不包含点），如果不存在则返回空字符串。
        """
        # 对标 Java: if (fileName == null) return "";
        # 对标 Java: if (fileName.isEmpty()) return "";
        if not file_name:
            return ""
            
        # 查找最后一个点的位置
        last_dot_index = file_name.rfind('.')
        
        # 如果没有点 (last_dot_index == -1)，返回空字符串
        # 否则，返回从最后一个点后一位开始的子串（即后缀）
        # 对标 Java: (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
        return "" if last_dot_index == -1 else file_name[last_dot_index + 1:]
    
    @staticmethod
    def _get_file_suffix(filename: str) -> str:
        """
        获取文件后缀名 (模拟 Java FileUtils.getFileSuffix)。
        """
        return filename.split('.')[-1] if '.' in filename else ''

    @staticmethod
    def _get_filename_from_headers(response: requests.Response) -> str:
        """
        尝试从 Content-Disposition 头部获取文件名。
        """
        cd = response.headers.get('content-disposition')
        if not cd:
            return ""
        
        # 尝试匹配 filename="...", filename*=utf-8''...
        filename_match = re.search(r'filename\*?=["\']?(.*?)["\']?(?:;|$)', cd)
        if filename_match:
            filename_part = filename_match.group(1)
            # 处理 filename*=utf-8''encoded_name 的情况
            if "''" in filename_part:
                filename = filename_part.split("''")[-1].strip()
            else:
                filename = filename_part.strip()
            
            # 基础的URL解码
            return unquote(filename)
            
        return ""

    @staticmethod
    def download_file(file_url: str, save_dir: Union[str, Path], is_uuid: bool) -> Path:
        """
        将文件从给定的URL下载到指定目录。
        对标 Java IFileInfoService.downloadFile 的核心逻辑。

        Args:
            file_url: 文件的完整URL。
            save_dir: 文件保存的本地目录。
            is_uuid: 是否使用UUID作为文件名以防止重名。
        
        Returns:
            目标文件的完整Path对象。
        """
        print(f"--- Attempting to download file from: {file_url} ---")
        
        # 1. 发起HTTP请求
        try:
            # stream=True 启用流式下载，timeout 设置超时
            response = requests.get(file_url, stream=True, timeout=60)
            response.raise_for_status() # 检查并抛出 HTTPError (4xx或5xx)
        except requests.exceptions.RequestException as e:
            # 模拟 Java 的 IOException 抛出
            raise IOError(f"Failed to connect or download from {file_url}: {e}") from e

        # 2. 获取文件名 (先尝试头部，后尝试URL路径)
        file_name = FileUtils._get_filename_from_headers(response)
        
        if not file_name:
            # Fallback to URL path的最后部分
            parsed_url = urlparse(file_url)
            file_name = Path(parsed_url.path).name
            
        if not file_name:
            # 兼容 Java 逻辑: 如果找不到文件名，抛出错误
            raise ValueError(f"Could not determine filename for URL: {file_url}")

        # 3. 处理 UUID 重命名 (防止重名)
        if is_uuid:
            suffix = FileUtils._get_file_suffix(file_name)
            file_name = f"{uuid.uuid4()}.{suffix}" if suffix else str(uuid.uuid4())

        # 4. 目录创建
        directory = Path(save_dir)
        directory.mkdir(parents=True, exist_ok=True) # 模拟 Java Files.createDirectories

        # 5. 构造目标路径
        target_path = directory / file_name

        # 6. 文件写入 (流式写入)
        try:
            with open(target_path, 'wb') as f:
                # 迭代响应内容块，分块写入
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk: 
                        f.write(chunk)
                        
        except IOError as e:
            # 模拟 Java 的 IOException 抛出
            raise IOError(f"Failed to write file to {target_path}") from e

        print(f"文件下载成功: {target_path.as_posix()}")
        return target_path


    @staticmethod
    def copy_files_to_directory(src_files: List[Path], dest_dir: str) -> List[Path]:
        """
        将源文件列表复制到目标目录。
        返回目标目录中的新文件路径列表。
        
        此实现是模拟版本，不会执行实际文件I/O。
        """
        dest_path = Path(dest_dir)
        new_paths = []
        
        for src in src_files:
            # 模拟文件复制操作，返回工作目录下的路径
            dest_file_path = dest_path / src.name
            print(f"[Mock] 复制文件: {src.name} 到 {dest_file_path.as_posix()}")
            # 在实际系统中，这里会执行文件IO
            new_paths.append(dest_file_path)
        
        return new_paths


    @staticmethod
    def delete_files_from_directory(files: List[Path]):
        """删除指定路径列表中的文件（兼容 str / Path / 文件对象）"""
        from pathlib import Path
        import os

        # 统一转换为 Path 对象
        normalized = []
        for f in files:
            if isinstance(f, Path):
                normalized.append(f)
            elif hasattr(f, "name") and isinstance(f.name, str):
                normalized.append(Path(f.name))
            elif isinstance(f, str):
                normalized.append(Path(f))
            else:
                continue  # 不可识别的类型，跳过

        print(f"[FileUtils] 正在删除临时输入文件: {[p.as_posix() for p in normalized]}")

        for file in normalized:
            try:
                if file.exists() and file.is_file():
                    os.unlink(file)
            except OSError as e:
                print(f"[ERROR] 无法删除文件 {file}: {e}")


    @staticmethod
    def get_all_files_from_directory(directory_path: Path) -> List[Path]:
        """递归获取指定目录下的所有文件。"""
        file_list = []
        if directory_path.is_dir():
            # 使用 rglob 递归遍历所有文件和目录
            for item in directory_path.rglob('*'):
                if item.is_file():
                    file_list.append(item)
        else:
            print(f"[FileUtils] 提供的路径不是有效的目录: {directory_path}")
        return file_list

    @staticmethod
    def upload_file_to_server(file_path, bucket, object_path) -> str:
        uploadLocalFile(file_path, bucket, object_path)
        return f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{CONFIG.MINIO_TEMP_FILES_BUCKET}/{object_path}"

    @staticmethod
    def convert_to_cog_and_upload(file_path, bucket, object_path) -> str:
        """
        将普通GeoTIFF转换为COG格式并上传到MinIO，返回MinIO URL
        """
        client = getMinioClient()

        # 打开输入文件
        with rasterio.open(file_path) as src:
            profile = cog_profiles.get("deflate")  # 可选: deflate/lzw/zstd等压缩方式
            profile.update({
                "BIGTIFF": "YES",
                "NUM_THREADS": "ALL_CPUS",
                "BLOCKSIZE": 256,
                "COMPRESS": "LZW",
                "OVERVIEWS": "AUTO",
                "OVERVIEW_RESAMPLING": "NEAREST",
            })
            # 使用内存文件作为输出
            with MemoryFile() as memfile:
                cog_translate(
                    src,
                    memfile.name,  # 内存文件临时名称
                    profile,
                    in_memory=True,  # 关键参数: 直接输出到内存
                    quiet=True,
                )

                # 获取生成的 COG 数据
                cog_bytes = memfile.read()

        # 上传到 MinIO
        cog_name = object_path
        cog_bytesio = io.BytesIO(cog_bytes)
        cog_bytesio.seek(0)

        client.put_object(
            bucket_name=bucket,
            object_name=cog_name,
            data=cog_bytesio,
            length=len(cog_bytes),
            content_type="image/tiff",
        )

        # 构造 URL
        url = f"http://{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}/{bucket}/{cog_name}"
        return url