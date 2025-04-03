/**
 * model and methods
 */

export type modelsOrMethods = {
    author: string
    createTime: string
    description: string
    inputParams: Array<methodsEvent>
    modelType: string
    name: string
    normalTags: Array<string>
    outputParams: Array<methodsEvent>
    params: Array<methodsEvent>
    problemTags: string
    updateTime: string
}

export type methodsEvent = {
    default_value: string
    description: string
    flags: Array<string>
    name: string
    optional: boolean
    parameter_type: object
}
