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
export const formatTime = (
    time: string | any,
    model: string = 'minutes',
    timeLag: number = 0,
    isHyphen: boolean = false,
) => {
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
export const formatTimeToText = (
    time: string | number | Date,
    lang: 'zh' | 'en' = 'zh',
): string => {
    const targetDate = new Date(time)
    const now = new Date()
    const diffMs = now.getTime() - targetDate.getTime()
    const diffSec = Math.floor(diffMs / 1000)
    const diffMin = Math.floor(diffSec / 60)
    const diffHour = Math.floor(diffMin / 60)
    const diffDay = Math.floor(diffHour / 24)

    // 精确到分钟
    if (diffSec < 300) {
        return lang === 'zh' ? '刚刚' : 'Just now'
    } else if (diffMin < 30) {
        return lang === 'zh' ? `${diffMin}分钟前` : `${diffMin} minutes ago`
    } else if (diffMin < 40) {
        return lang === 'zh' ? '半小时前' : 'Half an hour ago'
    } else if (diffMin < 60) {
        return lang === 'zh' ? `${diffMin}分钟前` : `${diffMin} minutes ago`
    } else if (diffHour < 24) {
        return lang === 'zh' ? `${diffHour}小时前` : `${diffHour} hours ago`
    } else if (diffDay === 1) {
        return lang === 'zh' ? '昨天' : 'Yesterday'
    } else if (diffDay === 2) {
        return lang === 'zh' ? '前天' : 'Day before yesterday'
    } else if (diffDay < 7) {
        return lang === 'zh' ? `${diffDay}天前` : `${diffDay} days ago`
    } else if (diffDay < 30) {
        return lang === 'zh'
            ? `${Math.floor(diffDay / 7)}周前`
            : `${Math.floor(diffDay / 7)} weeks ago`
    } else {
        return targetDate.toLocaleDateString(lang === 'zh' ? 'zh-CN' : 'en-US')
    }
}

/**
 * 统一科学计数法格式化：解决大数、小数过长问题
 */
export const formatSmartNumber = (numStr: any): string => {
    const num = parseFloat(numStr);
    if (isNaN(num)) return numStr;
    if (num === 0) return '0';

    const absNum = Math.abs(num);

    // 1. 大数字 (大于等于 10000) 或 极小数字 (小于 0.001) 使用科学计数法
    // 例如: 1,240,000 -> 1.24e6
    // 例如: 0.00000124 -> 1.24e-6
    if (absNum >= 10000 || absNum < 0.001) {
        // toExponential(2) 保留两位小数，如 1.24e+6
        // .replace(/\+/g, '') 移除正号(e+6 -> e6)进一步缩短长度
        return num.toExponential(2).replace(/\+/g, '');
    }

    // 2. 中间常规数字 (0.001 ~ 9999) 保持原样或限制小数
    // Number(...).toString() 自动去除末尾多余的 0
    return Number(num.toFixed(4)).toString();
};

/**
 * 简化区间 Label
 */
export const simplifyRangeLabel = (label: string): string => {
  if (!label || typeof label !== 'string') return label;

  /**
   * 正则解析逻辑：
   * ^(-?\d+\.?\d*(?:e[+-]?\d+)?) : 匹配开头的数字（支持负号、小数、科学计数法）
   * \s*-\s* : 匹配中间作为分隔符的横杠（允许左右有空格）
   * (-?\d+\.?\d*(?:e[+-]?\d+)?)$ : 匹配结尾的数字
   */
  const rangeRegex = /^(-?\d+\.?\d*(?:e[+-]?\d+)?)\s*-\s*(-?\d+\.?\d*(?:e[+-]?\d+)?)$/i;
  const match = label.trim().match(rangeRegex);

  if (match) {
      const low = match[1];
      const up = match[2];
      // 使用 "~" 分隔符，清晰区分两个数字，特别是当数字带负号或 e-7 时
      return `${formatSmartNumber(low)}~${formatSmartNumber(up)}`;
  }

  // 如果正则匹配失败，说明不是标准的 "数字-数字" 格式，回退到原字符串
  return label;
};

// 定义一些常数
const PI = 3.1415926535897932384626;
const a = 6378245.0; // 克拉索夫斯基椭球参数：长半轴
const ee = 0.00669342162296594323; // 偏心率平方

/**
 * GCJ-02 转 WGS-84
 */
export function gcj02towgs84(lng: number, lat: number): [number, number] {
    if (out_of_china(lng, lat)) {
        return [lng, lat];
    }
    let dlat = transformlat(lng - 105.0, lat - 35.0);
    let dlng = transformlng(lng - 105.0, lat - 35.0);
    let radlat = lat / 180.0 * PI;
    let magic = Math.sin(radlat);
    magic = 1 - ee * magic * magic;
    let sqrtmagic = Math.sqrt(magic);
    dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
    dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);
    let mglat = lat + dlat;
    let mglng = lng + dlng;
    return [lng * 2 - mglng, lat * 2 - mglat];
}

function transformlat(lng: number, lat: number): number {
    let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
    ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
    ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
    return ret;
}

function transformlng(lng: number, lat: number): number {
    let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
    ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
    ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
    return ret;
}

function out_of_china(lng: number, lat: number): boolean {
    return (lng < 72.004 || lng > 137.8347) || (lat < 0.8293 || lat > 55.8271);
}