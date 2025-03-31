import queue
import traceback
import uuid
import threading
from datetime import datetime
from typing import Any, Dict

from dataProcessing.config import STATUS_RUNNING, STATUS_COMPLETE, STATUS_ERROR, STATUS_PENDING, MAX_RUNNING_TASKS

from dataProcessing.model.mergeTifTask import MergeTifTask


class TaskScheduler:
    def __init__(self):
        self.pending_queue = queue.Queue()
        self.running_queue = queue.Queue(maxsize=MAX_RUNNING_TASKS)  # 限制并发数
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
                # --------- Pending queue to running queue -------------------------------------
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
        # --------- Execute the task ----------------------------
        try:
            task_instance = self.task_info[task_id]
            self.task_status[task_id] = STATUS_RUNNING
            result = task_instance.run()

            # --------- Update the status and queue ---------------------------
            with self.condition:
                self.task_status[task_id] = STATUS_COMPLETE
                self.task_results[task_id] = result

                # 显式移除特定任务ID
                # TODO RUNNING 数组
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
            # --------- Handle Exceptions --------------------------------
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
    # --------- Init the Task Scheduler ---------------------------
    global scheduler
    if scheduler is None:
        scheduler = TaskScheduler()
    return scheduler


if __name__ == "__main__":
    # 只在直接运行时启动（防止 import 时报错）
    init_scheduler()
