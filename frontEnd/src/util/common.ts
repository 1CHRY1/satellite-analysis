import { type polygonGeometry } from '@/util/share.type'
// 标准化文件大小
export const sizeConversion = (size: number) => {
    if (size < 1024) {
        return size + ' B'
    } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(2) + ' KB'
    } else {
        return (size / (1024 * 1024)).toFixed(2) + ' MB'
    }
}

// 时间格式化
export const formatTime = (time: string, model: string = 'minutes', timeLag: number = 0) => {
    // 将时间戳解析为 Date 对象
    const utcDate = new Date(time)

    // 转换为北京时间
    const beijingTime = new Date(utcDate.getTime() + timeLag * 60 * 60 * 1000)

    // 格式化日期和时间为 xxxx/xx/xx xx:xx
    const year = beijingTime.getFullYear()
    const month = String(beijingTime.getMonth() + 1).padStart(2, '0') // 月份从 0 开始，需加 1
    const day = String(beijingTime.getDate()).padStart(2, '0')
    const hours = String(beijingTime.getHours()).padStart(2, '0')
    const minutes = String(beijingTime.getMinutes()).padStart(2, '0')

    // 拼接为目标格式
    if (model === 'minutes') {
        return `${year}/${month}/${day} ${hours}:${minutes}`
    } else if (model === 'day') {
        return `${year}/${month}/${day}`
    }
    const formattedTime = `${year}/${month}/${day} ${hours}:${minutes}`
    return formattedTime
}

// polygon feature 转 box coordinates 序列
export function polygonGeometryToBoxCoordinates(polygonGeometry: polygonGeometry) {
    const boxCoordinates = [
        [polygonGeometry.coordinates[0][0][0], polygonGeometry.coordinates[0][0][1]],
        [polygonGeometry.coordinates[0][1][0], polygonGeometry.coordinates[0][1][1]],
        [polygonGeometry.coordinates[0][2][0], polygonGeometry.coordinates[0][2][1]],
        [polygonGeometry.coordinates[0][3][0], polygonGeometry.coordinates[0][3][1]],
    ]
    return boxCoordinates as [
        [number, number],
        [number, number],
        [number, number],
        [number, number],
    ]
}
