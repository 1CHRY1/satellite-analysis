import { type StyleSpecification } from 'mapbox-gl'
const TianMapkey = '51d72ac2491e6e4228bdc5dd2e0a61b2'
const TianVectorStyle = {
    version: 8,
    sources: {
        'Tian-Vectorlayer-Source': {
            type: 'raster',
            tiles: [
                `http://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TianMapkey}`,
            ],
            tileSize: 256,
        },
        'Tian-Vectorlable-Source': {
            type: 'raster',
            tiles: [
                `http://t0.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TianMapkey}`,
            ],
            tileSize: 256,
        },
    },
    layers: [
        {
            id: 'Tian-Vector-Layer',
            type: 'raster',
            source: 'Tian-Vectorlayer-Source',
        },
        {
            id: 'Tian-Vector-Label',
            type: 'raster',
            source: 'Tian-Vectorlable-Source',
            paint: {
                'raster-opacity': 0.8,
            },
        },
        {
            id: 'sky-layer',
            type: 'sky',
            paint: {
                'sky-type': 'gradient',
                'sky-gradient': [
                    'interpolate',
                    ['linear'],
                    ['sky-radial-progress'],
                    0.7,
                    'rgba(125, 206, 255, 1)',
                    1,
                    'rgba(245, 244, 238, 1)',
                ],
                'sky-gradient-center': [0, 0],
                'sky-gradient-radius': 90,
                'sky-opacity': 1,
            },
        },
    ],
}
const TianImageStyle = {
    version: 8,
    sources: {
        'Tian-Imagelayer-Source': {
            type: 'raster',
            tiles: [
                `http://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TianMapkey}`,
            ],
            tileSize: 256,
        },
        'Tian-Imagelable-Source': {
            type: 'raster',
            tiles: [
                `http://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TianMapkey}`,
            ],
            tileSize: 256,
        },
    },
    layers: [
        {
            id: 'Tian-Image-Layer',
            type: 'raster',
            source: 'Tian-Imagelayer-Source',
        },
        {
            id: 'Tian-Image-Label',
            type: 'raster',
            source: 'Tian-Imagelable-Source',
            paint: {
                'raster-opacity': 0.8,
            },
        },
        {
            id: 'sky-layer',
            type: 'sky',
            paint: {
                'sky-type': 'gradient',
                'sky-gradient': [
                    'interpolate',
                    ['linear'],
                    ['sky-radial-progress'],
                    0.7,
                    'rgba(125, 206, 255, 1)',
                    1,
                    'rgba(114, 128, 98, 1)',
                ],
                'sky-gradient-center': [0, 0],
                'sky-gradient-radius': 90,
                'sky-opacity': 1,
            },
        },
    ],
}

export type Style = 'image' | 'vector'

export const StyleMap = {
    image: TianImageStyle as unknown as StyleSpecification,
    vector: TianVectorStyle as unknown as StyleSpecification,
}
