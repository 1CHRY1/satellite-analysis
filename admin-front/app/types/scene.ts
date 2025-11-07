export interface Scene {
    sceneId: string
    productId: string
    sceneName: string
    sceneTime: string
    sensorId: string
    coordinateSystem: string
    boundingBox: any,
    description: string
    cloudPath: string
    bands: string[]
    bandNum: number
    bucket: string
    cloud: number
    tags: Tag
    noData: number
}

export interface Tag {
    source: "international" | "national"
    category: "ard" | "normal" | "traditional"
    production: "light" | "radar"
}