import type { SpatialFilterMethod } from "@/type/interactive-explore/filter"

export type VectorUrlParam = {
    landId: string
    source_layer: string
    spatialFilterMethod: SpatialFilterMethod
}
