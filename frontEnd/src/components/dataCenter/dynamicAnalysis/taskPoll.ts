import { useTaskStore } from "@/store"
import { computed, ref } from "vue"
import Bus from '@/store/bus'
import { message } from "ant-design-vue"
import { getMethLibCaseById } from "@/api/http/analytics-display/methlib.api"
import type { ModelStatus } from "@/api/http/satellite-data"

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

            const res = await getMethLibCaseById(taskId)
            if (res.data) {
                taskStore.setTaskStatus(taskId, res.data.status as ModelStatus)
                // 刷新列表
                Bus.emit('methlib-case-list-refresh')
                // 因为pollPending和pollRunning的间隔时间不一样，所以需要额外处理
                if (res.data.status === 'COMPLETE') {
                    message.success(`计算成功`)
                    taskStore.deleteTask(taskId)
                } else if (res.data.status === 'ERROR') {
                    message.error(`计算失败`)
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
            const res = await getMethLibCaseById(taskId)
            if (res.data) {
                if (res.data.status === 'COMPLETE') {
                    taskStore.setTaskStatus(taskId, res.data.status as ModelStatus)
                    message.success(`计算成功`)
                    taskStore.deleteTask(taskId)
                    // 刷新列表
                    Bus.emit('methlib-case-list-refresh')
                } else if (res.data.status === 'ERROR') {
                    message.error(`计算失败`)
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
