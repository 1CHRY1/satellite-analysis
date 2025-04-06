import * as SatelliteDataApi from '@/api/http/satellite-data'
import type { ImageFilterCondition, Project } from '../type'
import type { polygonGeometry } from '@/util/share.type'

///// View /////////////////////////
export interface ProductView {
    id: string
    name: string
    description: string
    resolution: string
    period: string
    sensorId: string
}

export interface SensorView {
    id: string
    name: string
    description: string
    products: ProductView[]
}

export interface SceneView {
    id: string
    productId: string
    sensorId: string
    name: string
    date: string
    cloudCover: number
    resolution: string
    preview_url: string
    geoFeature: polygonGeometry
    bands: string[]
    description: string
    tileLevelNum: number
    tileLevels: string[]
    crs: string
}

export interface BandView {
    id: string
    name: string
    sceneId: string
}

export interface OverlapTileInfoView {
    tilerServer: string
    objectPath: string
    tileId: string
    cloud: string
    sceneId: string
    columnId: string
    rowId: string
}

///// Adapter /////////////////////////

// 获取一个Sensor的Product列表
export async function fetchProductViews(sensorId: string): Promise<ProductView[]> {
    const productViews: ProductView[] = []

    const baseInfo = await SatelliteDataApi.getProductList(sensorId)

    baseInfo.forEach(async (pBaseInfo) => {
        const pDetailInfo = await SatelliteDataApi.getProductDetail(pBaseInfo.productId)
        const pView: ProductView = {
            id: pBaseInfo.productId,
            name: pBaseInfo.productName,
            description: pDetailInfo.description,
            resolution: pDetailInfo.resolution,
            period: pDetailInfo.period,
            sensorId: sensorId,
        }
        productViews.push(pView)
    })

    return productViews
}

// TODO: 分页查询
// 获取所有Sensor的列表
export async function fetchAllSensorViews(): Promise<SensorView[]> {
    const sensorsView: SensorView[] = []

    const sensorBaseInfoList = await SatelliteDataApi.getSensorList()

    sensorBaseInfoList.forEach(async (sensorBaseInfo) => {
        const detailInfo = await SatelliteDataApi.getSensorDetail(sensorBaseInfo.sensorId)

        const producs = await fetchProductViews(sensorBaseInfo.sensorId)

        let sensor: SensorView = {
            id: sensorBaseInfo.sensorId,
            name: sensorBaseInfo.sensorName,
            description: detailInfo.description,
            products: producs,
        }

        sensorsView.push(sensor)
    })

    sensorsView.sort((a, b) => a.products.length - b.products.length)

    return sensorsView

    // Testing
    // return new Promise((resolve) => {
    //     setTimeout(() => {
    //         resolve(sensorsView)
    //     }, 1000)
    // })
}

// 获取一个Product的Scene列表
export async function searchSceneViews(filterParams: ImageFilterCondition): Promise<SceneView[]> {
    return new Promise(async (resolve, reject) => {
        const sceneViews: SceneView[] = []
        const params = {
            sensorId: filterParams.product.sensorId,
            productId: filterParams.product.id,
            startTime: filterParams.dateRange[0]?.format('YYYY-MM-DD'),
            endTime: filterParams.dateRange[1]?.format('YYYY-MM-DD'),
            geometry: filterParams.geometry,
            // 'cloudCover': filterParams.cloudCover,
        }
        const sceneBaseInfoList = await SatelliteDataApi.searchSensorImage(params)

        const detailPromises = sceneBaseInfoList.features.map(async (sceneFeature) => {
            let sceneId = sceneFeature.id
            let sceneGeoFeature = sceneFeature.geometry
            let scenePreviewURL = await SatelliteDataApi.getSensorImagePreviewPng(sceneId)
            let sceneDetail = await SatelliteDataApi.getSensorImageDetail(sceneId)

            let sceneView: SceneView = {
                id: sceneId,
                productId: filterParams.product.id,
                sensorId: filterParams.product.sensorId,
                name: sceneDetail.sceneName,
                date: sceneDetail.sceneTime,
                cloudCover: parseFloat(sceneDetail.cloud),
                resolution: filterParams.product.resolution, // 从ProductView中获取
                preview_url: scenePreviewURL,
                geoFeature: sceneGeoFeature,
                bands: sceneDetail.bands,
                description: sceneDetail.description,
                tileLevelNum: sceneDetail.tileLevelNum,
                tileLevels: sceneDetail.tileLevels,
                crs: sceneDetail.crs,
            }
            sceneViews.push(sceneView)
        })

        await Promise.all(detailPromises)
        console.log('resolve : ', sceneViews)
        resolve(sceneViews)
    })
}

// 获取一个Scene的Band列表
export async function fetchBandViews(sceneId: string): Promise<BandView[]> {
    const bandViews: BandView[] = []

    const bandBaseInfoList = await SatelliteDataApi.getSensorImageBands(sceneId)

    bandBaseInfoList.forEach((bandBaseInfo) => {
        let bandView: BandView = {
            id: bandBaseInfo.imageId,
            name: bandBaseInfo.band,
            sceneId: sceneId,
        }
        bandViews.push(bandView)
    })

    return bandViews
}

// 获取一个Scene的Grid GeoJSON
export function getSceneGridGeoJsonURL(scene: SceneView) {
    // 现在只有一个等级,直接取第一等级
    const tileLevel = scene.tileLevels[0]
    const gridGeoJSON = SatelliteDataApi.getImageTilesGeojsonURL(scene.id, tileLevel)
    return gridGeoJSON
}

// 合并GridIDs 触发下载
export async function startMergeTiles(
    scene: SceneView,
    bands: string[],
    gridIds: string[],
): Promise<string> {
    let sceneId = scene.id
    let tileInfos: SatelliteDataApi.ImageTile.ImageTileTifMergeRequest['tiles'] = new Array(
        gridIds.length,
    )
    let promises = gridIds.map(async (gridId, index) => {
        let tileInfo = await SatelliteDataApi.getImageTileDetail(sceneId, gridId)
        tileInfos[index] = {
            columnId: tileInfo.columnId.toString(),
            rowId: tileInfo.rowId.toString(),
        }
    })
    await Promise.all(promises)
    let params: SatelliteDataApi.ImageTile.ImageTileTifMergeRequest = {
        sceneId: scene.id,
        tiles: tileInfos,
        bands: bands,
    }
    let result = await SatelliteDataApi.startMergeImageTiles(params)
    return result.data // return caseId
}
export async function mergingStatus(caseId: string) {
    let result = await SatelliteDataApi.getMergeImageTilesStatus(caseId)
    return result.data // return model status
}
export async function downloadMergeTiles(caseId: string, name?: string) {
    SatelliteDataApi.downloadMergeImageTilesResult(caseId, name)
}
// beautiful interval query merging status
export async function intervalQueryMergingStatus(
    caseId: string,
    statusCallback: (status: string) => void,
    completeCallback: () => void,
    errorCallback: () => void,
) {
    const interval = 1000
    return new Promise<void>((resolve, reject) => {
        const checkStatus = async () => {
            const status = await mergingStatus(caseId)
            statusCallback(status)
            if (status === 'COMPLETE') {
                completeCallback()
                resolve()
                return
            } else {
                if (status === 'ERROR') {
                    errorCallback()
                    reject('merging tiff status error')
                    return
                }
                setTimeout(checkStatus, interval)
            }
        }
        checkStatus()
    })
}
//// 上传选中的瓦片至已有项目
export async function uploadTilesToProject(
    sceneId: string,
    tileIds: string[],
    project: Project,
    successCallback: () => void,
    errorCallback: () => void,
): Promise<void> {
    return new Promise(async (resolve, reject) => {
        // open project
        let actionParams: SatelliteDataApi.Project.ProjectActionRequest = {
            projectId: project.projectId,
            userId: project.createUser,
            action: 'open',
        }
        const res = await SatelliteDataApi.operateProject(actionParams)

        if (res.status === 1) {
            let params: SatelliteDataApi.Project.ImageTileUploadToProjectRequest = {
                userId: project.createUser,
                projectId: project.projectId,
                sceneId: sceneId.toLowerCase(),
                tileIds: tileIds,
            }
            await SatelliteDataApi.uploadImageTilesToProject(params)
            successCallback()
            setTimeout(() => {
                let closeParams: SatelliteDataApi.Project.ProjectActionRequest = {
                    projectId: project.projectId,
                    userId: project.createUser,
                    action: 'close',
                }
                SatelliteDataApi.operateProject(closeParams)
            }, 5000)
            resolve()
        } else {
            errorCallback()
            reject('open project failed')
        }
    })
}


// 基于产品和行列号查询一个瓦片位置处的瓦片信息
export async function queryOverlapTileInfo(product: ProductView, gridId: string): Promise<OverlapTileInfoView[]> {
    const [x, y] = gridId.split('-')
    let params: SatelliteDataApi.ImageTile.ImageTileQueryRequest = {
        sensorId: product.sensorId,
        productId: product.id,
        tileLevel: '40031*20016', // 只有一个等级
        columnId: x,
        rowId: y,
        band: '1' // 为啥要波段呀我真服了
    }
    const res = await SatelliteDataApi.queryTileByXY(params)
    let overlapTileInfos: OverlapTileInfoView[] = []
    for (let i = 0; i < res.length; i++) {
        let tileInfo = res[i]
        let overlapTileInfo: OverlapTileInfoView = {
            tilerServer: tileInfo.tilerUrl,
            objectPath: tileInfo.object,
            tileId: tileInfo.tileId,
            sceneId: tileInfo.sceneId,
            cloud: tileInfo.cloud,
            columnId: x,
            rowId: y,
        }
        overlapTileInfos.push(overlapTileInfo)
    }
    return overlapTileInfos
}

// 给一个格网ID列表，返回一个Map，key为格网ID，value为瓦片信息列表
export async function queryOverlapTilesMap(product: ProductView, gridIDs: string[]): Promise<Map<string, OverlapTileInfoView[]>> {
    const map = new Map<string, OverlapTileInfoView[]>()

    let promises = gridIDs.map(async (gridID) => {
        let overlapTileInfos = await queryOverlapTileInfo(product, gridID)
        map.set(gridID, overlapTileInfos)
    })
    await Promise.all(promises)

    return map
}

// 给一个OverlapTileInfoView列表，查询景的时间范围, 云量范围
export async function statisticsOverlapTileInfoView(overlapTileInfos: OverlapTileInfoView[]): Promise<{timeRange: [string, string], cloudRange: [number, number]}> {
    let sceneTimeRange: [string, string] = ['', '']
    let minTime = new Date(2099, 11, 31)
    let maxTime = new Date(1900, 0, 1)
    let minCloud = 100
    let maxCloud = 0
    for (let i = 0; i < overlapTileInfos.length; i++) {
        let tileInfo = overlapTileInfos[i]
        let sceneId = tileInfo.sceneId
        let sceneDetail = await SatelliteDataApi.getSensorImageDetail(sceneId)
        let sceneTime = new Date(sceneDetail.sceneTime)
        if (sceneTime < minTime) {
            minTime = sceneTime
        }
        if (sceneTime > maxTime) {
            maxTime = sceneTime
        }
        let cloud = parseFloat(sceneDetail.cloud)
        if (cloud < minCloud) {
            minCloud = cloud
        }
        if (cloud > maxCloud) {
            maxCloud = cloud
        }
    }
    sceneTimeRange[0] = minTime.toLocaleDateString('zh-CN')
    sceneTimeRange[1] = maxTime.toLocaleDateString('zh-CN')
    return {
        timeRange: sceneTimeRange,
        cloudRange: [minCloud, maxCloud],
    }
}