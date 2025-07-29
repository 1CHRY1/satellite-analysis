import { ezStore } from "@/store"

const titilerProxyEndPoint = ezStore.get('conf')['titiler']
const minioEndPoint = ezStore.get('conf')['minioIpAndPort']

export const getImageUrl = (sensorName: string) => {
    let baseUrl = `${titilerProxyEndPoint}/image_visualization/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('sensorName', sensorName)
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log(fullUrl)
    return fullUrl
}