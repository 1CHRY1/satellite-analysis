import queue
from datetime import datetime, timedelta

from dataProcessing import config
from dataProcessing.Utils.osUtils import getMinioClient


def delete_old_objects():
    """删除当天之前上传的 objects"""
    today = datetime.now().strftime('%Y-%m/%d')

    objects_to_delete = []
    client = getMinioClient()
    for obj in client.list_objects(config.MINIO_TEMP_FILES_BUCKET, recursive=True):
        if obj.object_name < today:  # 只删除早于今天的
            objects_to_delete.append(obj.object_name)

    if objects_to_delete:
        print(f"Deleting {len(objects_to_delete)} old objects...")
        for obj_name in objects_to_delete:
            client.remove_object(config.MINIO_TEMP_FILES_BUCKET, obj_name)
        print("Deletion complete.")
    else:
        print("No old objects to delete.")


def reset_scheduler():
    from dataProcessing.model.scheduler import init_scheduler
    from datetime import datetime, timedelta

    scheduler = init_scheduler()
    threshold = datetime.now() - timedelta(minutes=30)

    # 使用一个临时队列存储未超时的元素
    new_queue = queue.Queue()

    while not scheduler.complete_queue.empty():
        item = scheduler.complete_queue.get()
        timestamp, task_id = item
        if timestamp >= threshold:  # 只保留未超时的数据
            new_queue.put(item)
        else:
            scheduler.task_status.pop(task_id, None)
            scheduler.task_results.pop(task_id, None)

    # 替换原队列
    scheduler.complete_queue = new_queue
