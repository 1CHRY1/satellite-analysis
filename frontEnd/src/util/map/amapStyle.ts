export const amapStyle = {
    version: 8,
    sources: {
        // 1. 卫星影像源 (底层)
        'gaode-satellite': {
            type: 'raster',
            tiles: [
                'https://webst01.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}',
                'https://webst02.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}',
            ],
            tileSize: 256,
        },
        // 2. 路网与注记源 (透明层，style=8)
        'gaode-cva': {
            type: 'raster',
            tiles: [
                'https://webst01.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}',
                'https://webst02.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}',
            ],
            tileSize: 256,
        },
    },
    layers: [
        // 先添加卫星层 (在下面)
        {
            id: 'gaode-satellite-layer',
            type: 'raster',
            source: 'gaode-satellite',
            minzoom: 0,
            maxzoom: 22,
        },
        // 后添加注记层 (压在上面)
        {
            id: 'gaode-cva-layer',
            type: 'raster',
            source: 'gaode-cva',
            minzoom: 0,
            maxzoom: 22,
        },
    ],
}