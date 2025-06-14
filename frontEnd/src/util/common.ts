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

// 支持更人性化的时间显示和国际化
export const formatTimeToText = (time: string | number | Date, lang: 'zh' | 'en' = 'zh'): string => {
    const targetDate = new Date(time);
    const now = new Date();
    const diffMs = now.getTime() - targetDate.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    
    // 精确到分钟
    if (diffSec < 60) {
      return lang === 'zh' ? '刚刚' : 'Just now';
    } else if (diffMin < 60) {
      return lang === 'zh' ? `${diffMin}分钟前` : `${diffMin} minutes ago`;
    } else if (diffHour < 24) {
      return lang === 'zh' ? `${diffHour}小时前` : `${diffHour} hours ago`;
    } else if (diffDay === 1) {
      return lang === 'zh' ? '昨天' : 'Yesterday';
    } else if (diffDay === 2) {
      return lang === 'zh' ? '前天' : 'Day before yesterday';
    } else if (diffDay < 7) {
      return lang === 'zh' ? `${diffDay}天前` : `${diffDay} days ago`;
    } else if (diffDay < 30) {
      return lang === 'zh' ? `${Math.floor(diffDay / 7)}周前` : `${Math.floor(diffDay / 7)} weeks ago`;
    } else {
      return targetDate.toLocaleDateString(lang === 'zh' ? 'zh-CN' : 'en-US');
    }
};
