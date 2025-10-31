// 时间格式化
export const formatTime = (time: string | any, model: string = 'minutes', timeLag: number = 0, isHyphen: boolean = false) => {
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
    const seconds = String(beijingTime.getSeconds()).padStart(2, '0')

    // 拼接为目标格式
    let result: string = ''
    if (model === 'seconds') {
        result = `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`
    } else if (model === 'minutes') {
        result = `${year}/${month}/${day} ${hours}:${minutes}`
    } else if (model === 'day') {
        result = `${year}/${month}/${day}`
    }
    if (isHyphen) {
        result = result.replace(/\//g, '-')
    }
    return result
}