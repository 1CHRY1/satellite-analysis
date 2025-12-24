from dataclasses import dataclass, field
from typing import Any, List, Dict, Optional
from datetime import datetime
from pathlib import Path

@dataclass
class MethodEntity:
    """方法实体：存储工具的静态配置信息，对标 Java MethodEntity 和 container_method 表结构"""
    # 核心字段 (CommandProcessor使用或关键标识符)
    id: Optional[int] = None           # bigint(20) 主键ID
    uuid: Optional[str] = None         # varchar(100) 唯一标识
    name: Optional[str] = None         # varchar(100) 方法名称
    nameZh: Optional[str] = None         # varchar(100) 方法名称
    params: Optional[str] = None       # json 参数信息 (JSON字符串)
    paramsZh: Optional[str] = None       # json 参数信息 (JSON字符串)
    execution: Optional[str] = None    # varchar(50) 执行类型 (e.g., "py", "exe", "jar")
    category: Optional[str] = None     # varchar(255) 条目
    tags: Optional[List[str]] = None

    # 附加字段 (来自完整的 container_method 表，通常为描述或元数据)
    description: Optional[str] = None  # varchar(255) 描述信息
    descriptionZh: Optional[str] = None  # varchar(255) 描述信息
    longDesc: Optional[str] = None    # text 详细描述信息
    longDescZh: Optional[str] = None    # text 详细描述信息
    copyright: Optional[str] = None    # varchar(255) 版权信息
    type: Optional[str] = None         # varchar(50) 方法类型
    createTime: Optional[datetime] = None # datetime 创建时间