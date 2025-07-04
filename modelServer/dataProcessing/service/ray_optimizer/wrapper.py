"""
Ray包装器模块 - 负责将优化后的代码包装在Ray执行环境中
"""

import json
import textwrap
from typing import Dict, Any

from .config import get_config


class RayWrapper:
    """Ray执行环境包装器"""
    
    def __init__(self):
        self.config = get_config()
    
    def wrap_with_ray_infrastructure(
        self, 
        code: str, 
        user_id: str, 
        project_id: str, 
        optimization_report: Dict[str, Any]
    ) -> str:
        """
        为代码添加完整的Ray基础设施
        
        Args:
            code: 优化后的代码
            user_id: 用户ID
            project_id: 项目ID
            optimization_report: 优化报告
            
        Returns:
            包装后的完整代码
        """
        
        infrastructure_code = f'''
import ray
import sys
import traceback
import time
from datetime import datetime
import json

# Ray集群配置
RAY_CONFIG = {json.dumps(self.config.RAY_CONFIG, indent=2)}

def init_ray_cluster():
    """初始化Ray集群"""
    try:
        # 尝试连接到现有集群
        ray.init(address='auto', ignore_reinit_error=True)
        print(f"[RAY] 连接到现有Ray集群成功")
        return True
    except:
        try:
            # 启动本地Ray节点
            ray.init(
                num_cpus=RAY_CONFIG.get('num_cpus'),
                object_store_memory=RAY_CONFIG.get('object_store_memory'),
                ignore_reinit_error=True
            )
            print(f"[RAY] 启动本地Ray节点成功")
            return True
        except Exception as e:
            print(f"[RAY] Ray初始化失败: {{e}}")
            return False

def print_optimization_report():
    """打印优化报告"""
    report = {json.dumps(optimization_report, indent=2, ensure_ascii=False)}
    print("\\n" + "="*50)
    print("Ray优化报告")
    print("="*50)
    if report.get('applied_optimizations'):
        print(f"应用的优化: {{', '.join(report['applied_optimizations'])}}")
        print(f"预估加速比: {{report.get('estimated_speedup', 1.0):.1f}}x")
        print(f"使用的Ray功能: {{', '.join(report.get('ray_features_used', []))}}")
    else:
        print("未应用特定优化，但代码运行在Ray环境中")
    print("="*50 + "\\n")

def print_ray_cluster_info():
    """打印Ray集群信息"""
    try:
        cluster_resources = ray.cluster_resources()
        print(f"[RAY] 集群资源: {{cluster_resources}}")
        
        # 打印节点信息
        nodes = ray.nodes()
        print(f"[RAY] 集群节点数: {{len(nodes)}}")
        
        for i, node in enumerate(nodes):
            print(f"[RAY] 节点 {{i+1}}: {{node.get('Resources', {{}})}}")
            
    except Exception as e:
        print(f"[RAY] 获取集群信息失败: {{e}}")

# 初始化Ray
ray_initialized = init_ray_cluster()
if ray_initialized:
    print_optimization_report()
    print_ray_cluster_info()

# 执行开始时间
start_time = datetime.now()
print(f"[RAY] 开始执行优化代码 (用户: {user_id}, 项目: {project_id})")
print(f"[RAY] 开始时间: {{start_time}}")

try:
    # ==================== 用户代码开始 ====================
{textwrap.indent(code, '    ')}
    # ==================== 用户代码结束 ====================
    
    print(f"[RAY] 代码执行成功完成")
    
except Exception as e:
    print(f"[RAY] 代码执行出错: {{e}}")
    traceback.print_exc()
    
finally:
    # 执行完成时间
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()
    
    print(f"[RAY] 执行完成时间: {{end_time}}")
    print(f"[RAY] 总执行时间: {{duration:.2f}}秒")
    
    # 显示性能统计
    if ray_initialized:
        try:
            print("\\n" + "="*30 + " Ray性能统计 " + "="*30)
            
            # 显示任务统计
            ray_stats = ray.experimental.state.summarize_tasks()
            if ray_stats:
                print(f"[RAY] 任务统计: {{ray_stats}}")
            
            # 显示对象存储统计
            object_store_stats = ray.experimental.state.summarize_objects()
            if object_store_stats:
                print(f"[RAY] 对象存储: {{object_store_stats}}")
                
        except Exception as e:
            print(f"[RAY] 获取性能统计失败: {{e}}")
    
    # 清理Ray资源
    try:
        if ray_initialized:
            ray.shutdown()
            print(f"[RAY] Ray资源已清理")
    except Exception as e:
        print(f"[RAY] Ray清理时出错: {{e}}")
'''
        
        return infrastructure_code
    
    def create_basic_wrapper(self, code: str, user_id: str, project_id: str) -> str:
        """
        创建基础Ray包装器（降级方案）
        
        当高级优化失败时使用的简化版本
        """
        wrapper = f'''
import ray
import traceback
from datetime import datetime

# 基础Ray初始化
try:
    ray.init(address='auto', ignore_reinit_error=True)
    print(f"[RAY] 基础模式启动成功 (用户: {user_id}, 项目: {project_id})")
    ray_available = True
except:
    print(f"[RAY] Ray不可用，使用标准执行模式")
    ray_available = False

start_time = datetime.now()
print(f"开始执行时间: {{start_time}}")

try:
{textwrap.indent(code, '    ')}
except Exception as e:
    print(f"执行出错: {{e}}")
    traceback.print_exc()
finally:
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()
    print(f"执行完成，耗时: {{duration:.2f}}秒")
    
    if ray_available:
        try:
            ray.shutdown()
        except:
            pass
'''
        return wrapper
    
    def create_monitoring_wrapper(self, code: str, user_id: str, project_id: str) -> str:
        """
        创建带监控的Ray包装器
        
        包含详细的性能监控和资源使用情况
        """
        wrapper = f'''
import ray
import traceback
import psutil
import time
from datetime import datetime

class RayMonitor:
    """Ray执行监控器"""
    
    def __init__(self):
        self.start_time = None
        self.ray_available = False
        self.metrics = {{}}
    
    def start_monitoring(self):
        """开始监控"""
        self.start_time = datetime.now()
        self.metrics['start_time'] = self.start_time
        self.metrics['initial_memory'] = psutil.virtual_memory().percent
        self.metrics['initial_cpu'] = psutil.cpu_percent()
        
        # 初始化Ray
        try:
            ray.init(address='auto', ignore_reinit_error=True)
            self.ray_available = True
            print(f"[RAY] 监控模式启动成功 (用户: {user_id}, 项目: {project_id})")
            
            # 记录Ray集群状态
            self.metrics['cluster_resources'] = ray.cluster_resources()
            
        except Exception as e:
            print(f"[RAY] Ray初始化失败: {{e}}")
            self.ray_available = False
    
    def record_checkpoint(self, name: str):
        """记录检查点"""
        current_time = datetime.now()
        self.metrics[f'checkpoint_{{name}}'] = {{
            'time': current_time,
            'elapsed': (current_time - self.start_time).total_seconds(),
            'memory': psutil.virtual_memory().percent,
            'cpu': psutil.cpu_percent()
        }}
    
    def finish_monitoring(self):
        """完成监控"""
        end_time = datetime.now()
        duration = (end_time - self.start_time).total_seconds()
        
        self.metrics['end_time'] = end_time
        self.metrics['total_duration'] = duration
        self.metrics['final_memory'] = psutil.virtual_memory().percent
        self.metrics['final_cpu'] = psutil.cpu_percent()
        
        # 打印监控报告
        self.print_monitoring_report()
        
        # 清理Ray
        if self.ray_available:
            try:
                ray.shutdown()
                print(f"[RAY] Ray资源已清理")
            except Exception as e:
                print(f"[RAY] Ray清理时出错: {{e}}")
    
    def print_monitoring_report(self):
        """打印监控报告"""
        print("\\n" + "="*60)
        print("Ray执行监控报告")
        print("="*60)
        print(f"用户ID: {user_id}")
        print(f"项目ID: {project_id}")
        print(f"总执行时间: {{self.metrics['total_duration']:.2f}}秒")
        print(f"内存使用: {{self.metrics['initial_memory']:.1f}}% → {{self.metrics['final_memory']:.1f}}%")
        print(f"CPU使用: {{self.metrics['initial_cpu']:.1f}}% → {{self.metrics['final_cpu']:.1f}}%")
        
        if self.ray_available and 'cluster_resources' in self.metrics:
            print(f"Ray集群资源: {{self.metrics['cluster_resources']}}")
        
        # 显示检查点信息
        checkpoints = [k for k in self.metrics.keys() if k.startswith('checkpoint_')]
        if checkpoints:
            print("\\n执行检查点:")
            for checkpoint in sorted(checkpoints):
                cp_data = self.metrics[checkpoint]
                print(f"  {{checkpoint.replace('checkpoint_', '')}}: "
                      f"{{cp_data['elapsed']:.2f}}s, "
                      f"内存{{cp_data['memory']:.1f}}%, "
                      f"CPU{{cp_data['cpu']:.1f}}%")
        
        print("="*60)

# 启动监控
monitor = RayMonitor()
monitor.start_monitoring()

try:
    monitor.record_checkpoint('code_start')
{textwrap.indent(code, '    ')}
    monitor.record_checkpoint('code_end')
    print(f"[RAY] 代码执行成功完成")
    
except Exception as e:
    monitor.record_checkpoint('error')
    print(f"[RAY] 代码执行出错: {{e}}")
    traceback.print_exc()
    
finally:
    monitor.finish_monitoring()
'''
        return wrapper


# 全局包装器实例
ray_wrapper = RayWrapper()


def wrap_code_with_ray(code: str, user_id: str, project_id: str, 
                      optimization_report: Dict[str, Any], 
                      wrapper_type: str = 'standard') -> str:
    """
    使用Ray包装代码的便捷函数
    
    Args:
        code: 要包装的代码
        user_id: 用户ID
        project_id: 项目ID
        optimization_report: 优化报告
        wrapper_type: 包装器类型 ('standard', 'basic', 'monitoring')
    
    Returns:
        包装后的代码
    """
    if wrapper_type == 'basic':
        return ray_wrapper.create_basic_wrapper(code, user_id, project_id)
    elif wrapper_type == 'monitoring':
        return ray_wrapper.create_monitoring_wrapper(code, user_id, project_id)
    else:
        return ray_wrapper.wrap_with_ray_infrastructure(
            code, user_id, project_id, optimization_report
        ) 