"""
Ray代码优化器核心模块
"""

import json
import time
import ast
import logging

logger = logging.getLogger(__name__)

def optimize_code(code_content, cpu_count=4, memory_mb=2048, analysis_mode="auto", timeout=60):
    """
    使用Ray对Python代码进行并行优化
    
    Args:
        code_content (str): 原始Python代码
        cpu_count (int): Ray使用的CPU数量
        memory_mb (int): Ray使用的内存量(MB)
        analysis_mode (str): 分析模式 (auto/conservative/aggressive)
        timeout (int): 超时时间(秒)
    
    Returns:
        dict: 优化结果
    """
    start_time = time.time()
    
    try:
        logger.info(f"Starting Ray optimization with {cpu_count} CPUs, {memory_mb}MB memory")
        
        # 检查代码是否适合优化
        if not _is_code_optimizable(code_content):
            return {
                "success": False,
                "error": "Code does not contain patterns suitable for Ray optimization",
                "analysis_time": time.time() - start_time
            }
        
        # 尝试导入Ray
        try:
            import ray
        except ImportError:
            return {
                "success": False,
                "error": "Ray is not installed. Please install ray: pip install ray",
                "analysis_time": time.time() - start_time
            }
        
        # 应用优化
        optimized_code = _apply_ray_optimization(code_content, analysis_mode)
        
        if optimized_code and optimized_code != code_content:
            # 估算加速比
            speedup = _estimate_speedup(code_content, cpu_count)
            
            result = {
                "success": True,
                "optimized_code": optimized_code,
                "speedup_factor": speedup,
                "analysis_time": time.time() - start_time,
                "optimization_type": "ray_parallel",
                "parallel_opportunities": _count_parallel_opportunities(code_content),
                "estimated_memory_usage": memory_mb * 0.8  # 估算使用80%的分配内存
            }
            
            logger.info(f"Optimization successful, estimated speedup: {speedup:.1f}x")
            return result
        else:
            return {
                "success": False,
                "error": "No optimization opportunities found",
                "analysis_time": time.time() - start_time
            }
            
    except Exception as e:
        logger.error(f"Ray optimization failed: {str(e)}")
        return {
            "success": False,
            "error": f"Optimization failed: {str(e)}",
            "analysis_time": time.time() - start_time
        }

def _is_code_optimizable(code):
    """检查代码是否包含可优化的模式"""
    if not code or not code.strip():
        return False
    
    code_lower = code.lower()
    
    # 检查是否包含循环
    has_loops = any(pattern in code_lower for pattern in ["for ", "while "])
    
    # 检查是否包含数组操作
    has_array_ops = any(pattern in code_lower for pattern in ["numpy", "pandas", "list(", "[", "range("])
    
    # 检查是否包含计算密集型操作
    has_compute_ops = any(pattern in code_lower for pattern in ["map(", "filter(", "sum(", "max(", "min("])
    
    return has_loops or has_array_ops or has_compute_ops

def _apply_ray_optimization(code, mode="auto"):
    """应用Ray优化"""
    try:
        # 解析AST
        tree = ast.parse(code)
        
        # 查找可优化的循环
        optimized_code = code
        
        # 简单的示例优化：将for循环转换为Ray parallel map
        if "for " in code and "range(" in code:
            # 在代码开头添加Ray初始化
            ray_init = "import ray\nray.init()\n\n"
            
            # 添加Ray装饰器到函数
            if "def " in code:
                optimized_code = ray_init + _add_ray_decorators(code)
            else:
                # 如果没有函数，包装成Ray任务
                optimized_code = ray_init + _wrap_with_ray_tasks(code)
        
        return optimized_code
        
    except Exception as e:
        logger.warning(f"Failed to apply optimization: {str(e)}")
        return code

def _add_ray_decorators(code):
    """为函数添加Ray装饰器"""
    lines = code.split('\n')
    optimized_lines = []
    
    for line in lines:
        if line.strip().startswith('def ') and not line.strip().startswith('def main'):
            optimized_lines.append('@ray.remote')
            optimized_lines.append(line)
        else:
            optimized_lines.append(line)
    
    return '\n'.join(optimized_lines)

def _wrap_with_ray_tasks(code):
    """将代码包装为Ray任务"""
    wrapper = f"""
@ray.remote
def parallel_task(data_chunk):
    # 原始代码的并行版本
{_indent_code(code, 4)}
    return result

# Ray并行执行
if __name__ == "__main__":
    # 数据分片并行处理
    futures = [parallel_task.remote(chunk) for chunk in data_chunks]
    results = ray.get(futures)
    ray.shutdown()
"""
    return wrapper

def _indent_code(code, spaces):
    """为代码添加缩进"""
    lines = code.split('\n')
    indent = ' ' * spaces
    return '\n'.join([indent + line if line.strip() else line for line in lines])

def _estimate_speedup(code, cpu_count):
    """估算加速比"""
    # 简单的启发式估算
    loop_count = code.lower().count('for ')
    parallel_ops = code.lower().count('map(') + code.lower().count('filter(')
    
    if loop_count > 0 or parallel_ops > 0:
        # 假设并行效率为60-80%
        efficiency = 0.7
        theoretical_speedup = min(cpu_count, loop_count + parallel_ops + 1)
        return theoretical_speedup * efficiency
    
    return 1.0

def _count_parallel_opportunities(code):
    """统计并行机会数量"""
    opportunities = 0
    code_lower = code.lower()
    
    opportunities += code_lower.count('for ')
    opportunities += code_lower.count('map(')
    opportunities += code_lower.count('filter(')
    opportunities += code_lower.count('list(')
    
    return opportunities 