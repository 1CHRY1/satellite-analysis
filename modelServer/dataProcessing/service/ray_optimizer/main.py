"""
Ray优化器主入口模块

整合所有优化功能，提供统一的代码优化接口
"""

import time
import logging
from typing import Dict, Any, Tuple

from .analyzer import analyze_code
from .security import check_code_security, resource_limit, get_code_hash
from .optimizers.numpy_optimizer import NumPyOptimizer
from .wrapper import wrap_code_with_ray
from .exceptions import SecurityError, OptimizationError, ResourceLimitError

logger = logging.getLogger(__name__)


def optimize_user_code(
    code: str, 
    user_id: str = "unknown", 
    project_id: str = "unknown"
) -> Tuple[str, Dict[str, Any]]:
    """
    Ray优化器主入口函数
    """
    start_time = time.time()
    
    try:
        # 1. 安全检查
        with resource_limit():
            check_code_security(code)
        
        # 2. 代码分析
        analysis_result = analyze_code(code)
        
        # 3. 检查是否需要优化
        if not analysis_result.has_optimizable_features():
            basic_report = {
                'applied_optimizations': [],
                'estimated_speedup': 1.0,
                'ray_features_used': ['基础Ray环境'],
                'optimization_time': time.time() - start_time,
                'code_hash': get_code_hash(code),
                'recommendations': ['代码已经很高效，无需进一步优化']
            }
            wrapped_code = wrap_code_with_ray(
                code, user_id, project_id, basic_report, wrapper_type='basic'
            )
            return wrapped_code, basic_report
        
        # 4. 应用优化策略
        final_code = code
        all_optimizations = []
        total_speedup = 1.0
        ray_features = set()
        
        # NumPy优化
        if len(analysis_result.numpy_operations) > 0:
            numpy_optimizer = NumPyOptimizer()
            numpy_result = numpy_optimizer.optimize(final_code, analysis_result)
            final_code = numpy_result.optimized_code
            all_optimizations.extend(numpy_result.applied_optimizations)
            total_speedup *= numpy_result.estimated_speedup
            ray_features.update(numpy_result.ray_features_used)
        
        # 5. 生成最终报告
        final_report = {
            'applied_optimizations': all_optimizations,
            'estimated_speedup': min(total_speedup, 8.0),
            'ray_features_used': list(ray_features),
            'optimization_time': time.time() - start_time,
            'code_hash': get_code_hash(code),
            'analysis_summary': analysis_result.get_summary(),
            'recommendations': analysis_result.optimization_opportunities
        }
        
        # 6. Ray包装
        wrapped_code = wrap_code_with_ray(
            final_code, user_id, project_id, final_report
        )
        
        return wrapped_code, final_report
        
    except Exception as e:
        # 降级到基础包装
        error_report = {
            'applied_optimizations': [],
            'estimated_speedup': 1.0,
            'ray_features_used': ['错误恢复模式'],
            'optimization_time': time.time() - start_time,
            'error': str(e),
            'fallback': True
        }
        wrapped_code = wrap_code_with_ray(
            code, user_id, project_id, error_report, wrapper_type='basic'
        )
        return wrapped_code, error_report


def optimize_user_code_advanced(
    code: str, 
    user_id: str = "unknown", 
    project_id: str = "unknown"
) -> Tuple[str, Dict[str, Any]]:
    """高级Ray优化器入口（兼容性函数）"""
    return optimize_user_code(code, user_id, project_id)


def create_optimized_script(
    original_script_path: str, 
    user_id: str, 
    project_id: str
) -> Tuple[str, Dict[str, Any]]:
    """从文件创建优化脚本"""
    try:
        # 读取原始代码
        with open(original_script_path, 'r', encoding='utf-8') as f:
            original_code = f.read()
        
        # 优化代码
        optimized_code, report = optimize_user_code(original_code, user_id, project_id)
        
        # 创建优化脚本文件
        optimized_path = original_script_path.replace('.py', '_ray_optimized.py')
        with open(optimized_path, 'w', encoding='utf-8') as f:
            f.write(optimized_code)
        
        return optimized_path, report
        
    except Exception as e:
        return original_script_path, {'error': str(e)} 