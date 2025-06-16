import { defineStore } from 'pinia'
import type { ModelStatus } from '@/api/http/satellite-data/satellite.type'

export const useTaskStore = defineStore('taskStore', {
    state: () => ({
        _taskObj: {},
        _taskCount: 0 as number,
        _isInitialTaskPending: false as boolean
    }),
    getters: {
        taskObj: (state) => state._taskObj,
        pendingTaskList: (state) => Object.entries(state._taskObj).filter(([,value]) => value === 'PENDING').map(([key]) => key),
        completeTaskList: (state) => Object.entries(state._taskObj).filter(([,value]) => value === 'COMPLETE').map(([key]) => key),
        runningTaskList: (state) => Object.entries(state._taskObj).filter(([,value]) => value === 'RUNNING').map(([key]) => key),
    },
    actions: {
        setTaskStatus(taskId: string, status: ModelStatus) {
            this._taskObj[taskId] = status
        },
        setIsInitialTaskPending(isInitialTaskPending: boolean) {
            this._isInitialTaskPending = isInitialTaskPending
        },
        deleteTask(taskId: string) {
            delete this._taskObj[taskId]
        }
    },
})
