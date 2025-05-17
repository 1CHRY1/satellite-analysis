interface WorkerPoolTask {
    id: number | string;
    urls: [string, string, string]; // 假设每次合成需要三个 URL
}

interface WorkerPoolResult {
    id: number | string;
    combinedPngBuffer: ArrayBuffer | null;
}

class WorkerPool {
    private static _instance: WorkerPool;
    static workerCount: number = navigator.hardwareConcurrency - 2// 默认使用 CPU 核心数
    private taskQueue: WorkerPoolTask[] = [];
    private activeTasks: Partial<Record<number | string, Worker>> = {};
    private workers: Worker[] = [];
    private taskIdCounter: number = 0;
    private onTaskCompletedCallback: ((result: WorkerPoolResult) => void) | null = null;
    private onErrorCallback: ((error: Error | string) => void) | null = null;

    private constructor() {
        this.initializeWorkers();
    }

    static get instance(): WorkerPool {
        if (!WorkerPool._instance) {
            WorkerPool._instance = new WorkerPool();
        }
        return WorkerPool._instance;
    }

    setTaskCompletedCallback(callback: (result: WorkerPoolResult) => void) {
        this.onTaskCompletedCallback = callback;
    }

    setErrorCallback(callback: (error: Error | string) => void) {
        this.onErrorCallback = callback;
    }

    private initializeWorkers() {
        for (let i = 0; i < WorkerPool.workerCount; i++) {
            const worker = new Worker(new URL('./worker.ts', import.meta.url), { type: 'module' });
            worker.onmessage = (event: MessageEvent<WorkerPoolResult>) => {
                const result = event.data;
                delete this.activeTasks[result.id];
                if (this.onTaskCompletedCallback) {
                    this.onTaskCompletedCallback(result);
                }
                this.processQueue();
            };
            worker.onerror = (error) => {
                console.error('Worker error:', error);
                if (this.onErrorCallback) {
                    this.onErrorCallback(error.message);
                }
                // 可以考虑在出错后重启 worker
            };
            this.workers.push(worker);
        }
    }

    enqueueTask(urls: [string, string, string]): number | string {
        const taskId = this.taskIdCounter++;
        this.taskQueue.push({ id: taskId, urls });
        this.processQueue();
        return taskId;
    }

    private processQueue() {
        if (this.taskQueue.length > 0 && this.getIdleWorker()) {
            const task = this.taskQueue.shift()!;
            const worker = this.getIdleWorker()!;
            this.activeTasks[task.id] = worker;
            worker.postMessage({ id: task.id, urls: task.urls });
        }
    }

    private getIdleWorker(): Worker | undefined {
        return this.workers.find(worker => !Object.values(this.activeTasks).includes(worker));
    }

    // 如果需要立即停止所有工作
    terminateAll() {
        this.workers.forEach(worker => worker.terminate());
        this.workers = [];
        this.activeTasks = {};
        this.taskQueue = [];
        this.initializeWorkers(); // 可选：重新初始化 worker 池
    }

    // 获取当前活跃任务的数量
    numActiveTasks(): number {
        return Object.keys(this.activeTasks).length;
    }

    // 获取队列中等待任务的数量
    numQueuedTasks(): number {
        return this.taskQueue.length;
    }
}

export default WorkerPool.instance