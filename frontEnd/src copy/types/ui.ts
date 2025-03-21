import { type ProductView } from './sensor'
import { type polygonGeometry } from './sharing'

/////// UI interface //////////////////////////////////
export interface FilterConditions {
    products: Array<ProductView>
    dateRange: [string, string]
    geometry: polygonGeometry
}
