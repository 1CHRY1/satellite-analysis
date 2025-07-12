"""
代码复杂度分析器
分析代码的计算复杂度和并行化潜力
"""
import ast
import logging
from typing import Dict, Any, List, Optional
from ..interfaces.analyzer import ICodeAnalyzer

logger = logging.getLogger(__name__)

class ComplexityAnalyzer(ICodeAnalyzer):
    """代码复杂度分析器实现"""
    
    def __init__(self):
        self.metrics = None
        self.ast_tree = None
        
    def analyze(self, code: str) -> Dict[str, Any]:
        """分析代码并返回分析结果"""
        try:
            # 解析代码为AST
            self.ast_tree = ast.parse(code)
            
            # 初始化度量指标
            self.metrics = {
                'loop_depth': 0,
                'loop_count': 0,
                'array_operations': 0,
                'function_calls': 0,
                'io_operations': 0,
                'computational_density': 0,
                'data_dependencies': 0,
                'parallelizable_loops': 0,
                'total_lines': len(code.splitlines()),
                'numpy_operations': 0,
                'pandas_operations': 0,
                'list_comprehensions': 0,
                'generator_expressions': 0,
                'estimated_data_size': 0,
            }
            
            # 遍历AST收集度量
            visitor = ComplexityVisitor(self.metrics)
            visitor.visit(self.ast_tree)
            
            # 计算复杂度分数
            complexity_score = self.calculate_complexity_score(self.metrics)
            
            return {
                'complexity_score': complexity_score,
                'parallelizable': self.metrics['parallelizable_loops'] > 0,
                'metrics': self.metrics,
                'ast_tree': self.ast_tree
            }
            
        except SyntaxError as e:
            logger.error(f"Syntax error in code: {e}")
            return {
                'complexity_score': 0,
                'parallelizable': False,
                'metrics': {},
                'error': str(e)
            }
        except Exception as e:
            logger.error(f"Error analyzing code: {e}")
            return {
                'complexity_score': 0,
                'parallelizable': False,
                'metrics': {},
                'error': str(e)
            }
    
    def calculate_complexity_score(self, metrics: Dict[str, Any]) -> float:
        """根据度量指标计算复杂度分数"""
        score = 0.0
        
        # 循环复杂度 (权重30%)
        loop_score = min(metrics['loop_count'] * 5 + metrics['loop_depth'] * 10, 30)
        score += loop_score
        
        # 计算密度 (权重25%)
        if metrics['total_lines'] > 0:
            density = (metrics['array_operations'] + metrics['numpy_operations'] + 
                      metrics['pandas_operations']) / metrics['total_lines']
            density_score = min(density * 50, 25)
            score += density_score
        
        # 并行化潜力 (权重25%)
        parallel_score = min(metrics['parallelizable_loops'] * 8 + 
                           metrics['list_comprehensions'] * 3, 25)
        score += parallel_score
        
        # 数据规模估计 (权重20%)
        data_score = self._estimate_data_scale_score(metrics)
        score += data_score
        
        return min(score, 100)
    
    def _estimate_data_scale_score(self, metrics: Dict[str, Any]) -> float:
        """估计数据规模分数"""
        # 基于循环、数组操作等估计数据规模
        estimated_operations = (
            metrics['loop_count'] * metrics['array_operations'] +
            metrics['numpy_operations'] * 10 +
            metrics['pandas_operations'] * 20
        )
        
        # 对数缩放
        import math
        if estimated_operations > 0:
            return min(math.log10(estimated_operations + 1) * 5, 20)
        return 0


class ComplexityVisitor(ast.NodeVisitor):
    """AST访问器，收集复杂度度量"""
    
    def __init__(self, metrics: Dict[str, Any]):
        self.metrics = metrics
        self.current_loop_depth = 0
        self.in_loop = False
        
    def visit_For(self, node: ast.For) -> None:
        """访问For循环"""
        self.metrics['loop_count'] += 1
        self.current_loop_depth += 1
        self.metrics['loop_depth'] = max(self.metrics['loop_depth'], 
                                        self.current_loop_depth)
        
        # 检查是否可并行化
        if self._is_parallelizable_loop(node):
            self.metrics['parallelizable_loops'] += 1
        
        old_in_loop = self.in_loop
        self.in_loop = True
        self.generic_visit(node)
        self.in_loop = old_in_loop
        self.current_loop_depth -= 1
        
    def visit_While(self, node: ast.While) -> None:
        """访问While循环"""
        self.metrics['loop_count'] += 1
        self.current_loop_depth += 1
        self.metrics['loop_depth'] = max(self.metrics['loop_depth'], 
                                        self.current_loop_depth)
        
        old_in_loop = self.in_loop
        self.in_loop = True
        self.generic_visit(node)
        self.in_loop = old_in_loop
        self.current_loop_depth -= 1
        
    def visit_ListComp(self, node: ast.ListComp) -> None:
        """访问列表推导式"""
        self.metrics['list_comprehensions'] += 1
        self.metrics['parallelizable_loops'] += 0.5  # 列表推导式易于并行化
        self.generic_visit(node)
        
    def visit_GeneratorExp(self, node: ast.GeneratorExp) -> None:
        """访问生成器表达式"""
        self.metrics['generator_expressions'] += 1
        self.generic_visit(node)
        
    def visit_Call(self, node: ast.Call) -> None:
        """访问函数调用"""
        self.metrics['function_calls'] += 1
        
        # 检查特定的函数调用
        func_name = self._get_func_name(node.func)
        
        # NumPy操作
        if func_name and ('numpy' in func_name or 'np.' in func_name):
            self.metrics['numpy_operations'] += 1
            if self.in_loop:
                self.metrics['computational_density'] += 2
                
        # Pandas操作
        elif func_name and ('pandas' in func_name or 'pd.' in func_name):
            self.metrics['pandas_operations'] += 1
            if self.in_loop:
                self.metrics['computational_density'] += 3
                
        # I/O操作
        elif func_name in ['open', 'read', 'write', 'print']:
            self.metrics['io_operations'] += 1
            
        self.generic_visit(node)
        
    def visit_Subscript(self, node: ast.Subscript) -> None:
        """访问数组下标操作"""
        self.metrics['array_operations'] += 1
        if self.in_loop:
            self.metrics['computational_density'] += 1
        self.generic_visit(node)
        
    def _is_parallelizable_loop(self, node: ast.For) -> bool:
        """检查循环是否可并行化"""
        # 简化的并行化检测
        # 检查循环体中是否有依赖
        
        # 获取循环变量
        if isinstance(node.target, ast.Name):
            loop_var = node.target.id
        else:
            return False
            
        # 检查循环体中的赋值操作
        for stmt in node.body:
            if isinstance(stmt, ast.Assign):
                # 检查是否有循环间依赖
                # 这是一个简化的检查
                return True
                
        return True
        
    def _get_func_name(self, node: ast.AST) -> Optional[str]:
        """获取函数名"""
        if isinstance(node, ast.Name):
            return node.id
        elif isinstance(node, ast.Attribute):
            value_name = self._get_func_name(node.value)
            if value_name:
                return f"{value_name}.{node.attr}"
            return node.attr
        return None 