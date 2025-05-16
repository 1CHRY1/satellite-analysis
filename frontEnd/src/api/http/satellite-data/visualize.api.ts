import { ezStore } from "@/store";
import { grid2bbox } from "@/util/map/gridMaker";

const titilerEndPoint = ezStore.get('conf')['titiler']
const minioEndPoint = ezStore.get('conf')['minioIpAndPort']

type GridImageParams = {
    rowId: number
    columnId: number
    resolution: number
    tifFullPath: string
}

// 返回可以作为layer source 的 url
export async function getGridImage(params: GridImageParams): Promise<string> {


    const statisticsJson = await getImgStatistics(params.tifFullPath)
    console.log(statisticsJson)

    const percentile_2 = statisticsJson.b1 ? statisticsJson.b1.min : 0;
    const percentile_98 = statisticsJson.b1 ? statisticsJson.b1.max : 20000;


    const bbox = grid2bbox(params.columnId, params.rowId, params.resolution)
    let url = `${titilerEndPoint}/bbox/${bbox.join(',')}.png`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + params.tifFullPath)
    requestParams.append('rescale', percentile_2 + ',' + percentile_98)
    url += '?' + requestParams.toString()

    return url

    // const response = await fetch(url)
    // const blob = await response.blob()

    // return URL.createObjectURL(blob)
}

const a = {
    "type": "FeatureCollection",
    "features": [
        {
            "type": "Feature",
            "properties": {},
            "geometry": {
                "coordinates": [
                    [
                        [
                            121.11627935820417,
                            31.87272629441364
                        ],
                        [
                            121.07374073491752,
                            31.379215343685047
                        ],
                        [
                            121.83234618352827,
                            31.501704907155386
                        ],
                        [
                            121.82702885561855,
                            31.844123070575307
                        ],
                        [
                            121.11627935820417,
                            31.87272629441364
                        ]
                    ]
                ],
                "type": "Polygon"
            }
        }
    ]
}
export async function getImgStatistics(tifFullPath: string): Promise<any> {

    let url = `${titilerEndPoint}/statistics`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + tifFullPath)
    url += '?' + requestParams.toString()

    const response = await fetch(url)
    const json = await response.json()

    return json
}

function makeBBox2Geojson(bbox: number[]) {
    // return {
    //     "type": "FeatureCollection",
    //     "features": [
    //         {
    //             "type": "Feature",
    //             "properties": {},
    //             "geometry": {
    //                 "coordinates": [
    //                     [
    //                         [
    //                             bbox[0],
    //                             bbox[1]
    //                         ],
    //                         [
    //                             bbox[2],
    //                             bbox[1]
    //                         ],
    //                         [
    //                             bbox[2],
    //                             bbox[3]
    //                         ],
    //                         [
    //                             bbox[0],
    //                             bbox[3]
    //                         ],
    //                         [
    //                             bbox[0],
    //                             bbox[1]
    //                         ]
    //                     ]
    //                 ],
    //                 "type": "Polygon"
    //             }
    //         }
    //     ]
    // }
    return {
        "type": "Feature",
        "properties": {},
        "geometry": {
            "coordinates": [
                [
                    [
                        bbox[0],
                        bbox[1]
                    ],
                    [
                        bbox[2],
                        bbox[1]
                    ],
                    [
                        bbox[2],
                        bbox[3]
                    ],
                    [
                        bbox[0],
                        bbox[3]
                    ],
                    [
                        bbox[0],
                        bbox[1]
                    ]
                ]
            ],
            "type": "Polygon"
        }
    }
}