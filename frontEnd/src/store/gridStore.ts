import { defineStore } from 'pinia'
import type { polygonGeometry } from '@/util/share.type'
const debug = false
export const useGridStore = defineStore('gridStore', {
    state: () => ({
        _selectedGrids: new Set(),
        _allGrids: new Set(),
        _polygon: null as polygonGeometry | null,
        _point: [] as number[],
        _line: [] as number[][],
    }),
    getters: {
        selectedGrids: (state) => Array.from(state._selectedGrids) as Array<string>,
        polygon: (state) => state._polygon,
        allGrids: (state) => Array.from(state._allGrids) as Array<string>,
        pickedLine: (state) => state._line,
    },
    actions: {
        removeGrid(id: string) {
            if (this._selectedGrids.has(id)) {
                this._selectedGrids.delete(id)
            }
            debug && console.log('removeGrid', id, this._selectedGrids)
        },
        addGrid(id: string) {
            this._selectedGrids.add(id)
            debug && console.log('addGrid', id, this._selectedGrids)
        },
        addGrids(ids: string[]) {
            ids.forEach((id) => this._selectedGrids.add(id))
            debug && console.log('addGrids', ids, this._selectedGrids)
        },
        storeAllGrids(ids: string[]) {
            this._allGrids = new Set(ids)
            debug && console.log('setAllGrids', ids, this._allGrids)
        },
        addAllGrids() {
            this.allGrids.forEach((id) => this._selectedGrids.add(id))
            debug && console.log('addAllGrids', this.selectedGrids)
        },
        cleadAllGrids() {
            this._selectedGrids.clear()
            debug && console.log('cleadAllGrids', this._selectedGrids)
        },
        setPolygon(polygon: polygonGeometry) {
            this._polygon = polygon
        },
        clearPolygon() {
            this._polygon = null
        },
        setPickedPoint(point: number[]) {
            this._point = point
        },
        setPickedLine(line: number[][]) {
            this._line = line
        },
    },
})
