import queue
import traceback
import uuid
import threading
from datetime import datetime
from typing import Any, Dict
import os
import json
import hashlib
from dataProcessing.config import current_config as CONFIG

from dataProcessing.model.mergeTifTask import MergeTifTask
from dataProcessing.model.mergeTifTaskV2 import MergeTifTaskV2
from dataProcessing.model.calc_NDVI import calc_NDVI
from dataProcessing.model.get_spectral_profile import get_spectral_profile
from dataProcessing.model.calc_raster_point import calc_raster_point
from dataProcessing.model.calc_raster_line import calc_raster_line
from dataProcessing.model.calc_no_cloud import calc_no_cloud
from dataProcessing.model.calc_no_cloud_grid import calc_no_cloud_grid
from dataProcessing.model.calc_no_cloud_complex import calc_no_cloud_complex
from dataProcessing.model.create_low_level_mosaic import create_low_level_mosaic
from dataProcessing.model.create_low_level_mosaic_threads import create_low_level_mosaic_threads
from dataProcessing.model.test_task import TestTask

STATUS_RUNNING = CONFIG.STATUS_RUNNING
STATUS_COMPLETE = CONFIG.STATUS_COMPLETE
STATUS_ERROR = CONFIG.STATUS_ERROR
STATUS_PENDING = CONFIG.STATUS_PENDING
MAX_RUNNING_TASKS = CONFIG.MAX_RUNNING_TASKS

class TaskScheduler:
    def __init__(self):
        self.pending_queue = queue.Queue()
        self.running_queue = queue.Queue(maxsize=MAX_RUNNING_TASKS)  # 限制并发数
        self.complete_queue = queue.Queue()
        self.error_queue = queue.Queue()
        self.task_status: Dict[str, Any] = {}
        self.task_info: Dict[str, Any] = {}
        self.task_results: Dict[str, Any] = {}
        self.task_md5: Dict[str, Any] = {}

        # 锁和条件变量
        self.lock = threading.Lock()
        self.condition = threading.Condition(self.lock)

        # 启动调度线程
        self.scheduler_thread = threading.Thread(target=self._scheduler_worker, daemon=True)
        self.scheduler_thread.start()

        # 加载历史任务
        self.load_history()

    def load_history(self):
        # --------- Load the history of the task ---------------------------
        file_path = os.path.join(os.path.dirname(__file__), "..", "history")
        os.makedirs(file_path, exist_ok=True)
        for file in os.listdir(file_path):
            with open(os.path.join(file_path, file), "r", encoding="utf-8") as f:
                try:
                    json_data = json.load(f)
                    self.set_status(json_data["ID"], json_data["STATUS"])
                    self.task_results[json_data["ID"]] = json_data["RESULT"]
                    self.task_md5[json_data["ID"]] = json_data["MD5"]
                    task_id = json_data["ID"]
                    if self.task_status[task_id] == STATUS_COMPLETE:
                        self.complete_queue.put(task_id)
                    # elif self.task_status[task_id] == STATUS_ERROR:
                    #     self.error_queue.put(task_id)
                except Exception as e:
                    print(f"Error loading history: {e}")
                    traceback.print_exc()

    def start_task(self, task_type, *args, **kwargs):
        cur_md5 = self.generate_md5(json.dumps(args))
        # if cur_md5 in self.task_md5.values():   #  测试原因，注释，提交的时候记得改回来~~~~~~~~~~~~~~~~~~~~~~~~~
        #     for key, value in self.task_md5.items():
        #         if value == cur_md5:
        #             return key
        # --------- Start task -------------------------------------
        task_id = str(uuid.uuid4())  # 生成唯一任务 ID
        task_class = self._get_task_class(task_type)
        with self.condition:
            # 初始化任务状态
            self.task_status[task_id] = STATUS_PENDING
            task_instance = task_class(task_id, *args, **kwargs)
            self.task_info[task_id] = task_instance
            self.task_md5[task_id] = self.generate_md5(json.dumps(args))

            # 加入pending队列
            self.pending_queue.put(task_id)
            self.condition.notify_all()  # 通知调度线程

        return task_id

    def generate_md5(self, input_string):
        md5_hasher = hashlib.md5()
        md5_hasher.update(input_string.encode('utf-8'))
        md5_hash = md5_hasher.hexdigest()
        return md5_hash

    def _get_task_class(self, task_type):
        """根据任务类型返回相应的任务类"""
        task_classes = {
            'merge_tif': MergeTifTask,
            'merge_tif_v2': MergeTifTaskV2,
            'calc_no_cloud': calc_no_cloud,
            'calc_no_cloud_grid': calc_no_cloud_grid,
            'calc_NDVI': calc_NDVI,
            'get_spectral_profile': get_spectral_profile,
            'calc_raster_point': calc_raster_point,
            'calc_raster_line': calc_raster_line,
            'calc_no_cloud_complex': calc_no_cloud_complex,
            'create_low_level_mosaic': create_low_level_mosaic,
            'create_low_level_mosaic_threads': create_low_level_mosaic_threads,
            'test': TestTask,
            # 可以在这里扩展其他类型的任务
        }
        return task_classes.get(task_type)

    def _scheduler_worker(self):
        print("调度器工作线程已启动")
        while True:
            try:
                # --------- Pending queue to running queue -------------------------------------
                with self.condition:
                    while not self.pending_queue.empty() and not self.running_queue.full():
                        task_id = self.pending_queue.get()
                        print(f"[调度器] 从待处理队列获取任务: {task_id}")
                        self.running_queue.put(task_id)
                        print(f"[调度器] 任务 {task_id} 已加入运行队列")

                        # 为每个运行中的任务创建执行线程
                        thread = threading.Thread(target=self._execute_task, args=(task_id,), daemon=True)
                        thread.start()
                        print(f"[调度器] 任务 {task_id} 的执行线程已启动")

                    # 暂停，等待任务变化
                    self.condition.wait(timeout=1)
            except Exception as e:
                print(f"Scheduler worker error: {e}")
                traceback.print_exc()

    def _execute_task(self, task_id: str):
        # --------- Execute the task ----------------------------
        print(f"[执行器] 开始执行任务: {task_id}")
        try:
            task_instance = self.task_info[task_id]
            print(f"[执行器] 获取任务实例: {task_instance.__class__.__name__}")
            self.task_status[task_id] = STATUS_RUNNING
            # Reuse the result of the task
            
            print(f"[执行器] 调用任务 {task_id} 的run方法")
            result = task_instance.run()
            print(f"[执行器] 任务 {task_id} 执行完成")

            # --------- Update the status and queue ---------------------------
            with self.condition:
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

                # Update the result and status
                self.task_results[task_id] = result
                self.task_status[task_id] = STATUS_COMPLETE
                self.condition.notify_all()

        except Exception as e:
            # --------- Handle Exceptions --------------------------------
            with self.condition:
                del self.task_md5[task_id]
                
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

                # Update the result and status
                self.task_status[task_id] = STATUS_ERROR
                self.task_results[task_id] = str(e)
                self.condition.notify_all()
        finally:
            if self.get_status(task_id) != 'COMPLETE':
                return
            # 持久化任务记录至history
            self.save_to_history(task_id)

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
        
    def set_status(self, task_id, status):
        # --------- Set the status of the task ---------------------------
        self.task_status[task_id] = status
        if status == 'COMPLETE':
            self.task_status[task_id] = STATUS_COMPLETE
        elif status == 'ERROR':
            self.task_status[task_id] = STATUS_ERROR
        elif status == 'PENDING':
            self.task_status[task_id] = STATUS_PENDING
        elif status == 'RUNNING':
            self.task_status[task_id] = STATUS_RUNNING
        elif status == 'NONE':
            self.task_status[task_id] = None

    def get_result(self, task_id):
        # --------- Get the result of the task ---------------------------
        if self.task_status.get(task_id) == STATUS_COMPLETE:
            return self.task_results.get(task_id, "Result Not Available")
        return "Task Not Completed or Not Found"

    def save_to_history(self, task_id):
        # 序列化任务记录至history
        file_path = os.path.join(os.path.dirname(__file__), "..", "history", f"{task_id}.json")
        json_data = {
            "STATUS": self.get_status(task_id),
            "RESULT": self.get_result(task_id),
            "TIME": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            # TASK TYPE
            "ID": task_id,
            "MD5": self.task_md5[task_id]
        }
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(json_data, f, ensure_ascii=False, indent=2)

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
