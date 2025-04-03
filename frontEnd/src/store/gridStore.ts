import { defineStore } from 'pinia'

export const useGridStore = defineStore('gridStore', {
    state: () => ({
        _selectedGrids: new Set(),
    }),
    getters: {
        selectedGrids: (state) => Array.from(state._selectedGrids) as Array<string>,
    },
    actions: {
        removeGrid(id: string) {
            if (this._selectedGrids.has(id)) {
                this._selectedGrids.delete(id)
            }
        },
        addGrid(id: string) {
            this._selectedGrids.add(id)
        },
        clearGrids() {
            this._selectedGrids.clear()
        },
    },
})
