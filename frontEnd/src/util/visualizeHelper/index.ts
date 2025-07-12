import { getTifbandMinMax } from "@/api/http/satellite-data/visualize.api"
import { ezStore } from "@/store"

type RGBTileLayerParams = {
    redPath: string
    greenPath: string
    bluePath: string
    r_min: number
    r_max: number
    g_min: number
    g_max: number
    b_min: number
    b_max: number
    nodata: number,
    gridsBoundary: any
}

const getRGBTileLayerParamFromSceneObject = async (scene, gridsBoundary?: any): Promise<RGBTileLayerParams> => {

    // const bandMapper = scene.bandMapper;
    const nodata = scene.noData
    const partiallyOverlapped = scene?.partiallyOverlapped
    let redPath = '', greenPath = '', bluePath = ''
    for (let img of scene.images) {

        if (img.band === scene.bandMapper['Red']) {
            redPath = img.bucket + '/' + img.tifPath
        }
        if (img.band === scene.bandMapper['Green']) {
            greenPath = img.bucket + '/' + img.tifPath
        }
        if (img.band === scene.bandMapper['Blue']) {
            bluePath = img.bucket + '/' + img.tifPath
        }
    }

    const cache = ezStore.get('statisticCache')
    const promises: any = []
    let [r_min, r_max, g_min, g_max, b_min, b_max] = [0, 0, 0, 0, 0, 0]

    if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
        console.log('cache hit!')
            ;[r_min, r_max] = cache.get(redPath)
            ;[g_min, g_max] = cache.get(greenPath)
            ;[b_min, b_max] = cache.get(bluePath)
    } else {
        promises.push(
            getTifbandMinMax(redPath),
            getTifbandMinMax(greenPath),
            getTifbandMinMax(bluePath),
        )
        await Promise.all(promises).then((values) => {
            r_min = values[0][0]
            r_max = values[0][1]
            g_min = values[1][0]
            g_max = values[1][1]
            b_min = values[2][0]
            b_max = values[2][1]
        })

        cache.set(redPath, [r_min, r_max])
        cache.set(greenPath, [g_min, g_max])
        cache.set(bluePath, [b_min, b_max])
    }

    return {
        redPath,
        greenPath,
        bluePath,
        r_min,
        r_max,
        g_min,
        g_max,
        b_min,
        b_max,
        nodata,
        gridsBoundary: partiallyOverlapped ? gridsBoundary : null,
    }
}


const getTerrainParamFromSceneObject = async (scene) => { 
    const terrainImg = scene.images[0]
    const terrainTifPath = terrainImg.bucket + '/' + terrainImg.tifPath
    return {
        terrainTifPath
    }
}

const getOneBandColorParamFromSceneObject = async (scene) => { 
    const oneBandImage = scene.images[0]
    const fullTifPath = oneBandImage.bucket + '/' + oneBandImage.tifPath
    return {
        fullTifPath
    }
}



export {
    getRGBTileLayerParamFromSceneObject,
    getTerrainParamFromSceneObject,
    getOneBandColorParamFromSceneObject
}