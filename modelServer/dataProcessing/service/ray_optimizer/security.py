"""
Ray优化器安全检查模块

提供代码安全验证、资源限制和执行环境隔离功能
"""

import ast
import resource
import platform
import logging
import hashlib
from contextlib import contextmanager
from typing import Optional

from .config import get_config
from .exceptions import SecurityError, ResourceLimitError

logger = logging.getLogger(__name__)


def get_code_hash(code: str) -> str:
    """生成代码哈希值用于缓存和追踪"""
    return hashlib.md5(code.encode('utf-8')).hexdigest()


def check_code_security(code: str) -> None:
    """
    检查代码安全性
    
    Args:
        code: 要检查的Python代码
        
    Raises:
        SecurityError: 如果代码包含不安全操作
    """
    config = get_config()
    
    # 检查代码长度
    if len(code) > config.MAX_FILE_SIZE:
        raise SecurityError(f"Code size exceeds limit: {len(code)} bytes")
    
    # 检查禁止的操作
    _check_forbidden_operations(code, config)
    
    # AST安全检查
    try:
        tree = ast.parse(code)
        visitor = SecurityVisitor(config)
        visitor.visit(tree)
    except SyntaxError as e:
        raise SecurityError(f"Syntax error in code: {e}")
    except Exception as e:
        raise SecurityError(f"Security check failed: {e}")


def _check_forbidden_operations(code: str, config) -> None:
    """检查禁止的操作"""
    code_lower = code.lower()
    for forbidden_op in config.FORBIDDEN_OPERATIONS:
        if forbidden_op in code_lower:
            raise SecurityError(f"Forbidden operation detected: {forbidden_op}")


class SecurityVisitor(ast.NodeVisitor):
    """AST安全检查访问器"""
    
    def __init__(self, config):
        self.config = config
        self.import_violations = []
        self.function_violations = []
    
    def visit_Import(self, node):
        """检查import语句"""
        for alias in node.names:
            module_name = alias.name.split('.')[0]  # 获取顶级模块名
            if module_name not in self.config.ALLOWED_IMPORTS:
                self.import_violations.append(module_name)
                logger.warning(f"Potentially unsafe import: {module_name}")
                # 记录但不直接抛出异常，允许一定的灵活性
        self.generic_visit(node)
    
    def visit_ImportFrom(self, node):
        """检查from ... import语句"""
        if node.module:
            module_name = node.module.split('.')[0]
            if module_name not in self.config.ALLOWED_IMPORTS:
                self.import_violations.append(module_name)
                logger.warning(f"Potentially unsafe import from: {module_name}")
        self.generic_visit(node)
    
    def visit_Call(self, node):
        """检查函数调用"""
        # 检查危险函数调用
        if isinstance(node.func, ast.Name):
            func_name = node.func.id
            if func_name in ['eval', 'exec', 'compile', '__import__']:
                raise SecurityError(f"Forbidden function call: {func_name}")
        
        # 检查属性调用
        elif isinstance(node.func, ast.Attribute):
            if isinstance(node.func.value, ast.Name):
                if (node.func.value.id == 'os' and 
                    node.func.attr in ['system', 'popen', 'spawn']):
                    raise SecurityError(f"Forbidden os operation: {node.func.attr}")
        
        self.generic_visit(node)
    
    def get_security_report(self) -> dict:
        """获取安全检查报告"""
        return {
            'import_violations': self.import_violations,
            'function_violations': self.function_violations,
            'security_level': self._calculate_security_level()
        }
    
    def _calculate_security_level(self) -> str:
        """计算安全级别"""
        violation_count = len(self.import_violations) + len(self.function_violations)
        if violation_count == 0:
            return "SAFE"
        elif violation_count <= 2:
            return "LOW_RISK"
        elif violation_count <= 5:
            return "MEDIUM_RISK"
        else:
            return "HIGH_RISK"


@contextmanager
def resource_limit():
    """
    设置资源限制上下文管理器
    
    注意：在Windows系统上某些资源限制可能不可用
    """
    config = get_config()
    
    # 检查操作系统支持
    if platform.system() == "Windows":
        logger.warning("Resource limits not fully supported on Windows")
        yield
        return
    
    original_limits = {}
    
    try:
        # 保存原始限制
        try:
            original_limits['memory'] = resource.getrlimit(resource.RLIMIT_AS)
            original_limits['cpu'] = resource.getrlimit(resource.RLIMIT_CPU)
        except (AttributeError, OSError):
            logger.warning("Some resource limits not available on this system")
        
        # 设置内存限制
        try:
            memory_limit = config.MAX_MEMORY_MB * 1024 * 1024
            resource.setrlimit(resource.RLIMIT_AS, (memory_limit, memory_limit))
        except (AttributeError, OSError, ValueError) as e:
            logger.warning(f"Could not set memory limit: {e}")
        
        # 设置CPU时间限制
        try:
            cpu_limit = config.MAX_EXECUTION_TIME
            resource.setrlimit(resource.RLIMIT_CPU, (cpu_limit, cpu_limit))
        except (AttributeError, OSError, ValueError) as e:
            logger.warning(f"Could not set CPU time limit: {e}")
        
        yield
        
    except MemoryError:
        raise ResourceLimitError("Memory limit exceeded")
    except Exception as e:
        if "CPU time limit exceeded" in str(e):
            raise ResourceLimitError("CPU time limit exceeded")
        raise
    finally:
        # 重置资源限制
        _restore_resource_limits(original_limits)


def _restore_resource_limits(original_limits: dict) -> None:
    """恢复原始资源限制"""
    try:
        if 'memory' in original_limits:
            resource.setrlimit(resource.RLIMIT_AS, original_limits['memory'])
        if 'cpu' in original_limits:
            resource.setrlimit(resource.RLIMIT_CPU, original_limits['cpu'])
    except (AttributeError, OSError, ValueError) as e:
        logger.warning(f"Could not restore resource limits: {e}")


class CodeValidator:
    """代码验证器 - 提供更细粒度的安全控制"""
    
    def __init__(self):
        self.config = get_config()
        self.validation_rules = self._init_validation_rules()
    
    def _init_validation_rules(self) -> dict:
        """初始化验证规则"""
        return {
            'max_lines': 1000,
            'max_functions': 50,
            'max_classes': 20,
            'max_imports': 30,
            'max_nested_depth': 6
        }
    
    def validate_code_structure(self, code: str) -> dict:
        """验证代码结构"""
        try:
            tree = ast.parse(code)
            validator = StructureValidator(self.validation_rules)
            validator.visit(tree)
            return validator.get_validation_result()
        except SyntaxError as e:
            raise SecurityError(f"Invalid code structure: {e}")
    
    def is_code_safe_for_execution(self, code: str) -> tuple[bool, list]:
        """判断代码是否安全执行"""
        try:
            check_code_security(code)
            structure_result = self.validate_code_structure(code)
            
            warnings = []
            if structure_result['complexity_score'] > 8:
                warnings.append("Code complexity is high")
            if structure_result['import_count'] > 15:
                warnings.append("Many imports detected")
            
            return True, warnings
            
        except SecurityError as e:
            return False, [str(e)]


class StructureValidator(ast.NodeVisitor):
    """代码结构验证器"""
    
    def __init__(self, rules: dict):
        self.rules = rules
        self.stats = {
            'line_count': 0,
            'function_count': 0,
            'class_count': 0,
            'import_count': 0,
            'max_nested_depth': 0,
            'current_depth': 0
        }
    
    def visit_FunctionDef(self, node):
        self.stats['function_count'] += 1
        self._update_depth(1)
        self.generic_visit(node)
        self._update_depth(-1)
    
    def visit_ClassDef(self, node):
        self.stats['class_count'] += 1
        self._update_depth(1)
        self.generic_visit(node)
        self._update_depth(-1)
    
    def visit_Import(self, node):
        self.stats['import_count'] += len(node.names)
        self.generic_visit(node)
    
    def visit_ImportFrom(self, node):
        self.stats['import_count'] += len(node.names) if node.names else 1
        self.generic_visit(node)
    
    def visit_For(self, node):
        self._update_depth(1)
        self.generic_visit(node)
        self._update_depth(-1)
    
    def visit_While(self, node):
        self._update_depth(1)
        self.generic_visit(node)
        self._update_depth(-1)
    
    def visit_If(self, node):
        self._update_depth(1)
        self.generic_visit(node)
        self._update_depth(-1)
    
    def _update_depth(self, delta: int):
        """更新嵌套深度"""
        self.stats['current_depth'] += delta
        self.stats['max_nested_depth'] = max(
            self.stats['max_nested_depth'], 
            self.stats['current_depth']
        )
    
    def get_validation_result(self) -> dict:
        """获取验证结果"""
        violations = []
        
        # 检查各项规则
        for rule, limit in self.rules.items():
            if rule in self.stats and self.stats[rule] > limit:
                violations.append(f"{rule}: {self.stats[rule]} > {limit}")
        
        # 计算复杂度分数
        complexity_score = (
            self.stats['function_count'] * 0.2 +
            self.stats['class_count'] * 0.3 +
            self.stats['max_nested_depth'] * 0.5
        )
        
        return {
            'valid': len(violations) == 0,
            'violations': violations,
            'stats': self.stats.copy(),
            'complexity_score': complexity_score
        } 