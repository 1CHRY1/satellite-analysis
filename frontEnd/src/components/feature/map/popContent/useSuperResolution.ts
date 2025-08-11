import { ref, type Ref } from "vue"
import { gridData } from "./shared"
import { ElMessage } from "element-plus"
import { getCaseStatus, getCaseBandsResult } from "@/api/http/satellite-data"
import bus from "@/store/bus"
import { GetSuperResolution } from '@/api/http/satellite-data/visualize.api'
import { visualLoad, selectedSensor, selectedRBand, selectedGBand, selectedBBand } from "./useGridScene"

export const useSuperResolution = () => {

    //超分
    type band_path={
        R: string,
        G: string,
        B: string
    }
    const calTask: Ref<any> = ref({
        calState: 'start',
        taskId: ''
    })

    const isSuperRes = ref(false)

    const handleSuperResolution = async ()=> {
        if (!gridData.value.sceneRes.dataset) {
            return
        }
        if (visualLoad.value){
            isSuperRes.value = !isSuperRes.value 
            try{
                // handleRemove()
                let currentScene
                for (const category of gridData.value.sceneRes.category) {
                    for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                        if (scene.sensorName === selectedSensor.value) {
                            currentScene = scene
                        }
                    }
                }

                const bands : band_path ={
                    R:'',
                    G:'',
                    B:''
                }
                if (!currentScene) {
                    ElMessage.error('未找到对应的数据');
                    return;
                }

                currentScene.images.forEach((bandImg) => {
                            if (bandImg.band === selectedRBand.value) {
                                bands.R = bandImg.bucket + '/' + bandImg.tifPath
                            }
                            if (bandImg.band === selectedGBand.value) {
                                bands.G = bandImg.bucket + '/' + bandImg.tifPath
                            }
                            if (bandImg.band === selectedBBand.value) {
                                bands.B = bandImg.bucket + '/' + bandImg.tifPath
                            }
                        })
                const result = await GetSuperResolution({
                    columnId: gridData.value.columnId,
                    rowId: gridData.value.rowId,
                    resolution: gridData.value.resolution,
                    band: bands
                });
                
                calTask.value.taskId = result.data
                // 2、轮询运行状态，直到运行完成
                // ✅ 轮询函数，直到 data === 'COMPLETE'
                const pollStatus = async (taskId: string) => {
                    console.log('查询报错')
                    const interval = 1000 // 每秒轮询一次
                    return new Promise<void>((resolve, reject) => {
                        const timer = setInterval(async () => {
                            try {
                                const res = await getCaseStatus(taskId)
                                console.log('轮询结果:', res)

                                if (res?.data === 'COMPLETE') {
                                    clearInterval(timer)
                                    resolve()
                                } else if (res?.data === 'ERROR') {
                                    console.log(res, res.data, 15616);

                                    clearInterval(timer)
                                    reject(new Error('任务失败'))
                                }
                            } catch (err) {
                                clearInterval(timer)
                                reject(err)
                            }
                        }, interval)
                    })
                }
                
                try {
                    console.log("开始")
                    await pollStatus(calTask.value.taskId)
                    // ✅ 成功后设置状态
                    calTask.value.calState = 'success'
                    let bandres = await getCaseBandsResult(calTask.value.taskId)
                    console.log(bandres, '结果');
                    console.log('超分返回数据',bandres.data)
                // console.log(result.value)
                    bus.emit('SuperResTimeLine', bandres.data,isSuperRes.value)
                    
                } catch (error) {
                    calTask.value.calState = 'failed'

                    console.error('有问题');
                    console.error('问题double')
                }
                ElMessage.success('超分处理成功')
                    
            }catch{
                ElMessage.error('超分处理失败');
            }
        } else{
            ElMessage.error('请先完成立方体可视化')
        }
    }

    return {
        handleSuperResolution,
    }
}