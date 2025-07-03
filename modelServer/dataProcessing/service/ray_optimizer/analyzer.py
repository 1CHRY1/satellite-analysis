"""
Ray优化器代码分析模块

提供智能代码分析、模式识别和优化机会检测功能
"""

import ast
import logging
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field

from .exceptions import CodeAnalysisError

logger = logging.getLogger(__name__)


@dataclass
class CodeAnalysisResult:
    """代码分析结果"""
    
    # 基础统计
    imports: List[str] = field(default_factory=list)
    functions: List[Dict[str, Any]] = field(default_factory=list)
    classes: List[Dict[str, Any]] = field(default_factory=list)
    
    # 操作检测
    numpy_operations: List[Dict[str, Any]] = field(default_factory=list)
    pandas_operations: List[Dict[str, Any]] = field(default_factory=list)
    loops: List[Dict[str, Any]] = field(default_factory=list)
    file_operations: List[Dict[str, Any]] = field(default_factory=list)
    image_operations: List[Dict[str, Any]] = field(default_factory=list)
    
    # 复杂度指标
    complexity_score: float = 0.0
    parallel_potential: float = 0.0
    optimization_opportunities: List[str] = field(default_factory=list)
    
    # 性能预估
    estimated_execution_time: float = 0.0
    bottlenecks: List[Dict[str, Any]] = field(default_factory=list)
    
    def has_optimizable_features(self) -> bool:
        """检查是否有可优化的特征"""
        return (len(self.numpy_operations) > 0 or 
                len(self.pandas_operations) > 0 or
                len(self.loops) > 0 or
                len(self.file_operations) > 0 or
                len(self.image_operations) > 0)
    
    def get_summary(self) -> Dict[str, Any]:
        """获取分析结果摘要"""
        return {
            'total_operations': (
                len(self.numpy_operations) + 
                len(self.pandas_operations) + 
                len(self.file_operations) + 
                len(self.image_operations)
            ),
            'loop_count': len(self.loops),
            'function_count': len(self.functions),
            'class_count': len(self.classes),
            'import_count': len(self.imports),
            'complexity_score': self.complexity_score,
            'parallel_potential': self.parallel_potential,
            'has_optimization_potential': self.has_optimizable_features(),
            'optimization_opportunities': len(self.optimization_opportunities)
        }


def analyze_code(code: str) -> CodeAnalysisResult:
    """分析代码的便捷函数"""
    analyzer = CodeAnalyzer()
    return analyzer.analyze(code)


class CodeAnalyzer(ast.NodeVisitor):
    """智能代码分析器"""
    
    def __init__(self):
        self.result = CodeAnalysisResult()
        self.in_loop = False
        self.loop_depth = 0
        self.function_depth = 0
    
    def analyze(self, code: str) -> CodeAnalysisResult:
        """分析代码并返回结果"""
        try:
            tree = ast.parse(code)
            self.visit(tree)
            self._finalize_analysis()
            return self.result
        except SyntaxError as e:
            raise CodeAnalysisError(f"Code syntax error: {e}")
        except Exception as e:
            raise CodeAnalysisError(f"Code analysis failed: {e}")
    
    def _finalize_analysis(self):
        """完成分析，计算最终指标"""
        self._calculate_complexity_score()
        self._calculate_parallel_potential()
        self._identify_optimization_opportunities()
    
    def visit_Import(self, node):
        """分析import语句"""
        for alias in node.names:
            self.result.imports.append(alias.name)
        self.generic_visit(node)
    
    def visit_ImportFrom(self, node):
        """分析from ... import语句"""
        if node.module:
            self.result.imports.append(node.module)
        self.generic_visit(node)
    
    def visit_FunctionDef(self, node):
        """分析函数定义"""
        self.function_depth += 1
        function_info = {
            'name': node.name,
            'line': node.lineno,
            'args': len(node.args.args),
            'complexity': self._estimate_function_complexity(node)
        }
        self.result.functions.append(function_info)
        self.generic_visit(node)
        self.function_depth -= 1
    
    def visit_ClassDef(self, node):
        """分析类定义"""
        class_info = {
            'name': node.name,
            'line': node.lineno,
            'methods': len([n for n in node.body if isinstance(n, ast.FunctionDef)])
        }
        self.result.classes.append(class_info)
        self.generic_visit(node)
    
    def visit_For(self, node):
        """分析for循环"""
        self.loop_depth += 1
        old_in_loop = self.in_loop
        self.in_loop = True
        
        loop_info = {
            'type': 'for',
            'line': node.lineno,
            'depth': self.loop_depth,
            'nested': old_in_loop,
            'parallelizable': self._check_loop_parallelizable(node)
        }
        self.result.loops.append(loop_info)
        
        self.generic_visit(node)
        
        self.in_loop = old_in_loop
        self.loop_depth -= 1
    
    def visit_While(self, node):
        """分析while循环"""
        self.loop_depth += 1
        old_in_loop = self.in_loop
        self.in_loop = True
        
        loop_info = {
            'type': 'while',
            'line': node.lineno,
            'depth': self.loop_depth,
            'nested': old_in_loop,
            'parallelizable': False
        }
        self.result.loops.append(loop_info)
        
        self.generic_visit(node)
        
        self.in_loop = old_in_loop
        self.loop_depth -= 1
    
    def visit_Call(self, node):
        """分析函数调用"""
        if self._is_numpy_call(node):
            op_info = {
                'function': self._get_function_name(node),
                'line': node.lineno,
                'in_loop': self.in_loop
            }
            self.result.numpy_operations.append(op_info)
        
        elif self._is_pandas_call(node):
            op_info = {
                'function': self._get_function_name(node),
                'line': node.lineno,
                'in_loop': self.in_loop
            }
            self.result.pandas_operations.append(op_info)
        
        elif self._is_file_operation(node):
            op_info = {
                'function': self._get_function_name(node),
                'line': node.lineno
            }
            self.result.file_operations.append(op_info)
        
        elif self._is_image_operation(node):
            op_info = {
                'function': self._get_function_name(node),
                'line': node.lineno
            }
            self.result.image_operations.append(op_info)
        
        self.generic_visit(node)
    
    def _is_numpy_call(self, node) -> bool:
        """检查是否为NumPy调用"""
        func_name = self._get_function_name(node)
        numpy_patterns = [
            'np.', 'numpy.', '.sum(', '.mean(', '.std(', '.var(',
            '.min(', '.max(', '.dot(', '.matmul(', '.reshape(',
            '.transpose(', '.flatten(', '.ravel('
        ]
        return any(pattern in func_name for pattern in numpy_patterns)
    
    def _is_pandas_call(self, node) -> bool:
        """检查是否为Pandas调用"""
        func_name = self._get_function_name(node)
        pandas_patterns = [
            'pd.', 'pandas.', '.apply(', '.groupby(', '.merge(',
            '.join(', '.concat(', '.pivot(', '.melt(', '.agg('
        ]
        return any(pattern in func_name for pattern in pandas_patterns)
    
    def _is_file_operation(self, node) -> bool:
        """检查是否为文件操作"""
        func_name = self._get_function_name(node)
        file_patterns = [
            'read_csv', 'to_csv', 'read_excel', 'to_excel',
            'read_json', 'to_json', 'read_hdf', 'to_hdf',
            'glob.glob', 'os.listdir'
        ]
        return any(pattern in func_name for pattern in file_patterns)
    
    def _is_image_operation(self, node) -> bool:
        """检查是否为图像操作"""
        func_name = self._get_function_name(node)
        image_patterns = [
            'cv2.', 'PIL.', 'skimage.', 'imread', 'imwrite', 
            'resize', 'rotate', 'filter', 'transform'
        ]
        return any(pattern in func_name for pattern in image_patterns)
    
    def _get_function_name(self, node) -> str:
        """获取函数调用的完整名称"""
        try:
            if isinstance(node.func, ast.Name):
                return node.func.id
            elif isinstance(node.func, ast.Attribute):
                return f"obj.{node.func.attr}"
            else:
                return str(node.func)
        except:
            return "unknown"
    
    def _check_loop_parallelizable(self, node) -> bool:
        """检查循环是否可并行化"""
        has_dependencies = False
        for stmt in node.body:
            if isinstance(stmt, ast.Assign):
                for target in stmt.targets:
                    if isinstance(target, ast.Subscript):
                        has_dependencies = True
                        break
        return not has_dependencies
    
    def _estimate_function_complexity(self, node) -> int:
        """估算函数复杂度"""
        complexity = 1
        for child in ast.walk(node):
            if isinstance(child, (ast.If, ast.For, ast.While)):
                complexity += 1
        return complexity
    
    def _calculate_complexity_score(self):
        """计算复杂度分数"""
        score = 0.0
        score += len(self.result.numpy_operations) * 2
        score += len(self.result.pandas_operations) * 3
        score += len(self.result.loops) * 1.5
        score += len(self.result.file_operations) * 2
        score += len(self.result.image_operations) * 2
        
        for func in self.result.functions:
            score += func['complexity'] * 0.5
        
        self.result.complexity_score = score
    
    def _calculate_parallel_potential(self):
        """计算并行化潜力"""
        potential = 0.0
        parallelizable_loops = [loop for loop in self.result.loops if loop['parallelizable']]
        potential += len(parallelizable_loops) * 2
        potential += len(self.result.numpy_operations) * 1.5
        potential += len(self.result.pandas_operations) * 2
        potential += len(self.result.file_operations) * 3
        potential += len(self.result.image_operations) * 2
        
        self.result.parallel_potential = min(10, potential)
    
    def _identify_optimization_opportunities(self):
        """识别优化机会"""
        opportunities = []
        
        if self.result.numpy_operations:
            opportunities.append(f"NumPy并行化 ({len(self.result.numpy_operations)} 个操作)")
        
        if self.result.pandas_operations:
            opportunities.append(f"Pandas并行化 ({len(self.result.pandas_operations)} 个操作)")
        
        parallelizable_loops = [loop for loop in self.result.loops if loop['parallelizable']]
        if parallelizable_loops:
            opportunities.append(f"循环并行化 ({len(parallelizable_loops)} 个循环)")
        
        if len(self.result.file_operations) > 5:
            opportunities.append(f"批量文件处理 ({len(self.result.file_operations)} 个文件)")
        
        if self.result.image_operations:
            opportunities.append(f"图像处理并行化 ({len(self.result.image_operations)} 个操作)")
        
        self.result.optimization_opportunities = opportunities 