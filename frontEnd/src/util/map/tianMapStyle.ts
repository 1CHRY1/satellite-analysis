import { type StyleSpecification } from 'mapbox-gl'
import { ezStore } from '@/store'

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
                intensity: [
                    'match',
                    ['config', 'lightPreset'],
                    'dawn',
                    0.75,
                    'day',
                    0.8,
                    'dusk',
                    0.8,
                    'night',
                    0.5,
                    0.8,
                ],
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
                    [
                        'match',
                        ['config', 'lightPreset'],
                        'dawn',
                        0.5,
                        'day',
                        0.2,
                        'dusk',
                        0,
                        'night',
                        0,
                        0.2,
                    ],
                    14,
                    [
                        'match',
                        ['config', 'lightPreset'],
                        'dawn',
                        0.5,
                        'day',
                        0.2,
                        'dusk',
                        0.2,
                        'night',
                        0.5,
                        0.2,
                    ],
                ],
                'cast-shadows': true,
                'shadow-intensity': ['match', ['config', 'lightPreset'], 'night', 0.5, 1],
            },
        },
    ],
    fog: {
        'vertical-range': [30, 120],
        range: [
            'interpolate',
            ['linear'],
            ['zoom'],
            13,
            ['literal', [1, 20]],
            15,
            ['literal', [1, 4]],
        ],
        color: [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.1,
                'hsla(240, 9%, 55%, 0.2)',
                0.4,
                'hsla(0, 0%, 100%, 0.2)',
            ],
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
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.1,
                'hsla(215, 60%, 20%, 1)',
                0.4,
                'hsla(215, 100%, 80%, 1)',
            ],
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
            [
                'interpolate',
                ['exponential', 1.2],
                ['measure-light', 'brightness'],
                0.35,
                0.03,
                0.4,
                0.07,
                0.45,
                0.03,
            ],
        ],
        'star-intensity': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            0.4,
            7,
            [
                'interpolate',
                ['exponential', 1.2],
                ['measure-light', 'brightness'],
                0.1,
                0.2,
                0.3,
                0,
            ],
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

//################################################################

// 内网影像风格底图
const LocalImageBaseMapStyle = {
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
                intensity: [
                    'match',
                    ['config', 'lightPreset'],
                    'dawn',
                    0.75,
                    'day',
                    0.8,
                    'dusk',
                    0.8,
                    'night',
                    0.5,
                    0.8,
                ],
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
                    [
                        'match',
                        ['config', 'lightPreset'],
                        'dawn',
                        0.5,
                        'day',
                        0.2,
                        'dusk',
                        0,
                        'night',
                        0,
                        0.2,
                    ],
                    14,
                    [
                        'match',
                        ['config', 'lightPreset'],
                        'dawn',
                        0.5,
                        'day',
                        0.2,
                        'dusk',
                        0.2,
                        'night',
                        0.5,
                        0.2,
                    ],
                ],
                'cast-shadows': true,
                'shadow-intensity': ['match', ['config', 'lightPreset'], 'night', 0.5, 1],
            },
        },
    ],
    fog: {
        'vertical-range': [30, 120],
        range: [
            'interpolate',
            ['linear'],
            ['zoom'],
            13,
            ['literal', [1, 20]],
            15,
            ['literal', [1, 4]],
        ],
        color: [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.1,
                'hsla(240, 9%, 55%, 0.2)',
                0.4,
                'hsla(0, 0%, 100%, 0.2)',
            ],
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
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.1,
                'hsla(215, 60%, 20%, 1)',
                0.4,
                'hsla(215, 100%, 80%, 1)',
            ],
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
            [
                'interpolate',
                ['exponential', 1.2],
                ['measure-light', 'brightness'],
                0.35,
                0.03,
                0.4,
                0.07,
                0.45,
                0.03,
            ],
        ],
        'star-intensity': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            0.4,
            7,
            [
                'interpolate',
                ['exponential', 1.2],
                ['measure-light', 'brightness'],
                0.1,
                0.2,
                0.3,
                0,
            ],
        ],
    },
    sources: {
        'Local-Imagelayer-Source': {
            type: 'raster',
            tiles: [
                `http://${window.location.host}` + ezStore.get('conf')['intranet_img_url']
            ],
            tileSize: 256,
        },
        'Local-Interal-Source': {
            type: 'raster',
            tiles: [
                `http://${window.location.host}` + ezStore.get('conf')['fk_url']
            ],
            tileSize: 256,
        },
    },
    layers: [
        {
            id: 'Local-Image-Layer',
            type: 'raster',
            source: 'Local-Imagelayer-Source',
        },
        {
            id: 'Local-Interal-Layer',
            type: 'raster',
            source: 'Local-Interal-Source',
        },
    ],
    glyphs: `http://${window.location.host}/glyphs/mapbox/{fontstack}/{range}.pbf`,
    "sprite": `http://${window.location.host}/sprite`,
}
// 内网矢量风格底图
const LocalVectorBaseMapStyle = {
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
                intensity: [
                    'match',
                    ['config', 'lightPreset'],
                    'dawn',
                    0.75,
                    'day',
                    0.8,
                    'dusk',
                    0.8,
                    'night',
                    0.5,
                    0.8,
                ],
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
                    [
                        'match',
                        ['config', 'lightPreset'],
                        'dawn',
                        0.5,
                        'day',
                        0.2,
                        'dusk',
                        0,
                        'night',
                        0,
                        0.2,
                    ],
                    14,
                    [
                        'match',
                        ['config', 'lightPreset'],
                        'dawn',
                        0.5,
                        'day',
                        0.2,
                        'dusk',
                        0.2,
                        'night',
                        0.5,
                        0.2,
                    ],
                ],
                'cast-shadows': true,
                'shadow-intensity': ['match', ['config', 'lightPreset'], 'night', 0.5, 1],
            },
        },
    ],
    fog: {
        'vertical-range': [30, 120],
        range: [
            'interpolate',
            ['linear'],
            ['zoom'],
            13,
            ['literal', [1, 20]],
            15,
            ['literal', [1, 4]],
        ],
        color: [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.1,
                'hsla(240, 9%, 55%, 0.2)',
                0.4,
                'hsla(0, 0%, 100%, 0.2)',
            ],
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
            [
                'interpolate',
                ['linear'],
                ['measure-light', 'brightness'],
                0.1,
                'hsla(215, 60%, 20%, 1)',
                0.4,
                'hsla(215, 100%, 80%, 1)',
            ],
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
            [
                'interpolate',
                ['exponential', 1.2],
                ['measure-light', 'brightness'],
                0.35,
                0.03,
                0.4,
                0.07,
                0.45,
                0.03,
            ],
        ],
        'star-intensity': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            5,
            0.4,
            7,
            [
                'interpolate',
                ['exponential', 1.2],
                ['measure-light', 'brightness'],
                0.1,
                0.2,
                0.3,
                0,
            ],
        ],
    },
    "sources": {
        "openmaptiles": {
            "type": "vector",
            "tiles": [
                `http://${window.location.host}` + ezStore.get('conf')['intranet_mvt_url']
            ],
            "maxzoom": 14,
            "minzoom": 0
        }
    },
    "sprite": `http://${window.location.host}/sprite`,
    "glyphs": `http://${window.location.host}/glyphs/{fontstack}/{range}.pbf`,
    "layers": [
        {
            "id": "background",
            "type": "background",
            "paint": { "background-color": "hsl(47, 26%, 88%)" }
        },
        {
            "id": "landuse-residential",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landuse",
            "filter": [
                "all",
                ["==", "$type", "Polygon"],
                ["in", "class", "residential", "suburb", "neighbourhood"]
            ],
            "layout": { "visibility": "visible" },
            "paint": { "fill-color": "hsl(47, 13%, 86%)", "fill-opacity": 0.7 }
        },
        {
            "id": "landcover_grass",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landcover",
            "filter": ["==", "class", "grass"],
            "paint": { "fill-color": "hsl(82, 46%, 72%)", "fill-opacity": 0.45 }
        },
        {
            "id": "landcover_wood",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landcover",
            "filter": ["==", "class", "wood"],
            "paint": {
                "fill-color": "hsl(82, 46%, 72%)",
                "fill-opacity": { "base": 1, "stops": [[8, 0.6], [22, 1]] }
            }
        },
        {
            "id": "water",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "water",
            "filter": [
                "all",
                ["==", "$type", "Polygon"],
                ["!=", "intermittent", 1],
                ["!=", "brunnel", "tunnel"]
            ],
            "layout": { "visibility": "visible" },
            "paint": { "fill-color": "hsl(205, 56%, 73%)" }
        },
        {
            "id": "water_intermittent",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "water",
            "filter": ["all", ["==", "$type", "Polygon"], ["==", "intermittent", 1]],
            "layout": { "visibility": "visible" },
            "paint": { "fill-color": "hsl(205, 56%, 73%)", "fill-opacity": 0.7 }
        },
        {
            "id": "landcover-ice-shelf",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landcover",
            "filter": ["==", "subclass", "ice_shelf"],
            "layout": { "visibility": "visible" },
            "paint": { "fill-color": "hsl(47, 26%, 88%)", "fill-opacity": 0.8 }
        },
        {
            "id": "landcover-glacier",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landcover",
            "filter": ["==", "subclass", "glacier"],
            "layout": { "visibility": "visible" },
            "paint": {
                "fill-color": "hsl(47, 22%, 94%)",
                "fill-opacity": { "base": 1, "stops": [[0, 1], [8, 0.5]] }
            }
        },
        {
            "id": "landcover_sand",
            "type": "fill",
            "metadata": {},
            "source": "openmaptiles",
            "source-layer": "landcover",
            "filter": ["all", ["in", "class", "sand"]],
            "paint": {
                "fill-antialias": false,
                "fill-color": "rgba(232, 214, 38, 1)",
                "fill-opacity": 0.3
            }
        },
        {
            "id": "landuse",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landuse",
            "filter": ["==", "class", "agriculture"],
            "layout": { "visibility": "visible" },
            "paint": { "fill-color": "#eae0d0" }
        },
        {
            "id": "landuse_overlay_national_park",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "landcover",
            "filter": ["==", "class", "national_park"],
            "paint": {
                "fill-color": "#E1EBB0",
                "fill-opacity": { "base": 1, "stops": [[5, 0], [9, 0.75]] }
            }
        },
        {
            "id": "waterway-tunnel",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "waterway",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "tunnel"]
            ],
            "layout": { "visibility": "visible" },
            "paint": {
                "line-color": "hsl(205, 56%, 73%)",
                "line-dasharray": [3, 3],
                "line-gap-width": { "stops": [[12, 0], [20, 6]] },
                "line-opacity": 1,
                "line-width": { "base": 1.4, "stops": [[8, 1], [20, 2]] }
            }
        },
        {
            "id": "waterway",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "waterway",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["!in", "brunnel", "tunnel", "bridge"],
                ["!=", "intermittent", 1]
            ],
            "layout": { "visibility": "visible" },
            "paint": {
                "line-color": "hsl(205, 56%, 73%)",
                "line-opacity": 1,
                "line-width": { "base": 1.4, "stops": [[8, 1], [20, 8]] }
            }
        },
        {
            "id": "waterway_intermittent",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "waterway",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["!in", "brunnel", "tunnel", "bridge"],
                ["==", "intermittent", 1]
            ],
            "layout": { "visibility": "visible" },
            "paint": {
                "line-color": "hsl(205, 56%, 73%)",
                "line-dasharray": [2, 1],
                "line-opacity": 1,
                "line-width": { "base": 1.4, "stops": [[8, 1], [20, 8]] }
            }
        },
        {
            "id": "tunnel_railway_transit",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "minzoom": 0,
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "tunnel"],
                ["==", "class", "transit"]
            ],
            "layout": { "line-cap": "butt", "line-join": "miter" },
            "paint": {
                "line-color": "hsl(34, 12%, 66%)",
                "line-dasharray": [3, 3],
                "line-opacity": { "base": 1, "stops": [[11, 0], [16, 1]] }
            }
        },
        {
            "id": "building",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "building",
            "paint": {
                "fill-antialias": true,
                "fill-color": "rgba(222, 211, 190, 1)",
                "fill-opacity": { "base": 1, "stops": [[13, 0], [15, 1]] },
                "fill-outline-color": {
                    "stops": [
                        [15, "rgba(212, 177, 146, 0)"],
                        [16, "rgba(212, 177, 146, 0.5)"]
                    ]
                }
            }
        },
        {
            "id": "housenumber",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "housenumber",
            "minzoom": 17,
            "filter": ["==", "$type", "Point"],
            "layout": {
                "text-field": "{housenumber}",
                "text-font": ["Open Sans Semibold"],
                "text-size": 10
            },
            "paint": { "text-color": "rgba(212, 177, 146, 1)" }
        },
        {
            "id": "road_area_pier",
            "type": "fill",
            "metadata": {},
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": ["all", ["==", "$type", "Polygon"], ["==", "class", "pier"]],
            "layout": { "visibility": "visible" },
            "paint": { "fill-antialias": true, "fill-color": "hsl(47, 26%, 88%)" }
        },
        {
            "id": "road_pier",
            "type": "line",
            "metadata": {},
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": ["all", ["==", "$type", "LineString"], ["in", "class", "pier"]],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "hsl(47, 26%, 88%)",
                "line-width": { "base": 1.2, "stops": [[15, 1], [17, 4]] }
            }
        },
        {
            "id": "road_bridge_area",
            "type": "fill",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "Polygon"],
                ["in", "brunnel", "bridge"]
            ],
            "layout": {},
            "paint": { "fill-color": "hsl(47, 26%, 88%)", "fill-opacity": 0.5 }
        },
        {
            "id": "road_path",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["in", "class", "path", "track"]
            ],
            "layout": { "line-cap": "square", "line-join": "bevel" },
            "paint": {
                "line-color": "hsl(0, 0%, 97%)",
                "line-dasharray": [1, 1],
                "line-width": { "base": 1.55, "stops": [[4, 0.25], [20, 10]] }
            }
        },
        {
            "id": "road_minor",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "minzoom": 13,
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["in", "class", "minor", "service"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "hsl(0, 0%, 97%)",
                "line-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] }
            }
        },
        {
            "id": "tunnel_minor",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "tunnel"],
                ["==", "class", "minor_road"]
            ],
            "layout": { "line-cap": "butt", "line-join": "miter" },
            "paint": {
                "line-color": "#efefef",
                "line-dasharray": [0.36, 0.18],
                "line-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] }
            }
        },
        {
            "id": "tunnel_major",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "tunnel"],
                ["in", "class", "primary", "secondary", "tertiary", "trunk"]
            ],
            "layout": { "line-cap": "butt", "line-join": "miter" },
            "paint": {
                "line-color": "#fff",
                "line-dasharray": [0.28, 0.14],
                "line-width": { "base": 1.4, "stops": [[6, 0.5], [20, 30]] }
            }
        },
        {
            "id": "aeroway-area",
            "type": "fill",
            "metadata": { "mapbox:group": "1444849345966.4436" },
            "source": "openmaptiles",
            "source-layer": "aeroway",
            "minzoom": 4,
            "filter": [
                "all",
                ["==", "$type", "Polygon"],
                ["in", "class", "runway", "taxiway"]
            ],
            "layout": { "visibility": "visible" },
            "paint": {
                "fill-color": "rgba(255, 255, 255, 1)",
                "fill-opacity": { "base": 1, "stops": [[13, 0], [14, 1]] }
            }
        },
        {
            "id": "aeroway-taxiway",
            "type": "line",
            "metadata": { "mapbox:group": "1444849345966.4436" },
            "source": "openmaptiles",
            "source-layer": "aeroway",
            "minzoom": 12,
            "filter": [
                "all",
                ["in", "class", "taxiway"],
                ["==", "$type", "LineString"]
            ],
            "layout": {
                "line-cap": "round",
                "line-join": "round",
                "visibility": "visible"
            },
            "paint": {
                "line-color": "rgba(255, 255, 255, 1)",
                "line-opacity": 1,
                "line-width": { "base": 1.5, "stops": [[12, 1], [17, 10]] }
            }
        },
        {
            "id": "aeroway-runway",
            "type": "line",
            "metadata": { "mapbox:group": "1444849345966.4436" },
            "source": "openmaptiles",
            "source-layer": "aeroway",
            "minzoom": 4,
            "filter": [
                "all",
                ["in", "class", "runway"],
                ["==", "$type", "LineString"]
            ],
            "layout": {
                "line-cap": "round",
                "line-join": "round",
                "visibility": "visible"
            },
            "paint": {
                "line-color": "rgba(255, 255, 255, 1)",
                "line-opacity": 1,
                "line-width": { "base": 1.5, "stops": [[11, 4], [17, 50]] }
            }
        },
        {
            "id": "road_trunk_primary",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["in", "class", "trunk", "primary"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "#fff",
                "line-width": { "base": 1.4, "stops": [[6, 0.5], [20, 30]] }
            }
        },
        {
            "id": "road_secondary_tertiary",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["in", "class", "secondary", "tertiary"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "#fff",
                "line-width": { "base": 1.4, "stops": [[6, 0.5], [20, 20]] }
            }
        },
        {
            "id": "road_major_motorway",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "class", "motorway"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "hsl(0, 0%, 100%)",
                "line-offset": 0,
                "line-width": { "base": 1.4, "stops": [[8, 1], [16, 10]] }
            }
        },
        {
            "id": "railway-transit",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "class", "transit"],
                ["!=", "brunnel", "tunnel"]
            ],
            "layout": { "visibility": "visible" },
            "paint": {
                "line-color": "hsl(34, 12%, 66%)",
                "line-opacity": { "base": 1, "stops": [[11, 0], [16, 1]] }
            }
        },
        {
            "id": "railway",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": ["==", "class", "rail"],
            "layout": { "visibility": "visible" },
            "paint": {
                "line-color": "hsl(34, 12%, 66%)",
                "line-opacity": { "base": 1, "stops": [[11, 0], [16, 1]] }
            }
        },
        {
            "id": "waterway-bridge-case",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "waterway",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "bridge"]
            ],
            "layout": { "line-cap": "butt", "line-join": "miter" },
            "paint": {
                "line-color": "#bbbbbb",
                "line-gap-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] },
                "line-width": { "base": 1.6, "stops": [[12, 0.5], [20, 10]] }
            }
        },
        {
            "id": "waterway-bridge",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "waterway",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "bridge"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "hsl(205, 56%, 73%)",
                "line-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] }
            }
        },
        {
            "id": "bridge_minor case",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "bridge"],
                ["==", "class", "minor_road"]
            ],
            "layout": { "line-cap": "butt", "line-join": "miter" },
            "paint": {
                "line-color": "#dedede",
                "line-gap-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] },
                "line-width": { "base": 1.6, "stops": [[12, 0.5], [20, 10]] }
            }
        },
        {
            "id": "bridge_major case",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "bridge"],
                ["in", "class", "primary", "secondary", "tertiary", "trunk"]
            ],
            "layout": { "line-cap": "butt", "line-join": "miter" },
            "paint": {
                "line-color": "#dedede",
                "line-gap-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] },
                "line-width": { "base": 1.6, "stops": [[12, 0.5], [20, 10]] }
            }
        },
        {
            "id": "bridge_minor",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "bridge"],
                ["==", "class", "minor_road"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "#efefef",
                "line-width": { "base": 1.55, "stops": [[4, 0.25], [20, 30]] }
            }
        },
        {
            "id": "bridge_major",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "transportation",
            "filter": [
                "all",
                ["==", "$type", "LineString"],
                ["==", "brunnel", "bridge"],
                ["in", "class", "primary", "secondary", "tertiary", "trunk"]
            ],
            "layout": { "line-cap": "round", "line-join": "round" },
            "paint": {
                "line-color": "#fff",
                "line-width": { "base": 1.4, "stops": [[6, 0.5], [20, 30]] }
            }
        },
        {
            "id": "admin_sub",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "filter": ["in", "admin_level", 4, 6, 8],
            "layout": { "visibility": "visible" },
            "paint": { "line-color": "hsla(0, 0%, 60%, 0.5)", "line-dasharray": [2, 1] }
        },
        {
            "id": "admin_country_z0-4",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "minzoom": 0,
            "maxzoom": 5,
            "filter": [
                "all",
                ["<=", "admin_level", 2],
                ["==", "$type", "LineString"],
                ["!has", "claimed_by"]
            ],
            "layout": {
                "line-cap": "round",
                "line-join": "round",
                "visibility": "visible"
            },
            "paint": {
                "line-color": "hsl(0, 0%, 100%)",
                "line-opacity": 0.0,
                "line-width": { "base": 1.3, "stops": [[3, 0.5], [22, 15]] }
            }
        },
        {
            "id": "admin_country_z5-",
            "type": "line",
            "source": "openmaptiles",
            "source-layer": "boundary",
            "minzoom": 5,
            "filter": [
                "all",
                ["<=", "admin_level", 2],
                ["==", "$type", "LineString"]
            ],
            "layout": {
                "line-cap": "round",
                "line-join": "round",
                "visibility": "visible"
            },
            "paint": {
                "line-color": "hsl(0, 0%, 60%)",
                "line-opacity": 0.0,
                "line-width": { "base": 1.3, "stops": [[3, 0.5], [22, 15]] }
            }
        },
        {
            "id": "poi_label",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "poi",
            "minzoom": 14,
            "filter": ["all", ["==", "$type", "Point"], ["==", "rank", 1]],
            "layout": {
                "icon-size": 1,
                "text-anchor": "top",
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-max-width": 8,
                "text-offset": [0, 0.5],
                "text-size": 11,
                "visibility": "visible"
            },
            "paint": {
                "text-color": "#666",
                "text-halo-blur": 1,
                "text-halo-color": "rgba(255,255,255,0.75)",
                "text-halo-width": 1
            }
        },
        {
            "id": "airport-label",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "aerodrome_label",
            "minzoom": 10,
            "filter": ["all", ["has", "iata"]],
            "layout": {
                "icon-size": 1,
                "text-anchor": "top",
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-max-width": 8,
                "text-offset": [0, 0.5],
                "text-size": 11,
                "visibility": "visible"
            },
            "paint": {
                "text-color": "#666",
                "text-halo-blur": 1,
                "text-halo-color": "rgba(255,255,255,0.75)",
                "text-halo-width": 1
            }
        },
        {
            "id": "road_major_label",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "transportation_name",
            "minzoom": 13,
            "filter": ["==", "$type", "LineString"],
            "layout": {
                "symbol-placement": "line",
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-letter-spacing": 0.1,
                "text-rotation-alignment": "map",
                "text-size": { "base": 1.4, "stops": [[10, 8], [20, 14]] },
                "text-transform": "uppercase",
                "visibility": "visible"
            },
            "paint": {
                "text-color": "#444",
                "text-halo-color": "hsl(0, 0%, 100%)",
                "text-halo-width": 2
            }
        },
        {
            "id": "place_label_other",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "place",
            "minzoom": 8,
            "filter": [
                "all",
                ["==", "$type", "Point"],
                ["!in", "class", "city", "state", "country", "continent"]
            ],
            "layout": {
                "text-anchor": "center",
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-max-width": 6,
                "text-size": { "stops": [[6, 10], [12, 14]] },
                "visibility": "visible"
            },
            "paint": {
                "text-color": "#333",
                "text-halo-blur": 0,
                "text-halo-color": "hsl(0, 0%, 100%)",
                "text-halo-width": 2
            }
        },
        {
            "id": "place_label_city",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "place",
            "maxzoom": 16,
            "filter": ["all", ["==", "$type", "Point"], ["==", "class", "city"]],
            "layout": {
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-max-width": 10,
                "text-size": { "stops": [[3, 12], [8, 16]] }
            },
            "paint": {
                "text-color": "#333",
                "text-halo-blur": 0,
                "text-halo-color": "hsla(0, 0%, 100%, 0.75)",
                "text-halo-width": 2
            }
        },
        {
            "id": "country_label-other",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "place",
            "maxzoom": 12,
            "filter": [
                "all",
                ["==", "$type", "Point"],
                ["==", "class", "country"],
                ["!has", "iso_a2"]
            ],
            "layout": {
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-max-width": 10,
                "text-size": { "stops": [[3, 12], [8, 22]] },
                "visibility": "visible"
            },
            "paint": {
                "text-color": "#333",
                "text-halo-blur": 0,
                "text-halo-color": "rgba(255,255,255,0.75)",
                "text-halo-width": 2
            }
        },
        {
            "id": "country_label",
            "type": "symbol",
            "source": "openmaptiles",
            "source-layer": "place",
            "maxzoom": 12,
            "filter": [
                "all",
                ["==", "$type", "Point"],
                ["==", "class", "country"],
                ["has", "iso_a2"]
            ],
            "layout": {
                "text-field": "{name:zh}",
                "text-font": ["Open Sans Semibold"],
                "text-max-width": 10,
                "text-size": { "stops": [[3, 12], [8, 22]] },
                "visibility": "visible"
            },
            "paint": {
                "text-color": "#333",
                "text-halo-blur": 0,
                "text-halo-color": "rgba(255,255,255,0.75)",
                "text-halo-width": 2
            }
        }
    ],
}

// 我们自己的矢量瓦片底图
const OurVectorBaseMapStyle = {
    version: 8,
    sources: {
        offlineMapTiles: {
            type: 'vector',
            tiles: [ `http://${window.location.host}` + ezStore.get('conf')['intranet_mvt_url']],
            minzoom: 0,
            maxzoom: 22,
        },
    },
    layers: [
        {
            id: 'park_polygon',
            type: 'fill',
            source: 'offlineMapTiles',
            minzoom: 0,
            maxzoom: 22,
            'source-layer': 'park',
            layout: {
                visibility: 'visible',
            },
            paint: {
                'fill-color': 'hsl(204, 0%, 100%)',
                'fill-opacity': 1.0,
            },
            filter: ['==', '$type', 'Polygon'],
        },
        {
            id: 'transportation_line',
            type: 'line',
            source: 'offlineMapTiles',
            minzoom: 0,
            maxzoom: 22,
            'source-layer': 'transportation',
            layout: {
                visibility: 'visible',
            },
            paint: {
                'line-color': 'hsl(207, 84%, 73%)',
                'line-width': 1,
                'line-opacity': 1.0,
            },
            filter: [
                'all',
                ['==', ['geometry-type'], 'LineString'],
                [
                    'all',
                    ['!', ['has', 'access']],
                    [
                        'match',
                        ['get', 'class'],
                        [
                            'primary',
                            'primary_construction',
                            'secondary',
                            'secondary_construction',
                            'tertiary',
                        ],
                        true,
                        false,
                    ],
                ],
            ],
        },
        {
            id: 'water_polygon',
            type: 'fill',
            source: 'offlineMapTiles',
            'source-layer': 'water',
            minzoom: 0,
            maxzoom: 22,
            layout: {
                visibility: 'visible',
            },
            paint: {
                'fill-color': 'rgb(174,197,238)',
                'fill-opacity': 1,
            },
            filter: ['==', '$type', 'Polygon'],
        },
    ],
    glyphs: '/glyphs/mapbox/{fontstack}/{range}.pbf',
}

const empty = {
    version: 8,
    sources: {},
    layers: [],
    glyphs: '/glyphs/mapbox/{fontstack}/{range}.pbf',
}

export type Style = 'image' | 'vector' | 'local' | 'empty'

export const StyleMap = {
    image: TianImageStyle as unknown as StyleSpecification,
    vector: TianVectorStyle as unknown as StyleSpecification,
    local: LocalImageBaseMapStyle as unknown as StyleSpecification,

    localVec: LocalVectorBaseMapStyle as unknown as StyleSpecification,
    localImg: LocalImageBaseMapStyle as unknown as StyleSpecification,
    localMvt: OurVectorBaseMapStyle as unknown as StyleSpecification,
    empty: empty as unknown as StyleSpecification,
}
