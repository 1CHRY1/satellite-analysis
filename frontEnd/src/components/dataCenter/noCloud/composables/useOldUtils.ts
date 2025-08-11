import { exploreData } from './shared'

/**
 * 工具函数相关的组合式函数
 */

export const useUtils = () => {
    
    // 算格网的颜色,接收的数据分别为：要上色的格网本身，累积影像分布到格网的结果，格网数量，所属层级
    // 取消勾选，即回到上一级数据格网的结果也没问题，第三个传输就传递上一级（和第二个参数相同）即可
    const classifyGridSource = (
        index: any,
        sceneGridsRes: any,
        lastGridFeature?: any,
        type?: string,
    ) => {
        if (lastGridFeature === null) {
            let source: string | null
            sceneGridsRes[index]?.scenes.length > 0 ? (source = 'demotic1m') : (source = null)
            return source
        } else if (type !== undefined) {
            let source: string | null
            let lastSource = lastGridFeature.features[index].properties.source
            lastSource
                ? (source = lastSource)
                : sceneGridsRes[index]?.scenes.length > 0
                    ? (source = type)
                    : (source = null)
            return source
        }
        return null
    }

    // 判断格网到底有没有数据，有就返回0.3
    const judgeGridOpacity = (index: number, sceneGridsRes: any) => {
        let opacity = 0.01
        sceneGridsRes[index]?.scenes.length > 0 ? (opacity = 0.3) : (opacity = 0.01)
        return opacity
    }

    // 算覆盖率
    const getCoverage = (gridImages: any, gridCount: number) => {
        const nonEmptyScenesCount = gridImages.filter((item) => item.scenes.length > 0).length
        let coverage = ((nonEmptyScenesCount * 100) / gridCount).toFixed(2) + '%'
        return coverage
    }

    // 计算四个覆盖率
    const calculateCoverageRates = async (getSceneGrids: any, demotic1mImages: any, demotic2mImages: any, internationalImages: any, radarImages: any) => {
        let gridCount = exploreData.grids.length
        let allGrids = exploreData.grids.map((item: any) => {
            return {
                rowId: item.rowId,
                columnId: item.columnId,
                resolution: item.resolution,
            }
        })

        // 计算四种情况的格网分布情况
        const demotic1mGridImages = await getSceneGrids({
            grids: allGrids,
            sceneIds: demotic1mImages.map((images) => images.sceneId),
        })
        const demotic1mCoverage = getCoverage(demotic1mGridImages, gridCount)

        let addDemotic1mImages = demotic1mImages.concat(demotic2mImages)
        const demotic2mGridImages = await getSceneGrids({
            grids: allGrids,
            sceneIds: addDemotic1mImages.map((images) => images.sceneId),
        })
        const demotic2mCoverage = getCoverage(demotic2mGridImages, gridCount)

        let addInternationalImages = addDemotic1mImages.concat(internationalImages)
        const interGridImages = await getSceneGrids({
            grids: allGrids,
            sceneIds: addInternationalImages.map((images) => images.sceneId),
        })
        const internationalCoverage = getCoverage(interGridImages, gridCount)

        let addRadarImages = addInternationalImages.concat(radarImages)
        const radarGridImages = await getSceneGrids({
            grids: allGrids,
            sceneIds: addRadarImages.map((images) => images.sceneId),
        })
        const radarCoverage = getCoverage(radarGridImages, gridCount)

        return {
            demotic1m: demotic1mCoverage,
            demotic2m: demotic2mCoverage,
            international: internationalCoverage,
            radar: radarCoverage
        }
    }

    return {
        classifyGridSource,
        judgeGridOpacity,
        getCoverage,
        calculateCoverageRates
    }
}