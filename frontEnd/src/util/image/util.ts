import workerPool from '@/util/worker/workerPool'
import { getTifPreviewUrl, getGridPreviewUrl } from '@/api/http/satellite-data/visualize.api'

type GridPreviewParams = {
    rowId: number
    columnId: number
    resolution: number
    redPath: string
    greenPath: string
    bluePath: string
}

type BandMergeParams = {
    redPath: string
    greenPath: string
    bluePath: string
}

const handleError = (error: Error | string) => {
    console.error('Worker Error:', error);
}


class BandMergeHelper {
    private taskCallbacks: Map<number | string, (url: string) => void> = new Map();
    private nextTaskId: number = 0;
    static _instance: BandMergeHelper | null = null;

    constructor() {
        workerPool.setTaskCompletedCallback(this.handleTaskCompleted.bind(this));
        workerPool.setErrorCallback(handleError);
    }

    static getInstance(): BandMergeHelper {
        if (!this._instance) {
            this._instance = new BandMergeHelper();
        }
        return this._instance;
    }

    private handleTaskCompleted = (result: { id: number | string; combinedPngBuffer: ArrayBuffer | null }) => {
        if (result.combinedPngBuffer) {
            const blob = new Blob([result.combinedPngBuffer], { type: 'image/png' });
            const url = URL.createObjectURL(blob);
            const callback = this.taskCallbacks.get(result.id);
            if (callback) {
                callback(url);
                this.taskCallbacks.delete(result.id);
            } else {
                console.warn(`No callback found for task ID: ${result.id}`);
            }
        } else {
            console.error(`Task ${result.id} failed to combine images.`);
        }
    }

    /**
     * 整景的波段合并，参数是三个tif的完整！路径
     * @param params 
     * @param callback 
     */
    async mergeOne(params: BandMergeParams, callback: (url: string) => void): Promise<void> {

        const redPath = await getTifPreviewUrl(params.redPath)
        const greenPath = await getTifPreviewUrl(params.greenPath)
        const bluePath = await getTifPreviewUrl(params.bluePath)

        const taskId = this.nextTaskId++;
        this.taskCallbacks.set(taskId, callback);
        workerPool.enqueueTask([redPath, greenPath, bluePath], taskId);
    }

    async mergeGrid(params: GridPreviewParams, callback: (url: string) => void): Promise<void> {

        // const redPath = await getTifPreviewUrl(params.redPath)
        // const greenPath = await getTifPreviewUrl(params.greenPath)
        // const bluePath = await getTifPreviewUrl(params.bluePath)
        const gridParams = {
            rowId: params.rowId,
            columnId: params.columnId,
            resolution: params.resolution
        }
        const redPath = await getGridPreviewUrl({
            ...gridParams,
            tifFullPath: params.redPath
        })
        const greenPath = await getGridPreviewUrl({
            ...gridParams,
            tifFullPath: params.greenPath
        })
        const bluePath = await getGridPreviewUrl({
            ...gridParams,
            tifFullPath: params.bluePath
        })

        const taskId = this.nextTaskId++;
        this.taskCallbacks.set(taskId, callback);
        workerPool.enqueueTask([redPath, greenPath, bluePath], taskId);

    }
}

export default BandMergeHelper.getInstance()