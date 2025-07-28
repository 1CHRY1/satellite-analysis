import { ref } from "vue"
const classifiedScenes = ref({}) // 注意全局变量写在外面
const classifiedProducts = ref({})
/**
 * 各数据类型统计信息
 */
export const useStats = () => {
    /**
     * 1. 遥感数据统计信息
     */

    // 数各种分辨率分别覆盖了多少格网
    const countResolutionScenesCoverage = (allGridScene: any[]) => {
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

        // const addToCategory = (key: string, platform: any) => {
        //     if (!result[key].includes(platform)) {
        //         result[key].push(platform)
        //     }
        // }

        const addToCategory = (key: string, platform: string | any) => {
            const categoryItems = result[key] || []; // 确保数组存在
            
            // 如果是字符串，直接比较
            if (typeof platform === 'string') {
              if (!categoryItems.includes(platform)) {
                result[key] = [...categoryItems, platform];
              }
            } 
            // 如果是对象，比较 sensorName
            else if (typeof platform === 'object' && platform !== null) {
              const alreadyExists = categoryItems.some(
                (item) => typeof item === 'object' && item.sensorName === platform.sensorName
              );
              if (!alreadyExists) {
                result[key] = [...categoryItems, platform];
              }
            }
        };


        for (const scene of allScenes) {
            const resStr = scene.resolution?.toString().replace('m', '')
            const res = parseFloat(resStr)
            const platformName = scene.platformName
            const sensorName = scene.sensorName

            if (!platformName || isNaN(res)) continue

            if (res <= 1) {
                if (scene.tags.includes('ard')) {
                    addToCategory('1m', {sensorName, platformName})
                }
            } else if (res === 2) {
                addToCategory('2m', {sensorName, platformName})
            } else if (res === 10) {
                addToCategory('10m', {sensorName, platformName})
            } else if (res === 30) {
                addToCategory('30m', {sensorName, platformName})
            } else {
                addToCategory('500m', {sensorName, platformName})
                // continue
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
     * 3. 遥感影像产品统计信息
     */
    // 数各种产品分别覆盖了多少格网
    const countProductsCoverage = (allGridScene: any[]) => {
        const result = {
            'DEM': 0,
            '红绿立体影像': 0,
            '形变速率': 0,
            'NDVI': 0,
            '其他': 0,
        }

        allGridScene.forEach((grid) => {
            const seen = new Set<string>() // 记录当前 grid 中已统计过的 product 分类

            for (const scene of grid.scenes) {
                const dataType = scene.dataType

                if (dataType === 'satellite') continue

                let key: string = ''
                if (dataType === 'dem') {
                    key = 'DEM'
                } else if (dataType === '3d') {
                    key = '红绿立体影像'
                } else if (dataType === 'svr') {
                    key = '形变速率'
                } else if (dataType === 'ndvi') {
                    key = 'NDVI'
                } else {
                    key = '其他'
                }

                if (!seen.has(key)) {
                    seen.add(key)
                    result[key] += 1
                }
            }

        })

        return result
    }

    // 数各种产品分别覆盖了多少景
    const classifyProducts = (allProducts: any[]) => {
        const result = {
            'DEM': [],
            '红绿立体影像': [],
            '形变速率': [],
            'NDVI': [],
            '其他': [],
        }

        const addToCategory = (key: string, platform: string) => {
            if (!result[key].includes(platform)) {
                result[key].push(platform)
            }
        }
        console.log(allProducts, 'allProducts')
        for (const scene of allProducts) {
            const dataType = scene.dataType
            const platform = scene.platformName

            if (!platform || dataType === 'satellite') continue

            if (dataType === 'dem') {
                addToCategory('DEM', platform)
            } else if (dataType === '3d') {
                addToCategory('红绿立体影像', platform)
            } else if (dataType === 'svr') {
                addToCategory('形变速率', platform)
            } else if (dataType === 'ndvi') {
                addToCategory('NDVI', platform)
            } else {
                // addToCategory('其他', platform)
                continue
            }
        }
        classifiedProducts.value = result
        console.log(result, 'result')
    }

    const getSceneCountByProduct = (product: string, allProducts: any[]) => {
        let filteredScenes = allProducts.filter(item => item.dataType === product)

        return filteredScenes.length
    }

    return {
        countResolutionScenesCoverage,
        classifyScenesByResolution,
        getSceneCountByResolution,
        getSceneCountByProduct,
        getSceneIdsByPlatformName,
        getSensorNamebyPlatformName,
        classifiedScenes,
        classifiedProducts,
        classifyProducts,
        countProductsCoverage,
    }
}
