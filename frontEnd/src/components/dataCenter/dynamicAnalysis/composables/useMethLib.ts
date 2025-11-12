import { getMethLibPage } from '@/api/http/analytics-display/methlib.api'
import type { MethLib } from '@/api/http/analytics-display/methlib.type'
import { ref } from 'vue'

export const useMethLib = () => {
    const isMethLibExpand = ref(true)
    /**
     * 列表相关变量
     */
    const searchQuery = ref('')
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
            // tags: [selectedTask.value],
            tags: ['1'],
        })
        methLibList.value = res.data.records
        total.value = res.data.total
    }

    return {
        getMethLibList,
        currentPage,
        pageSize,
        total,
        methLibList,
        searchQuery,
        isMethLibExpand
    }
}
