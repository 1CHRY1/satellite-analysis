from abc import ABC, abstractmethod


class Task(ABC):
    """任务抽象基类，所有任务类型都需要继承此类并实现 run() 方法"""

    def __init__(self, task_id, *args, **kwargs):
        self.task_id = task_id
        self.args = args
        self.kwargs = kwargs

    @abstractmethod
    def run(self):
        """运行任务的抽象方法"""
        pass
