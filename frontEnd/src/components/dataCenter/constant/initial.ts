import type { ProductView } from '../apiAdapter/adapter'
import type { ImageFilterCondition } from '../type'
import dayjs from 'dayjs'

export const initialFilterConditions: ImageFilterCondition = {
    product: undefined as unknown as ProductView,
    dateRange: [dayjs('2010-10'), dayjs('2024-10')],
    geometry: {
        type: 'Polygon',
        coordinates: [
            [
                [-180, 90],
                [-180, -90],
                [180, -90],
                [180, 90],
                [-180, 90],
            ],
        ],
    },
    cloudCover: [10, 35],
}