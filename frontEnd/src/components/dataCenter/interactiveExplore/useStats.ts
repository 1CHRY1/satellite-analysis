import { ref } from "vue"
const classifiedScenes = ref({}) // 注意全局变量写在外面
/**
 * 各数据类型统计信息
 */
export const useStats = () => {
    /**
     * 1. 遥感数据统计信息
     */

    // 数各种分辨率分别覆盖了多少格网
    const countResolutionCoverage = (allGridScene: any[]) => {
        const result = {
            '亚米': 0,
            '2米': 0,
            '10米': 0,
            '30米': 0,
            '其他': 0,
        }

        allGridScene.forEach((grid) => {
            const seen = new Set<string>() // 记录当前 grid 中已统计过的 resolution 分类

            grid.scenes?.forEach((scene: any) => {
                const resStr = scene.resolution?.toString().replace('m', '')
                const res = parseFloat(resStr)

                let key: string
                if (res <= 1) {
                    key = '亚米'
                } else if (res === 2) {
                    key = '2米'
                } else if (res === 10) {
                    key = '10米'
                } else if (res === 30) {
                    key = '30米'
                } else {
                    key = '其他'
                }

                if (!seen.has(key)) {
                    seen.add(key)
                    result[key] += 1
                }
            })
        })

        return result
    }

    // 数各种分辨率的数据集分别覆盖了多少格网
    const classifyScenesByResolution = (allScenes: any[]) => {
        const result = {
            '1m': [],
            '2m': [],
            '10m': [],
            '30m': [],
            '500m': [],
        }

        const addToCategory = (key: string, platform: string) => {
            if (!result[key].includes(platform)) {
                result[key].push(platform)
            }
        }

        for (const scene of allScenes) {
            const resStr = scene.resolution?.toString().replace('m', '')
            const res = parseFloat(resStr)
            const platform = scene.platformName

            if (!platform || isNaN(res)) continue

            if (res <= 1) {
                if (scene.tags.includes('ard')) {
                    addToCategory('1m', platform)
                }
            } else if (res === 2) {
                addToCategory('2m', platform)
            } else if (res === 10) {
                addToCategory('10m', platform)
            } else if (res === 30) {
                addToCategory('30m', platform)
            } else {
                addToCategory('500m', platform)
            }
        }
        classifiedScenes.value = result
    }
    
    const getSceneCountByResolution = (resolution: number, allScenes: any[]) => {
        let count = 0
    
        // 去掉亚米传统数据
        let filteredScenes = allScenes.filter(item => item.tags.includes('ard') || parseFloat(item.resolution) > 1)
    
        if (resolution === 1) {
            filteredScenes.forEach((scene: any) => {
                let data = parseFloat(scene.resolution)
                if (data <= 1) {
                    count++
                }
            })
            return count
        } else if (resolution === 500) {
            filteredScenes.forEach((scene: any) => {
                let data = parseFloat(scene.resolution)
                if (data > 1 && data != 2 && data != 10 && data != 30) {
                    count++
                }
            })
            return count
        } else {
            filteredScenes.forEach((scene: any) => {
                let data = parseFloat(scene.resolution)
                if (data === resolution) {
                    count++
                }
            })
            return count
        }
    }

    // 工具函数： 获取platformName名对应的所有景
    const getSceneIdsByPlatformName = (label: string, selectPlatformName: string, allScenes: any[]) => {
        console.log('所有景', allScenes)
        console.log('选中的平台名', selectPlatformName)
        let scenes = allScenes
        if (label === '亚米') {
            scenes = allScenes.filter((scene) => {
                if (scene.tags.includes('ard')) {
                    return scene
                }
            })
        }
        console.log(scenes, 'allImages')

        if (selectPlatformName === 'all') return scenes.map((item) => item.sceneId)

        const res: any[] = []
        scenes.forEach((item) => {
            if (item.platformName == selectPlatformName) {
                res.push(item.sceneId)
            }
        })
        console.log(res, 'images')

        return res
    }

    // 工具函数： platformName -> sensorName, 接口需要sensorName
    const getSensorNamebyPlatformName = (selectPlatformName: string, allScenes: any[]) => {
        // if (platformName === 'all') return 'all'
        // return platformName.split('_')[1]
        // 先把全选去了，接口没留全选逻辑
        let sensorName = ''
        for (let item of allScenes) {
            if (item.platformName === selectPlatformName) {
                sensorName = item.sensorName
                break
            }
        }
        return sensorName
    }
    

    /**
     * 2. 矢量数据统计信息
     */

    /**
     * 3. 数字高程模型统计信息
     */

    return {
        countResolutionCoverage,
        classifyScenesByResolution,
        getSceneCountByResolution,
        getSceneIdsByPlatformName,
        getSensorNamebyPlatformName,
        classifiedScenes
    }
}
