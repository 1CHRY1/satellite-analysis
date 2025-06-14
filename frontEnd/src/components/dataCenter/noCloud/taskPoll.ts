import { getCaseById, pollStatus, type ModelStatus } from "@/api/http/satellite-data"
import { useTaskStore } from "@/store"

export function useTaskPollModule() {
    const taskStore = useTaskStore()

    const getRunningTaskList = () => {
        const runningTaskList = taskStore.runningTaskList
        if (runningTaskList.length === 0) return

        
    }

    const updatePendingTaskStatus = () => {
        // 这里的pending是指任务已经提交，但是可能还没有持久化到数据库case列表
        setInterval(() => {
            const pendingTaskList = taskStore.pendingTaskList
            if (pendingTaskList.length === 0) return

            pendingTaskList.forEach(async (taskId) => {
                const res = await getCaseById(taskId)
                if (res.data) {
                    taskStore.setTaskStatus(taskId, res.data.status as ModelStatus)
                }
            })
        }, 1000)
    }

    return {
    }
}
