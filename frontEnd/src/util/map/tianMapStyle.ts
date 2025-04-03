import { type StyleSpecification } from 'mapbox-gl'

const TianMapkey = '51d72ac2491e6e4228bdc5dd2e0a61b2'
const TianImageStyle = {
    version: 8,
    lights: [
        {
            id: 'ambient',
            type: 'ambient',
            properties: {
                color: [
                    'match',
                    ['config', 'lightPreset'],
                    'dawn',
                    'hsl(28, 98%, 93%)',
                    'day',
                    'hsl(0, 0%, 100%)',
                    'dusk',
                    'hsl(228, 27%, 29%)',
                    'night',
                    'hsl(217, 100%, 11%)',
                    'hsl(0, 0%, 100%)',
                ],
                intensity: ['match', ['config', 'lightPreset'], 'dawn', 0.75, 'day', 0.8, 'dusk', 0.8, 'night', 0.5, 0.8],
            },
        },
        {
            id: 'directional',
            type: 'directional',
            properties: {
                direction: [
                    'match',
                    ['config', 'lightPreset'],
                    'dawn',
                    ['literal', [120, 50]],
                    'day',
                    ['literal', [180, 20]],
                    'dusk',
                    ['literal', [240, 80]],
                    'night',
                    ['literal', [270, 20]],
                    ['literal', [180, 20]],
                ],
                color: [
                    'match',
                    ['config', 'lightPreset'],
                    'dawn',
                    'hsl(33, 98%, 77%)',
                    'day',
                    'hsl(0, 0%, 100%)',
                    'dusk',
                    'hsl(30, 98%, 76%)',
                    'night',
                    'hsl(0, 0%, 29%)',
                    'hsl(0, 0%, 100%)',
                ],
                intensity: [
                    'interpolate',
                    ['linear'],
                    ['zoom'],
                    12,
                    ['match', ['config', 'lightPreset'], 'dawn', 0.5, 'day', 0.2, 'dusk', 0, 'night', 0, 0.2],
                    14,
                    ['match', ['config', 'lightPreset'], 'dawn', 0.5, 'day', 0.2, 'dusk', 0.2, 'night', 0.5, 0.2],
                ],
                'cast-shadows': true,
                'shadow-intensity': ['match', ['config', 'lightPreset'], 'night', 0.5, 1],
            },
        },
    ],
    fog: {
        'vertical-range': [30, 120],
        range: ['interpolate', ['linear'], ['zoom'], 13, ['literal', [1, 20]], 15, ['literal', [1, 4]]],
        color: [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            ['interpolate', ['linear'], ['measure-light', 'brightness'], 0.1, 'hsla(240, 9%, 55%, 0.2)', 0.4, 'hsla(0, 0%, 100%, 0.2)'],
            7,
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.02,
                'hsla(213, 63%, 20%, 0.9)',
                0.03,
                'hsla(30, 65%, 60%, 0.5)',
                0.4,
                'hsla(10, 79%, 88%, 0.8)',
                0.45,
                'hsla(200, 60%, 98%, 0.9)',
            ],
        ],
        'high-color': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            ['interpolate', ['linear'], ['measure-light', 'brightness'], 0.1, 'hsla(215, 60%, 20%, 1)', 0.4, 'hsla(215, 100%, 80%, 1)'],
            7,
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0,
                'hsla(228, 38%, 20%, 1)',
                0.05,
                'hsla(360, 100%, 85%, 1)',
                0.2,
                'hsla(205, 88%, 86%, 1)',
                0.4,
                'hsla(270, 65%, 85%, 1)',
                0.45,
                'hsla(0, 0%, 100%, 1)',
            ],
        ],
        'space-color': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            'hsl(211, 84%, 0%)',
            7,
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0,
                'hsl(210, 84%, 5%)',
                0.2,
                'hsl(210, 40%, 30%)',
                0.4,
                'hsl(210, 45%, 30%)',
                0.45,
                'hsl(210, 100%, 80%)',
            ],
        ],
        'horizon-blend': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            0.004,
            7,
            ['interpolate', ['exponential', 1.2], ['measure-light', 'brightness'], 0.35, 0.03, 0.4, 0.07, 0.45, 0.03],
        ],
        'star-intensity': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            0.4,
            7,
            ['interpolate', ['exponential', 1.2], ['measure-light', 'brightness'], 0.1, 0.2, 0.3, 0],
        ],
    },
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
    ],
}

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
                'sky-gradient': ['interpolate', ['linear'], ['sky-radial-progress'], 0.7, 'rgba(125, 206, 255, 1)', 1, 'rgba(245, 244, 238, 1)'],
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
