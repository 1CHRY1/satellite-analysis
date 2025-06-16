import { getCaseById, pollStatus, type ModelStatus } from "@/api/http/satellite-data"
import { useTaskStore } from "@/store"
import { ElMessage } from "element-plus"
import { computed, ref } from "vue"
import Bus from '@/store/bus'

export function useTaskPollModule() {
    const taskStore = useTaskStore()
    const pendingTaskList = computed<string[]>(() => getPendingTaskList())
    const pendingInterval = ref<number | null>(null)
    const runningInterval = ref<number | null>(null)

    const getPendingTaskList = () => {
        const pendingTaskList = taskStore.pendingTaskList
        if (pendingTaskList.length === 0) return []
        else return pendingTaskList
    }

    const pollPendingTaskStatus = () => {
        const pendingTaskList = taskStore.pendingTaskList
        if (pendingTaskList.length === 0) return
        console.log('pollPending')
        pendingTaskList.forEach(async (taskId) => {

            const res = await getCaseById(taskId)
            if (res.data) {
                taskStore.setTaskStatus(taskId, res.data.status as ModelStatus)
                // 刷新列表
                Bus.emit('case-list-refresh')
                // 因为pollPending和pollRunning的间隔时间不一样，所以需要额外处理
                if (res.data.status === 'COMPLETE') {
                    ElMessage.success(`${res.data.address}无云一版图计算成功`)
                    taskStore.deleteTask(taskId)
                } else if (res.data.status === 'ERROR') {
                    ElMessage.error(`${res.data.address}无云一版图计算失败`)
                    taskStore.deleteTask(taskId)
                }
            }
        })
    }

    const updatePendingTaskStatus = () => {
        pollPendingTaskStatus()
        // 这里的pending是指任务已经提交，但是可能还没有持久化到数据库case列表
        pendingInterval.value = setInterval(() => {
            pollPendingTaskStatus()
        }, 1500)
    }

    const pollRunningTaskStatus = () => {
        const runningTaskList = taskStore.runningTaskList
        if (runningTaskList.length === 0) return
        console.log('pollRunning')
        runningTaskList.forEach(async (taskId) => {
            const res = await getCaseById(taskId)
            if (res.data) {
                if (res.data.status === 'COMPLETE') {
                    taskStore.setTaskStatus(taskId, res.data.status as ModelStatus)
                    ElMessage.success(`${res.data.address}无云一版图计算成功`)
                    taskStore.deleteTask(taskId)
                    // 刷新列表
                    Bus.emit('case-list-refresh')
                } else if (res.data.status === 'ERROR') {
                    ElMessage.error(`${res.data.address}无云一版图计算失败`)
                    taskStore.deleteTask(taskId)
                }
            }
        })
    }

    const updateRunningTaskStatus = () => {
        pollRunningTaskStatus()
        runningInterval.value = setInterval(() => {
            pollRunningTaskStatus()
        }, 5000)
    }

    const stopPolling = () => {
        clearInterval(pendingInterval.value as number);
        clearInterval(runningInterval.value as number);
    };

    const startPolling = () => {
        updatePendingTaskStatus()
        updateRunningTaskStatus()
    }

    return {
        pendingTaskList,
        getPendingTaskList,
        updatePendingTaskStatus,
        updateRunningTaskStatus,
        startPolling,
        stopPolling
    }
}
