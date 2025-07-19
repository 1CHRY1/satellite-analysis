<template>
    <div class="relative flex flex-1 flex-row bg-black">
        <div class="w-[28vw] max-h-[calc(100vh-100px)] p-4 text-gray-200 mb-0 gap-0">
            <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                <div class="section-header">
                    <div class="section-icon">
                        🔍
                    </div>
                    <span class="page-title">交互式探索</span>
                    <div class="section-icon absolute right-0 cursor-pointer"
                        @click="toNoCloud">
                        <a-tooltip>
                            <template #title>无云一版图</template>
                            <ChevronRight :size="22" />
                        </a-tooltip>
                    </div>
                </div>
            </section>
            <div class="custom-panel px-2 mb-0 ">
                <dv-border-box12 class="!h-full">
                    <div class="main-container">
                        <!-- 数据检索 -->
                        <section class="panel-section">
                            <div class="section-header">
                                <div class="section-icon">
                                    <ListFilter :size="18" />
                                </div>
                                <h2 class="section-title">数据检索</h2>
                                <div class="absolute right-2 cursor-pointer">
                                    <ChevronDown v-if="isExpand" :size="22" @click="isExpand = false" />
                                    <ChevronUp v-else @click="isExpand = true" :size="22" />
                                </div>
                            </div>
                            <div v-show="isExpand" class="section-content flex-col !items-start">
                                <div class="config-container">
                                    <div class="config-item">
                                        <div class="config-label relative">
                                            <MapIcon :size="16" class="config-icon" />
                                            <span>空间位置</span>
                                        </div>
                                        <div class="config-control">
                                            <segmented class="!w-full scale-y-90 scale-x-90 origin-center" :options="tabs" :active-tab="activeTab" @change="handleSelectTab" />
                                        </div>
                                        <div class="config-control mt-3 mb-3">
                                            <div v-if="activeTab === 'region'" class="config-control justify-center">
                                                <RegionSelects v-model="region" :placeholder="[t('datapage.explore.section1.intext_choose')]"
                                                    class="flex gap-2"
                                                    select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                            </div>
                                            <div v-else-if="activeTab === 'poi'" class="config-control justify-center w-full">
                                                <el-select v-model="selectedPOI" filterable remote reserve-keyword value-key="id"
                                                    :placeholder="t('datapage.explore.section1.intext_POI')" :remote-method="fetchPOIOptions"
                                                    class="!w-[90%] bg-[#0d1526] text-white" popper-class="bg-[#0d1526] text-white">
                                                    <el-option v-for="item in poiOptions" :key="item.id"
                                                        :label="item.name + '(' + item.pname + item.cityname + item.adname + item.address + ')'"
                                                        :value="item" />
                                                </el-select>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="config-container">
                                    <div class="config-item">
                                        <div class="config-label relative">
                                            <BoltIcon :size="16" class="config-icon" />
                                            <!-- 格网分辨率 -->
                                            <span>{{t('datapage.explore.section1.subtitle2')}}</span>
                                        </div>
                                        <div class="config-control flex-col !items-start">
                                            <div class="flex flex-row gap-2 items-center w-full">
                                                <!-- {{ t('datapage.explore.section1.resolution') }} -->
                                                <select v-model="selectedGrid"
                                                    class="w-40 scale-88 appearance-none rounded-lg border border-[#2c3e50] bg-[#0d1526] px-4 py-2 pr-8 text-white transition-all duration-200 hover:border-[#206d93] focus:border-[#3b82f6] focus:outline-none">
                                                    <option v-for="option in gridOptions" :key="option" :value="option"
                                                        class="bg-[#0d1526] text-white">
                                                        {{ option }}km
                                                    </option>
                                                </select>
                                                <a-button class="a-button" type="primary" @click="getAllGrid">
                                                    {{ t('datapage.explore.section1.button') }}
                                                </a-button>
                                            </div>
                                            <div class="flex flex-row mt-2 ml-2">
                                                <div class="text-red-500">*</div>
                                                <span class="text-xs text-gray-400">{{ t('datapage.explore.section1.advice') }}</span>
                                            </div>
                                        </div>
                                    </div>
                                    <!-- <button @click="getAllGrid"
                                        class="cursor-pointer scale-98 rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-93">
                                        {{ t('datapage.explore.section1.button') }}
                                    </button> -->
                                </div>
                                <div class="config-container">
                                    <div class="config-item">
                                        <div class="config-label relative">
                                            <CalendarIcon :size="16" class="config-icon" />
                                            <span>{{ t('datapage.explore.section_time.subtitle1') }}</span>
                                        </div>
                                        <div class="config-control">
                                            <a-range-picker class="custom-date-picker" v-model:value="defaultConfig.dateRange"
                                                :allow-clear="false" :placeholder="t('datapage.explore.section_time.intext_date')" />
                                        </div>
                                    </div>
                                </div>
                                <div class="config-container">
                                    <div class="config-item-no-hover">
                                        <div class="config-label relative">
                                            <ZapIcon :size="16" class="config-icon" />
                                            <span>快速操作</span>
                                        </div>
                                        <div class="config-control">
                                            <div class="control-info-container">
                                                <div class="control-info-item" @click="applyFilter"
                                                    :class="{ 'cursor-not-allowed': filterLoading,
                                                        'cursor-pointer': !filterLoading,
                                                    }">
                                                    <div class="result-info-icon">
                                                        🔍
                                                    </div>
                                                    <div class="result-info-content" >
                                                        <div class="result-info-value">
                                                            <span style="font-size: 1rem;">数据检索</span>
                                                            <Loader v-if="filterLoading" class="ml-2" />
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="control-info-item" @click="handleOnTheFly"
                                                    :class="{ 'cursor-not-allowed': !isFilterDone,
                                                            'cursor-pointer': isFilterDone,
                                                    }">
                                                    <div class="result-info-icon">
                                                        ⚡
                                                    </div>
                                                    <div class="result-info-content" >
                                                        <div class="result-info-value">
                                                            <span style="font-size: 1rem;">on-the-fly</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="control-info-item" @click="toNoCloud"
                                                    :class="{ 'cursor-not-allowed': !isFilterDone,
                                                            'cursor-pointer': isFilterDone,
                                                    }">
                                                    <div class="result-info-icon">
                                                        🗺️
                                                    </div>
                                                    <div class="result-info-content">
                                                        <div class="result-info-value">
                                                            <span style="font-size: 1rem;">数据准备</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>
                        <!-- 交互探索 -->
                        <section class="panel-section">
                            <div class="section-header">
                                <div class="section-icon">
                                    <DatabaseIcon :size="18" />
                                </div>
                                <h2 class="section-title">{{ t('datapage.explore.section_interactive.sectiontitle') }}</h2>
                                <div class="section-icon absolute right-0 cursor-pointer" @click="clearAllShowingSensor">
                                    <a-tooltip>
                                        <template #title>{{ t('datapage.explore.section_interactive.clear') }}</template>
                                        <Trash2Icon :size="18" />
                                    </a-tooltip>
                                </div>
                            </div>
                            <div class="section-content">
                                <div class="stats">
                                    <div class="stats-header">
                                        <div class="config-label relative">
                                            <BoltIcon :size="16" class="config-icon" />
                                            <span class="text-sm">遥感影像</span>
                                        </div>
                                        <div class="absolute right-2 cursor-pointer">
                                            <ChevronDown v-if="isRSExpand" :size="22" @click="isRSExpand = false" />
                                            <ChevronUp v-else @click="isRSExpand = true" :size="22" />
                                        </div>
                                    </div>
                                    <div class="stats-content" v-show="isRSExpand">
                                        <div class="stats-item">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span>{{ t('datapage.explore.section_time.subtitle2') }}</span>
                                            </div>
                                            <div class="config-control flex-col gap-4">
                                                <div class="result-info-container">
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <MapIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">{{ t('datapage.explore.section_time.resolution') }}</div>
                                                            <div class="result-info-value">
                                                                {{ selectedGrid }}km
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CalendarIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">{{ t('datapage.explore.section_time.time') }}</div>
                                                            <div class="result-info-value date-range">
                                                                <div class="date-item">
                                                                    {{
                                                                        formatTime(
                                                                            defaultConfig.dateRange[0],
                                                                            'day',
                                                                        )
                                                                    }}~
                                                                    {{
                                                                        formatTime(
                                                                            defaultConfig.dateRange[1],
                                                                            'day',
                                                                        )
                                                                    }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CloudIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">{{ t('datapage.explore.section_time.search') }}</div>
                                                            <div class="result-info-value">
                                                                {{ allScenes.length }}{{ t('datapage.explore.scene') }}
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CloudIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">{{ t('datapage.explore.percent') }}</div>
                                                            <div class="result-info-value">
                                                                {{
                                                                    coverageRSRate != 'NaN%'
                                                                        ? coverageRSRate
                                                                        : '待计算'
                                                                }}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="stats-item-tree" v-for="([label, value], index) in resolutionType" :key="value"
                                            :class="{
                                                'stats-item-rounded-t': index === 0,
                                                'stats-item-rounded-b': index === resolutionType.length - 1,
                                            }"
                                        >
                                            <div class="config-label relative">
                                                <ChevronDown v-if="isRSItemExpand[index]" :size="22" class="config-icon cursor-pointer" @click="isRSItemExpand[index] = false" />
                                                <ChevronRight v-else @click="isRSItemExpand[index] = true" :size="22" class="config-icon cursor-pointer" />
                                                <span>{{ label }}{{ t('datapage.explore.section_interactive.subtitle') }}</span>
                                                <span v-if="label === '亚米'">{{ t('datapage.explore.section_interactive.subtitle1') }}</span>
                                            </div>
                                            <div class="stats-subtitile" v-show="!isRSItemExpand[index]">
                                                <div class="flex flex-row gap-2 items-center ml-4">
                                                    <ImageIcon :size="12" class="text-[#7a899f]" @click="isRSItemExpand[index] = false" />
                                                    <span class="text-xs text-[#7a899f]">{{ getSceneCountByResolution(value, allScenes) }} 景</span>
                                                </div>
                                                <div class="flex flex-row gap-2 items-center">
                                                    <ChartColumnBig :size="12" class="text-[#7a899f]" @click="isRSItemExpand[index] = false" />
                                                    <span class="text-xs text-[#7a899f]">
                                                        {{
                                                            (
                                                                (allGridsInResolution[label] * 100) /
                                                                allGridCount
                                                            ).toFixed(2) + '%'
                                                        }}
                                                    </span>
                                                </div>
                                            </div>
                                            <div class="config-control flex w-full flex-col gap-4 h-full" v-show="isRSItemExpand[index]">
                                                <div class="result-info-container w-full h-full">
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CloudIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">{{ t('datapage.explore.include') }}</div>
                                                            <div class="result-info-value">
                                                                {{ getSceneCountByResolution(value, allScenes) }}{{ t('datapage.explore.scene') }}
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CloudIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">{{ t('datapage.explore.percent') }}</div>
                                                            <div v-if="!(allScenes.length > 0)" class="result-info-value">
                                                                {{ t('datapage.explore.section_interactive.intext1') }}
                                                            </div>
                                                            <div v-else class="result-info-value">
                                                                {{
                                                                    (
                                                                        (allGridsInResolution[label] * 100) /
                                                                        allGridCount
                                                                    ).toFixed(2) + '%'
                                                                }}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div v-if="Object.keys(classifiedScenes).length > 0" class="!w-full ml-3">
                                                    <label class="mr-2 text-white">{{ t('datapage.explore.section_interactive.scene') }}</label>
                                                    <select
                                                        class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                                        v-model="resolutionPlatformSensor[label]">
                                                        <option disabled selected value="">{{ t('datapage.explore.section_interactive.choose') }}</option>
                                                        <!-- <option :value="'all'" class="truncate">全选</option> -->
                                                        <option v-for="platformName in classifiedScenes[
                                                            value + 'm'
                                                        ]" :value="platformName" :key="platformName" class="truncate">
                                                            {{ platformName }}
                                                        </option>
                                                    </select>
                                                    <div class="flex flex-row items-center">
                                                        <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                            @click="handleShowImageInBoundary(label)"
                                                            :disabled="!resolutionPlatformSensor[label]">
                                                            {{ t('datapage.explore.section_interactive.button') }}
                                                        </a-button>
                                                        <a-tooltip>
                                                            <template #title>{{ t('datapage.explore.section_interactive.clear') }}</template>
                                                            <Trash2Icon :size="18" class="mt-4! ml-4! cursor-pointer"
                                                                @click="clearAllShowingSensor" />
                                                        </a-tooltip>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="stats">
                                    <div class="stats-header">
                                        <div class="config-label relative">
                                            <BoltIcon :size="16" class="config-icon" />
                                            <span>矢量数据</span>
                                        </div>
                                        <div class="absolute right-2 cursor-pointer">
                                            <ChevronDown v-if="isVectorExpand" :size="22" @click="isVectorExpand = false" />
                                            <ChevronUp v-else @click="isVectorExpand = true" :size="22" />
                                        </div>
                                    </div>
                                    <div class="stats-content" v-show="isVectorExpand">
                                        <div class="config-label relative">
                                            <span class="result-info-label">共找到 {{allVectors.length}} 条记录</span>
                                        </div>
                                        <div v-for="(item, index) in allVectors" class="config-item mb-3" :key="item.tableName">
                                            <div class="config-label relative">
                                                <Image :size="16" class="config-icon" />
                                                <span>{{ item.vectorName }}</span>
                                                <div class="absolute right-0 cursor-pointer">
                                                    <a-tooltip>
                                                        <template #title>{{t('datapage.history.preview')}}</template>
                                                        <Eye v-if="previewVectorList[index]" @click="unPreviewVector" :size="16" class="cursor-pointer"/>
                                                        <EyeOff v-else :size="16" @click="showVectorResult(item.tableName)" class="cursor-pointer"/>
                                                    </a-tooltip>
                                                </div>
                                            </div>
                                            <!-- <div class="config-control flex-col !items-start">
                                                <div class="flex w-full flex-col gap-2">
                                                    <div class="result-info-container">
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <DatabaseIcon :size="12" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">{{t('datapage.history.data')}}</div>
                                                                <div>
                                                                    {{ item.dataSet }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div> -->
                                        </div>
                                        <a-empty v-if="allVectors.length === 0" />
                                        <!-- <div v-if="Object.keys(allVectors).length > 0" class="!w-full ml-3">
                                            <label class="mr-2 text-white">选择数据集：</label>
                                            <select
                                                class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                                v-model="selectedVectorTableName">
                                                <option disabled selected value="">{{ t('datapage.explore.section_interactive.choose') }}</option>
                                                <option v-for="vector in allVectors" :value="vector.tableName" :key="vector.tableName" class="truncate">
                                                            {{ vector.vectorName }}
                                                </option>
                                            </select>
                                            <div class="flex flex-row items-center">
                                                <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                    @click="handleShowVectorInBoundary(selectedVectorTableName)">
                                                    矢量可视化
                                                </a-button>
                                                <a-tooltip>
                                                    <template #title>{{ t('datapage.explore.section_interactive.clear') }}</template>
                                                    <Trash2Icon :size="18" class="mt-4! ml-4! cursor-pointer"
                                                        @click="clearAllShowingSensor" />
                                                </a-tooltip>
                                            </div>
                                        </div> -->
                                    </div>
                                </div>
                                <div class="stats">
                                    <div class="stats-header">
                                        <div class="config-label relative">
                                            <BoltIcon :size="16" class="config-icon" />
                                            <span>栅格产品</span>
                                        </div>
                                        <div class="absolute right-2 cursor-pointer">
                                            <ChevronDown v-if="isProductsExpand" :size="22" @click="isProductsExpand = false" />
                                            <ChevronUp v-else @click="isProductsExpand = true" :size="22" />
                                        </div>
                                    </div>
                                    <div class="stats-content" v-show="isProductsExpand">
                                        <div class="config-label relative">
                                            <span class="result-info-label">共找到 {{allProducts.length}} 条记录</span>
                                        </div>
                                        <div class="stats-item-tree" v-for="([label, value], index) in productType" :key="value"
                                            :class="{
                                                'stats-item-rounded-t': index === 0,
                                                'stats-item-rounded-b': index === productType.length - 1,
                                            }"
                                        >
                                            <div class="config-label relative">
                                                <ChevronDown v-if="isProductsItemExpand[index]" :size="22" class="config-icon cursor-pointer" @click="isProductsItemExpand[index] = false" />
                                                <ChevronRight v-else @click="isProductsItemExpand[index] = true" :size="22" class="config-icon cursor-pointer" />
                                                <span>{{ label }}产品</span>
                                                <div class="absolute right-0 flex flex-row gap-2 items-center">
                                                    <ImageIcon :size="12" class="text-[#7a899f]" @click="isProductsItemExpand[index] = false" />
                                                    <span class="text-xs text-[#7a899f]">{{ getSceneCountByProduct(value, allProducts) }} 幅</span>
                                                </div>
                                            </div>

                                            <div v-show="isProductsItemExpand[index]">
                                                <div v-for="(item, index) in classifiedProducts[label]" class="config-item mt-1 mb-2" :key="index">
                                                    <div class="config-label relative">
                                                        <Image :size="16" class="config-icon" />
                                                        <span>{{ item }}</span>
                                                        <div class="absolute right-0 cursor-pointer">
                                                            <a-tooltip>
                                                                <template #title>{{t('datapage.history.preview')}}</template>
                                                                <Eye v-if="shouldShowEyeOff(label, index)" @click="toggleEye(label, index, item)" :size="16" class="cursor-pointer"/>
                                                                <EyeOff v-else :size="16" @click="toggleEye(label, index, item)" class="cursor-pointer"/>
                                                            </a-tooltip>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="config-container">
                                    
                                    <!-- 检索后的统计信息 -->
                                    <!-- <div class="config-item">
                                        <div class="config-label relative">
                                            <BoltIcon :size="16" class="config-icon" />
                                            <span>统计信息</span>
                                        </div>
                                        <div class="config-control flex-col gap-4">
                                            <div v-for="(sensorItem, index) in filteredSensorsItems" class="config-item w-full">
                                                <div>传感器名称及分辨率：{{ sensorItem.key }}</div>
                                                <div>类型：{{ imageType(sensorItem.tags) }}</div>
                                                <div class="result-info-container">
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CloudIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">包含</div>
                                                            <div class="result-info-value">
                                                                {{ sensorItem.sceneCount }}景影像
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <CloudIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">影像覆盖率</div>
                                                            <div class="result-info-value">
                                                                {{
                                                                    sensorItem.coveredCount != 'NaN%'
                                                                        ? (
                                                                            (sensorItem.coveredCount *
                                                                                100) /
                                                                            allGridCount
                                                                        ).toFixed(2) + '%'
                                                                        : '待计算'
                                                                }}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div>
                                                    <label class="mr-2 text-white">选择影像：</label>
                                                    <select @change="
                                                        (e) =>
                                                            showImageBySensorAndSelect(
                                                                sensorItem,
                                                                (e.target as HTMLSelectElement).value,
                                                            )
                                                    "
                                                        class="max-h-[600px] max-w-[calc(100%-90px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none">
                                                        <option disabled selected value="">
                                                            请选择影像
                                                        </option>
                                                        <option v-for="sceneName in sensorItem.sceneNames" :key="sceneName"
                                                            :value="sceneName" class="truncate">
                                                            {{ sceneName }}
                                                        </option>
                                                    </select>
                                                </div>
                                                <div v-show="sensorItem.selectedSceneInfo" class="mt-4">
                                                    <div class="mr-1 grid grid-cols-[1fr_2fr]">
                                                        <span class="text-white">亮度拉伸:</span>
                                                        <a-slider :tip-formatter="scaleRateFormatter"
                                                            v-model:value="sensorItem.scaleRate"
                                                            @afterChange="onAfterScaleRateChange" />
                                                    </div>
                                                    <div>

                                                        <a-button class="custom-button w-full!" :loading="sensorItem.loading"
                                                            @click="handleShowImage(sensorItem)">
                                                            影像可视化
                                                        </a-button>
                                                    </div>
                                                </div>
                                            </div>

                                        </div>
                                    </div> -->
                                </div>
                            </div>
                        </section>
                    </div>
                </dv-border-box12>
            </div>
        </div>
        <MapComp class="flex-1" :style="'local'" :proj="'globe'" :isPicking="isPicking" />
    </div>
</template>

<script setup lang="ts">
import MapComp from '@/components/feature/map/mapComp.vue'
import segmented from '@/components/common/segmented.vue';
import { ref, computed, type Ref, watch, reactive, onMounted, provide,inject } from 'vue'
import { RegionSelects } from 'v-region'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { formatTime } from '@/util/common'
import * as MapOperation from '@/util/map/operation'
import { mapManager } from '@/util/map/mapManager'
import { ezStore } from '@/store'
import {
    DatabaseIcon,
    MapPinIcon,
    SearchIcon,
    CalendarIcon,
    UploadCloudIcon,
    RefreshCwIcon,
    HexagonIcon,
    CloudIcon,
    ApertureIcon,
    ClockIcon,
    ImageIcon,
    LayersIcon,
    DownloadIcon,
    FilePlus2Icon,
    BoltIcon,
    BanIcon,
    MapIcon,
    Cloud,
    Clock,
    Images,
    Loader,
    Trash2Icon,
    ChevronRight,
    FilterIcon,
    ListFilter,
    ZapIcon,
    SearchCheckIcon,
    CloudOffIcon,
    ChevronDown,
    ChevronUp,
    Percent,
    ChartColumnBig,
    EyeOff,
    Eye,
} from 'lucide-vue-next'
const emit = defineEmits(['submitConfig'])
import { useExploreStore } from '@/store/exploreStore'
const exploreData = useExploreStore()
import router from '@/router'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

import { useFilter } from './useFilter'
import { useStats } from './useStats'
import { useLayer } from './useLayer'
import { message } from 'ant-design-vue';
const selectedVectorTableName = ref('')

const {
    // 筛选条件默认配置
    defaultConfig,
    // 格网检索相关变量
    gridOptions, selectedGrid, allGrids, allGridCount, getAllGrid,
    // 空间检索方法
    activeSpatialFilterMethod: activeTab, tabs,
    // 空间检索方法 -- region
    region, 
    // 空间检索方法 -- POI
    selectedPOI, poiOptions, fetchPOIOptions,
    // 选择空间检索方法
    handleSelectTab,
    // 筛选
    allScenes, allGridsInResolution, allProducts, allGridsInProduct, allVectors, filter: applyFilter, filterLoading, isFilterDone,
    // 统计信息面板
    coverageRSRate, coverageProductsRate, handleShowImageInBoundary, handleShowProductInBoundary, handleShowVectorInBoundary,
    // 影像分辨率相关变量
    resolutionType, resolutionPlatformSensor, productType, productPlatformSensor,
    // on-the-fly
    handleCreateNoCloudTiles: handleOnTheFly,
} = useFilter()
const { classifiedScenes, getSceneCountByResolution, classifiedProducts, getSceneCountByProduct } = useStats()
const { clearAllShowingSensor } = useLayer()
const isExpand = ref<boolean>(true)
const isRSExpand = ref<boolean>(true)
const isVectorExpand = ref<boolean>(true)
const isProductsExpand = ref<boolean>(true)
const isRSItemExpand = ref<boolean[]>([])
const isVectorItemExpand = ref<boolean[]>([])
const isProductsItemExpand = ref<boolean[]>([])

const setInitialListExpand = () => {
    resolutionType.value.forEach(item => {
        isRSItemExpand.value.push(false)
    })
    productType.value.forEach(item => {
        isProductsItemExpand.value.push(false)
    })
    isRSItemExpand.value[0] = true
    isProductsItemExpand.value[0] = true
}

const toNoCloud = () => {
    if (!isFilterDone.value) return message.warning('请先进行数据检索')
    if (router.currentRoute.value.path !== '/nocloud') {
        // TODO 跳转有Bug
        router.push('/nocloud')
    }
}

const previewVectorList = computed<boolean[]>(() => {
    const list = Array(allVectors.value.length).fill(false)
    if (previewVectorIndex.value !== null) {
        list[previewVectorIndex.value] = true
    }
    return list
})
const previewVectorIndex = ref<number | null>(null)
const showVectorResult = async (tableName: string) => {
    previewVectorIndex.value = allVectors.value.findIndex(item => item.tableName === tableName)
    handleShowVectorInBoundary(tableName)
}
const unPreviewVector = () => {
    previewVectorIndex.value = null
    MapOperation.map_destroyMVTLayer()
}

// 使用一个对象来存储每个 Product Item 的显示状态
const eyeStates = ref({});
// 切换显示状态的方法
const toggleEye = (label: string, index: number, platformName: string) => {
    const key = `${label}_${index}`;
    let isShow = eyeStates.value[key];
    eyeStates.value[key] = !isShow;
    if (eyeStates.value[key]) {
        Object.keys(eyeStates.value).forEach(item => {
            if (item !== key) {
                eyeStates.value[item] = false
            }
        })
        showProductResult(label, platformName)
    } else {
        unPreviewProduct()
    }
};

// 判断当前应该显示 Eye 还是 EyeOff
const shouldShowEyeOff = (label: string, index: number) => {
  const key = `${label}_${index}`;
  return eyeStates.value[key];
};
const showProductResult = async (label: string, platformName: string) => {
    clearAllShowingSensor()
    handleShowProductInBoundary(label, platformName)
}
const unPreviewProduct = () => {
    clearAllShowingSensor()
}

/**
 * !!!!!!!!!
 * !!@!!!!!!!
 * !!!!!!E!@#@#@##!@\
 * 应特别需要，亚米数据的非ard数据从传感器到可视化全部被筛除掉了
 * 小心这个坑
 * 这样会导致亚米传统数据不属于任何一个分类
 * 小心
 * 小心
 * 小心
 */

// 地图展示
const isPicking = ref(false)

onMounted(async() => {
    setInitialListExpand()

    if (!ezStore.get('statisticCache')) ezStore.set('statisticCache', new Map())
    if (!ezStore.get('sceneNodataMap')) ezStore.set('sceneNodataMap', new Map())

    await mapManager.waitForInit();

    // 最后添加边界（确保在最上层）
    setTimeout(() => {
        if (exploreData.load){
            MapOperation.map_addPolygonLayer({
                geoJson: exploreData.boundary,
                id: 'UniqueLayer',
                lineColor: '#8fffff'
            });
        }
    }, 2); // 适当延迟
})
</script>

<style scoped src="../tabStyle.css">
:deep(button.ant-picker-year-btn) {
    pointer-events: none;
    color: #aaa;
}

:deep(button.ant-picker-month-btn) {
    pointer-events: none;
    color: #aaa;
}

</style>
