# dataProcessing/model/test_task.py
"""简单的测试任务，用于验证调度器功能"""

import time
from dataProcessing.model.task import Task

class TestTask(Task):
    """简单的测试任务"""
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.config = args[0] if args else kwargs.get('config', {})
        print(f"[TestTask] 初始化任务 {task_id}，配置: {self.config}")
    
    def run(self):
        """执行测试任务"""
        print(f"[TestTask] 任务 {self.task_id} 开始执行")
        
        try:
            # 模拟一些工作
            duration = self.config.get('duration', 5)
            print(f"[TestTask] 任务 {self.task_id} 将运行 {duration} 秒")
            
            for i in range(duration):
                print(f"[TestTask] 任务 {self.task_id} 进度: {i+1}/{duration}")
                time.sleep(1)
            
            result = {
                'success': True,
                'message': f'测试任务完成，运行了 {duration} 秒',
                'task_id': self.task_id,
                'config': self.config
            }
            
            print(f"[TestTask] 任务 {self.task_id} 执行成功")
            return result
            
        except Exception as e:
            print(f"[TestTask] 任务 {self.task_id} 执行失败: {e}")
            return {
                'success': False,
                'message': f'测试任务失败: {str(e)}',
                'task_id': self.task_id
            }