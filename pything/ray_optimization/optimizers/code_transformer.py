"""
代码转换器
将普通Python代码转换为Ray优化代码
"""
import ast
import logging
from typing import Dict, Any, List, Optional

logger = logging.getLogger(__name__)

class CodeTransformer:
    """代码转换器，实现各种优化转换"""
    
    def __init__(self):
        self.ray_imported = False
        self.remote_functions = {}
        
    def transform(self, code: str, patterns: List[Dict[str, Any]]) -> str:
        """
        根据检测到的模式转换代码
        
        Args:
            code: 原始代码
            patterns: 检测到的可优化模式
            
        Returns:
            转换后的代码
        """
        try:
            # 解析代码
            tree = ast.parse(code)
            
            # 应用转换
            transformer = RayTransformer(patterns)
            transformed_tree = transformer.visit(tree)
            
            # 添加必要的导入
            if transformer.needs_ray:
                transformed_tree = self._add_ray_imports(transformed_tree)
                
            # 生成代码
            import astor
            optimized_code = astor.to_source(transformed_tree)
            
            return optimized_code
            
        except Exception as e:
            logger.error(f"Error transforming code: {e}")
            # 返回原始代码
            return code
            
    def _add_ray_imports(self, tree: ast.Module) -> ast.Module:
        """添加Ray相关的导入语句"""
        ray_import = ast.Import(names=[ast.alias(name='ray', asname=None)])
        
        # 找到插入位置（在其他导入之后）
        insert_pos = 0
        for i, node in enumerate(tree.body):
            if isinstance(node, (ast.Import, ast.ImportFrom)):
                insert_pos = i + 1
            else:
                break
                
        tree.body.insert(insert_pos, ray_import)
        return tree


class RayTransformer(ast.NodeTransformer):
    """AST转换器，应用Ray优化"""
    
    def __init__(self, patterns: List[Dict[str, Any]]):
        self.patterns = patterns
        self.needs_ray = False
        self.pattern_map = {pattern['node']: pattern for pattern in patterns}
        
    def visit_For(self, node: ast.For) -> ast.AST:
        """转换For循环"""
        # 检查是否有对应的优化模式
        if node in self.pattern_map:
            pattern = self.pattern_map[node]
            optimization = pattern['optimization']
            
            if optimization == 'ray_parallel_map':
                return self._transform_map_pattern(node)
            elif optimization == 'ray_reduce':
                return self._transform_reduce_pattern(node)
            elif optimization == 'ray_nested_parallel':
                return self._transform_nested_pattern(node)
                
        # 继续访问子节点
        self.generic_visit(node)
        return node
        
    def visit_ListComp(self, node: ast.ListComp) -> ast.AST:
        """转换列表推导式"""
        if node in self.pattern_map:
            pattern = self.pattern_map[node]
            if pattern['optimization'] == 'ray_parallel_listcomp':
                return self._transform_listcomp_pattern(node)
                
        return node
        
    def _transform_map_pattern(self, node: ast.For) -> ast.AST:
        """转换简单的map模式为Ray并行"""
        self.needs_ray = True
        
        # 提取循环信息
        target = node.target
        iter_expr = node.iter
        body = node.body
        
        # 创建remote函数
        func_name = f"_ray_map_func_{id(node)}"
        
        # 函数参数
        args = ast.arguments(
            args=[ast.arg(arg=target.id, annotation=None)],
            posonlyargs=[],
            kwonlyargs=[],
            kw_defaults=[],
            defaults=[]
        )
        
        # 函数体
        func_body = body.copy()
        
        # 创建函数定义
        func_def = ast.FunctionDef(
            name=func_name,
            args=args,
            body=func_body,
            decorator_list=[ast.Attribute(
                value=ast.Name(id='ray', ctx=ast.Load()),
                attr='remote',
                ctx=ast.Load()
            )],
            returns=None
        )
        
        # 创建并行调用
        # futures = [func.remote(item) for item in items]
        futures_comp = ast.ListComp(
            elt=ast.Call(
                func=ast.Attribute(
                    value=ast.Name(id=func_name, ctx=ast.Load()),
                    attr='remote',
                    ctx=ast.Load()
                ),
                args=[ast.Name(id='item', ctx=ast.Load())],
                keywords=[]
            ),
            generators=[ast.comprehension(
                target=ast.Name(id='item', ctx=ast.Store()),
                iter=iter_expr,
                ifs=[],
                is_async=0
            )]
        )
        
        futures_assign = ast.Assign(
            targets=[ast.Name(id='_ray_futures', ctx=ast.Store())],
            value=futures_comp
        )
        
        # results = ray.get(futures)
        results_assign = ast.Assign(
            targets=[ast.Name(id='_ray_results', ctx=ast.Store())],
            value=ast.Call(
                func=ast.Attribute(
                    value=ast.Name(id='ray', ctx=ast.Load()),
                    attr='get',
                    ctx=ast.Load()
                ),
                args=[ast.Name(id='_ray_futures', ctx=ast.Load())],
                keywords=[]
            )
        )
        
        # 返回转换后的代码块
        return [func_def, futures_assign, results_assign]
        
    def _transform_reduce_pattern(self, node: ast.For) -> ast.AST:
        """转换累积模式"""
        # TODO: 实现reduce模式的转换
        logger.debug("Reduce pattern transformation not yet implemented")
        return node
        
    def _transform_nested_pattern(self, node: ast.For) -> ast.AST:
        """转换嵌套循环模式"""
        # TODO: 实现嵌套循环的转换
        logger.debug("Nested loop pattern transformation not yet implemented")
        return node
        
    def _transform_listcomp_pattern(self, node: ast.ListComp) -> ast.AST:
        """转换列表推导式为Ray并行"""
        self.needs_ray = True
        
        # 简化实现：转换为普通for循环然后应用map转换
        # TODO: 实现更优雅的列表推导式转换
        logger.debug("List comprehension transformation not yet implemented")
        return node 