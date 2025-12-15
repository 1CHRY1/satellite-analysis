export type AttrSymbology = {
    type: number | string,
    label: string,
    color?: string,
}

export type VectorSymbology = {
    [key: string]: {
        selectedField: string | undefined,
        attrs: Array<AttrSymbology>,
        checkedAttrs: Array<string>,
        checkAll: boolean,
        isIndeterminate: boolean,
        isRequesting: boolean
    },
}