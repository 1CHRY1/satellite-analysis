import queue
import traceback
import uuid
import threading
from datetime import datetime
from queue import Queue
from typing import Any, Dict

from dataProcessing.config import STATUS_RUNNING, STATUS_COMPLETE, STATUS_ERROR, STATUS_PENDING
import time

from dataProcessing.model.mergeTifTask import MergeTifTask
max_running_tasks = 2

class TaskScheduler:
    def __init__(self):
        self.pending_queue = queue.Queue()
        self.running_queue = queue.Queue(maxsize=max_running_tasks)  # 限制并发数
        self.complete_queue = queue.Queue()
        self.error_queue = queue.Queue()
        self.task_status: Dict[str, Any] = {}
        self.task_info: Dict[str, Any] = {}
        self.task_results: Dict[str, Any] = {}

        # 锁和条件变量
        self.lock = threading.Lock()
        self.condition = threading.Condition(self.lock)

        # 启动调度线程
        self.scheduler_thread = threading.Thread(target=self._scheduler_worker, daemon=True)
        self.scheduler_thread.start()

    def start_task(self, task_type, *args, **kwargs):
        # --------- Start task -------------------------------------
        task_id = str(uuid.uuid4())  # 生成唯一任务 ID
        task_class = self._get_task_class(task_type)
        with self.condition:
            # 初始化任务状态
            self.task_status[task_id] = STATUS_PENDING
            task_instance = task_class(task_id, *args, **kwargs)
            self.task_info[task_id] = task_instance

            # 加入pending队列
            self.pending_queue.put(task_id)
            self.condition.notify_all()  # 通知调度线程

        return task_id

    def _get_task_class(self, task_type):
        """根据任务类型返回相应的任务类"""
        task_classes = {
            'merge_tif': MergeTifTask,
            # 可以在这里扩展其他类型的任务
        }
        return task_classes.get(task_type)

    def _scheduler_worker(self):
        while True:
            try:
                # 尝试从pending队列调度任务到running队列
                with self.condition:
                    while not self.pending_queue.empty() and not self.running_queue.full():
                        task_id = self.pending_queue.get()
                        self.running_queue.put(task_id)

                        # 为每个运行中的任务创建执行线程
                        thread = threading.Thread(target=self._execute_task, args=(task_id,), daemon=True)
                        thread.start()

                    # 暂停，等待任务变化
                    self.condition.wait(timeout=1)
            except Exception as e:
                print(f"Scheduler worker error: {e}")
                traceback.print_exc()

    def _execute_task(self, task_id: str):
        try:
            # 执行任务
            task_instance = self.task_info[task_id]
            self.task_status[task_id] = STATUS_RUNNING
            result = task_instance.run()

            # 更新任务为完成状态
            with self.condition:
                self.task_status[task_id] = STATUS_COMPLETE
                self.task_results[task_id] = result

                # 显式移除特定任务ID
                temp_queue = queue.Queue()
                found = False
                while not self.running_queue.empty():
                    current_task = self.running_queue.get()
                    if current_task == task_id:
                        found = True
                    else:
                        temp_queue.put(current_task)

                # 恢复其他任务
                while not temp_queue.empty():
                    self.running_queue.put(temp_queue.get())

                if not found:
                    print(f"Warning: Task {task_id} not found in running queue")

                self.complete_queue.put((datetime.now(), task_id))
                self.condition.notify_all()

        except Exception as e:
            # 类似地处理错误情况
            with self.condition:
                self.task_status[task_id] = STATUS_ERROR
                self.task_results[task_id] = str(e)

                temp_queue = queue.Queue()
                found = False
                while not self.running_queue.empty():
                    current_task = self.running_queue.get()
                    if current_task == task_id:
                        found = True
                    else:
                        temp_queue.put(current_task)

                while not temp_queue.empty():
                    self.running_queue.put(temp_queue.get())

                if not found:
                    print(f"Warning: Task {task_id} not found in running queue")

                self.error_queue.put(task_id)
                self.condition.notify_all()

    def _task_worker(self):
        # --------- Execute the task queue ---------------------------
        from flask import current_app
        from dataProcessing.server import app  # 替换为您的 Flask 应用模块
        while True:
            task_id = self.task_queue.get()  # 取出队列中的任务 ID
            if task_id is None:
                break  # 退出循环，结束任务执行

            task_instance = self.task_info[task_id]  # 获取任务实例
            self.task_status[task_id] = STATUS_RUNNING  # 任务开始执行

            try:
                with app.app_context():
                    result = task_instance.run()  # 执行任务
                    self.task_results[task_id] = result  # 存储结果
                    self.task_status[task_id] = STATUS_COMPLETE  # 任务完成
            except Exception as e:
                self.task_status[task_id] = STATUS_ERROR  # 任务出错
                self.task_results[task_id] = str(e)
                # 记录错误日志
                print(f"Task {task_id} failed: {e}")
                import traceback
                traceback.print_exc()  # 打印完整的堆栈跟踪

    def get_status(self, task_id):
        # --------- Get the status of the task ---------------------------
        current_status = self.task_status.get(task_id)
        if current_status is None:
            return 'NONE'
        elif current_status == STATUS_PENDING:
            return 'PENDING'
        elif current_status == STATUS_RUNNING:
            return 'RUNNING'
        elif current_status == STATUS_COMPLETE:
            return 'COMPLETE'
        elif current_status == STATUS_ERROR:
            return 'ERROR'

    def get_result(self, task_id):
        # --------- Get the result of the task ---------------------------
        if self.task_status.get(task_id) == STATUS_COMPLETE:
            return self.task_results.get(task_id, "Result Not Available")
        return "Task Not Completed or Not Found"


scheduler = None


def init_scheduler():
    """初始化任务调度器"""
    global scheduler
    if scheduler is None:
        scheduler = TaskScheduler()
        # scheduler._task_worker()
    return scheduler

if __name__ == "__main__":
    # 只在直接运行时启动（防止 import 时报错）
    init_scheduler()
