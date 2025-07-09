"""
代码模式检测器
检测可优化的代码模式
"""
import ast
import logging
from typing import Dict, Any, List
from ..interfaces.analyzer import IPatternDetector

logger = logging.getLogger(__name__)

class PatternDetector(IPatternDetector):
    """代码模式检测器实现"""
    
    def __init__(self):
        self.patterns = []
        
    def detect_patterns(self, ast_node: Any) -> List[Dict[str, Any]]:
        """检测代码中的可优化模式"""
        self.patterns = []
        
        # 使用访问器模式遍历AST
        visitor = PatternVisitor(self.patterns)
        visitor.visit(ast_node)
        
        return self.patterns


class PatternVisitor(ast.NodeVisitor):
    """模式检测访问器"""
    
    def __init__(self, patterns: List[Dict[str, Any]]):
        self.patterns = patterns
        
    def visit_For(self, node: ast.For) -> None:
        """检测For循环模式"""
        
        # 模式1: 简单的map操作
        if self._is_map_pattern(node):
            self.patterns.append({
                'type': 'map_pattern',
                'node': node,
                'description': 'Simple map operation that can be parallelized',
                'optimization': 'ray_parallel_map'
            })
            
        # 模式2: 累积操作
        elif self._is_accumulation_pattern(node):
            self.patterns.append({
                'type': 'accumulation_pattern',
                'node': node,
                'description': 'Accumulation that might benefit from reduction',
                'optimization': 'ray_reduce'
            })
            
        # 模式3: 嵌套循环
        elif self._has_nested_loops(node):
            self.patterns.append({
                'type': 'nested_loop_pattern',
                'node': node,
                'description': 'Nested loops that can be flattened or parallelized',
                'optimization': 'ray_nested_parallel'
            })
            
        self.generic_visit(node)
        
    def visit_ListComp(self, node: ast.ListComp) -> None:
        """检测列表推导式模式"""
        self.patterns.append({
            'type': 'list_comprehension',
            'node': node,
            'description': 'List comprehension that can be parallelized',
            'optimization': 'ray_parallel_listcomp'
        })
        self.generic_visit(node)
        
    def visit_Call(self, node: ast.Call) -> None:
        """检测函数调用模式"""
        func_name = self._get_func_name(node.func)
        
        # 检测数据处理模式
        if func_name and 'apply' in func_name:
            self.patterns.append({
                'type': 'apply_pattern',
                'node': node,
                'description': 'Apply operation that can be parallelized',
                'optimization': 'ray_parallel_apply'
            })
            
        # 检测I/O密集型操作
        elif func_name in ['open', 'requests.get', 'urllib.request.urlopen']:
            self.patterns.append({
                'type': 'io_pattern',
                'node': node,
                'description': 'I/O operation that can be made asynchronous',
                'optimization': 'ray_async_io'
            })
            
        self.generic_visit(node)
        
    def _is_map_pattern(self, node: ast.For) -> bool:
        """检查是否是map模式"""
        # 简单的启发式：循环体只包含一个赋值或append操作
        if len(node.body) == 1:
            stmt = node.body[0]
            
            # 检查是否是列表append
            if (isinstance(stmt, ast.Expr) and 
                isinstance(stmt.value, ast.Call) and
                isinstance(stmt.value.func, ast.Attribute) and
                stmt.value.func.attr == 'append'):
                return True
                
            # 检查是否是简单赋值
            if isinstance(stmt, ast.Assign):
                return True
                
        return False
        
    def _is_accumulation_pattern(self, node: ast.For) -> bool:
        """检查是否是累积模式"""
        for stmt in node.body:
            if isinstance(stmt, ast.AugAssign):  # += 操作
                return True
            if isinstance(stmt, ast.Assign):
                # 检查是否是 x = x + ... 模式
                for target in stmt.targets:
                    if (isinstance(target, ast.Name) and 
                        isinstance(stmt.value, ast.BinOp) and
                        isinstance(stmt.value.left, ast.Name) and
                        stmt.value.left.id == target.id):
                        return True
        return False
        
    def _has_nested_loops(self, node: ast.For) -> bool:
        """检查是否有嵌套循环"""
        for stmt in node.body:
            if isinstance(stmt, (ast.For, ast.While)):
                return True
        return False
        
    def _get_func_name(self, node: ast.AST) -> str:
        """获取函数名"""
        if isinstance(node, ast.Name):
            return node.id
        elif isinstance(node, ast.Attribute):
            return node.attr
        return "" 