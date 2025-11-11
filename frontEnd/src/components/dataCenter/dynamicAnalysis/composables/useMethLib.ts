import { getMethLibPage } from '@/api/http/analytics-display/methlib.api'
import type { MethLib } from '@/api/http/analytics-display/methlib.type'
import { useToolRegistryStore } from '@/store'
import { computed, ref } from 'vue'
const toolRegistry = useToolRegistryStore()
import { useI18n } from 'vue-i18n'

export const useMethLib = () => {
    const { t } = useI18n()
    /**
     * Category相关变量
     */
    const builtinToolCategories = [
        {
            name: 'Whitebox Geospatial',
            tools: [
                { value: '1', label: 'Math and Stats Tools', disabled: false },
                { value: '2', label: 'Image Processing Tools', disabled: false },
                { value: '3', label: 'Filters', disabled: false },
                { value: '4', label: 'Data Tools', disabled: false },
                { value: '5', label: 'GIS Analysis', disabled: false },
                { value: '6', label: 'LiDAR Tools', disabled: false },
                { value: '7', label: 'Geomorphometric Analysis', disabled: false },
                { value: '8', label: 'Hydrological Analysis', disabled: false },
                { value: '9', label: 'Overlay Tools', disabled: false },
                { value: '10', label: 'Image Enhancement', disabled: false },
                { value: '11', label: 'Patch Shape Tools', disabled: false },
                { value: '12', label: 'Distance Tools', disabled: false },
                { value: '13', label: 'Stream Network Analysis', disabled: false },
                { value: '14', label: 'Whitebox Utilities', disabled: false },
                { value: '15', label: 'Machine Learning', disabled: false },
            ],
        },
        {
            name: '图像',
            tools: [
                { value: '指数分析', label: '指数分析', disabled: false },
                {
                    value: 'NDVI时序计算',
                    label: t('datapage.analysis.optionallab.task_NDVI'),
                    disabled: false,
                },
                {
                    value: '光谱分析',
                    label: t('datapage.analysis.optionallab.task_spectral'),
                    disabled: false,
                },
                {
                    value: 'DSM分析',
                    label: t('datapage.analysis.optionallab.task_DSM'),
                    disabled: false,
                },
                {
                    value: 'DEM分析',
                    label: t('datapage.analysis.optionallab.task_DEM'),
                    disabled: false,
                },
                {
                    value: '红绿立体',
                    label: t('datapage.analysis.optionallab.task_red_green'),
                    disabled: false,
                },
                {
                    value: '形变速率',
                    label: t('datapage.analysis.optionallab.task_rate'),
                    disabled: false,
                },
                { value: '伪彩色分割', label: '伪彩色分割', disabled: false },
                { value: '空间分析', label: '空间分析', disabled: false },
                { value: 'boxSelect', label: '归一化差异', disabled: false },
                { value: 'hillShade', label: '地形渲染', disabled: false },
                { value: 'landcoverClean', label: '地表覆盖数据清洗', disabled: false },
                { value: 'ReduceReg', label: '区域归约', disabled: false },
                { value: 'PixelArea', label: '像元面积', disabled: false },
                { value: 'PixelLonLat', label: '像元经纬度坐标', disabled: false },
            ],
        },
        {
            name: '影像集合',
            tools: [
                { value: 'Clipped Composite', label: '裁剪合成影像', disabled: false },
                { value: 'Filtered Composite', label: '滤波合成影像', disabled: false },
                { value: 'Linear Fit', label: '线性拟合', disabled: false },
                { value: 'Simple Cloud Score', label: '简易云量评分', disabled: false },
            ],
        },
        {
            name: '要素集合',
            tools: [
                { value: 'Buffer', label: '缓冲区分析', disabled: false },
                { value: 'Distance', label: '距离计算', disabled: false },
                { value: 'Join', label: '空间连接', disabled: false },
                { value: 'Computed Area Filter', label: '基于计算面积的筛选', disabled: false },
            ],
        },
    ]

    const searchQuery = ref('')

    const expandedCategories = ref<string[]>(['Whitebox Geospatial'])

    // const filteredCategories = computed(() => {
    //     const categories = allToolCategories.value
    //     if (!searchQuery.value) return categories

    //     const query = searchQuery.value.toLowerCase()
    //     return categories
    //         .map(category => ({
    //             ...category,
    //             tools: category.tools.filter(tool =>
    //                 tool.label.toLowerCase().includes(query) ||
    //                 category.name.toLowerCase().includes(query)
    //             )
    //         }))
    //         .filter(category => category.tools.length > 0)
    // })

    const dynamicToolCategories = computed(() => {
        const categoryMap = new Map<
            string,
            { name: string; tools: Array<{ value: string; label: string; disabled: boolean }> }
        >()
        toolRegistry.tools.forEach((tool) => {
            const categoryName = tool.category || '自定义'
            if (!categoryMap.has(categoryName)) {
                categoryMap.set(categoryName, {
                    name: categoryName,
                    tools: [],
                })
            }
            categoryMap.get(categoryName)?.tools.push({
                value: `dynamic:${tool.id}`,
                label: tool.name,
                disabled: false,
            })
        })
        return Array.from(categoryMap.values())
    })

    const allToolCategories = computed(() => {
        const categoryMap = new Map<
            string,
            { name: string; tools: Array<{ value: string; label: string; disabled: boolean }> }
        >()
        builtinToolCategories.forEach((category) => {
            categoryMap.set(category.name, {
                name: category.name,
                tools: [...category.tools],
            })
        })
        dynamicToolCategories.value.forEach((category) => {
            if (!categoryMap.has(category.name)) {
                categoryMap.set(category.name, {
                    name: category.name,
                    tools: [],
                })
            }
            categoryMap.get(category.name)?.tools.push(...category.tools)
        })
        return Array.from(categoryMap.values())
    })
    const selectedTask = ref<string>('')

    /**
     * 列表相关变量
     */
    const methLibList = ref<MethLib.Method[]>([])
    const currentPage = ref<number>(1)
    const pageSize = ref<number>(3)
    const total = ref<number>(1)
    const getMethLibList = async () => {
        // TEMP
        methLibList.value = [
            {
                id: 23,
                name: 'AverageOverlay',
                description:
                    'Calculates the average for each grid cell from a group of raster images.',
                longDesc:
                    'This tool can be used to find the average value in each cell of a grid from a set of input images (--inputs). It is therefore similar to the WeightedSum tool except that each input image is given equal weighting. This tool operates on a cell-by-cell basis. Therefore, each of the input rasters must share the same number of rows and columns and spatial extent. An error will be issued if this is not the case. At least two input rasters are required to run this tool. Like each of the WhiteboxTools overlay tools, this tool has been optimized for parallel processing.\n\nSee Also: WeightedSum',
                copyright: 'https://www.whiteboxgeo.com/',
                category: '',
                uuid: 'whitebox',
                type: 'refactoring',
                params: [
                    {
                        Name: 'Input Files',
                        Type: 'DataInput',
                        Flags: ['-i', '--inputs'],
                        Optional: false,
                        Description: 'Input raster files.',
                        default_value: null,
                        parameter_type: {
                            FileList: 'Raster',
                        },
                    },
                    {
                        Name: 'Output File',
                        Type: 'DataOutput',
                        Flags: ['-o', '--output'],
                        Optional: false,
                        Description: 'Output raster file.',
                        default_value: null,
                        parameter_type: {
                            NewFile: 'Raster',
                        },
                    },
                ],
                execution: 'exe',
                createTime: '2024-08-14 21:12:04',
            },
            {
                id: 3,
                name: 'Add',
                description:
                    'Performs an addition operation on two rasters or a raster and a constant value.',
                longDesc:
                    'This tool creates a new raster in which each grid cell is equal to the addition of the corresponding grid cells in two input rasters or one input raster and a constant value. If two images are input, both images must possess the same number of rows and columns and spatial extent, as the analysis will be carried out on a cell-by-cell basis. If a grid cell contains a NoData value in either of the input images, the cell will be excluded from the analysis.\n\nSee Also: Subtract, Multiply, Divide, InPlaceAdd',
                copyright: 'https://www.whiteboxgeo.com/',
                category: '',
                uuid: 'whitebox',
                type: 'refactoring',
                params: [
                    {
                        Name: 'Input File Or Constant Value',
                        Type: 'DataInput',
                        Flags: ['--input1'],
                        Optional: false,
                        Description: 'Input raster file or constant value.',
                        default_value: null,
                        parameter_type: {
                            ExistingFileOrFloat: 'Raster',
                        },
                    },
                    {
                        Name: 'Input File Or Constant Value',
                        Type: 'DataInput',
                        Flags: ['--input2'],
                        Optional: false,
                        Description: 'Input raster file or constant value.',
                        default_value: null,
                        parameter_type: {
                            ExistingFileOrFloat: 'Raster',
                        },
                    },
                    {
                        Name: 'Output File',
                        Type: 'DataOutput',
                        Flags: ['-o', '--output'],
                        Optional: false,
                        Description: 'Output raster file.',
                        default_value: null,
                        parameter_type: {
                            NewFile: 'Raster',
                        },
                    },
                ],
                execution: 'exe',
                createTime: '2024-08-14 21:11:54',
            },
            {
                id: 6,
                name: 'And',
                description: 'Performs a logical AND operator on two Boolean raster images.',
                longDesc:
                    'This tool is a Boolean AND operator, i.e. it works on True or False (1 and 0) values. Grid cells for which the first and second input rasters (--input1; --input2) have True values are assigned 1 in the output raster, otherwise grid cells are assigned a value of 0. All non-zero values in the input rasters are considered to be True, while all zero-valued grid cells are considered to be False. Grid cells containing NoData values in either of the input rasters will be assigned a NoData value in the output raster (--output).\n\nSee Also: Not, Or, Xor',
                copyright: 'https://www.whiteboxgeo.com/',
                category: '',
                uuid: 'whitebox',
                type: 'refactoring',
                params: [
                    {
                        Name: 'Input File',
                        Type: 'DataInput',
                        Flags: ['--input1'],
                        Optional: false,
                        Description: 'Input raster file.',
                        default_value: null,
                        parameter_type: {
                            ExistingFile: 'Raster',
                        },
                    },
                    {
                        Name: 'Input File',
                        Type: 'DataInput',
                        Flags: ['--input2'],
                        Optional: false,
                        Description: 'Input raster file.',
                        default_value: null,
                        parameter_type: {
                            ExistingFile: 'Raster',
                        },
                    },
                    {
                        Name: 'Output File',
                        Type: 'DataOutput',
                        Flags: ['-o', '--output'],
                        Optional: false,
                        Description: 'Output raster file.',
                        default_value: null,
                        parameter_type: {
                            NewFile: 'Raster',
                        },
                    },
                ],
                execution: 'exe',
                createTime: '2024-08-14 21:11:55',
            },
        ]
        const res = await getMethLibPage({
            page: currentPage.value,
            pageSize: pageSize.value,
            asc: false,
            searchText: searchQuery.value,
            tags: [selectedTask.value],
        })
        methLibList.value = res.data.records
        total.value = res.data.total
    }

    return {
        builtinToolCategories,
        searchQuery,
        expandedCategories,
        dynamicToolCategories,
        allToolCategories,
        getMethLibList,
        currentPage,
        pageSize,
        total,
        methLibList,
        selectedTask,
    }
}
