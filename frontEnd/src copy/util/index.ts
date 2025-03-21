import { type polygonGeometry } from '@/types/sharing'

/////// File Download //////////////////////////////////
export function blobDownload(blob: Blob, name: string) {
    if (!blob) return
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.style.display = 'none'
    link.href = url
    link.download = name
    link.click()

    URL.revokeObjectURL(url)
}

/////// UI Color Palette ///////////////////////////////
export const UIColorPalette = [
    '#0284c7',
    '#16a34a',
    '#dc2626',
    '#eab308',
    '#6366f1',
    '#0891b2',
    '#059669',
    '#0d9488',
]
export const getColorFromPalette = (index: number): string => {
    return UIColorPalette[index % UIColorPalette.length]
}

/////// polygonGeometry to boxCoordinates //////////////////////////////////
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
