export type AttrSymbology = {
    type: number,
    label: string,
    color?: string,
}

export type VectorSymbology = {
    [key: string]: {
        attrs: Array<AttrSymbology>,
        checkedAttrs: Array<string>,
        checkAll: boolean,
        isIndeterminate: boolean
    },
}