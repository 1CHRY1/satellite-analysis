<template>
    <div class="relative flex flex-1 flex-row bg-black">
        <a-tour v-model:current="current" :open="openTour" :steps="steps" @close="handleOpenTour(false)" />
        <subtitle class="z-10 absolute" style="margin-top: 60px; " />
        <div class=" absolute left-18 h-[calc(100vh-100px)] p-4 text-gray-200 mb-0 gap-0 z-10"
            :class="showPanel ? 'w-[545px]' : 'w-16 transition-all duration-300'">
            <button @click="showPanel = !showPanel" class="absolute top-1/2 right-0 -translate-y-1/2 h-12 w-6 text-white rounded-l-lg shadow-lg 
                 items-center justify-center transition-all z-10"
                :class="showPanel ? 'bg-blue-600 hover:bg-blue-500' : 'bg-gray-800 hover:bg-gray-700'">
                <ChevronRightIcon :size="16" class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': showPanel }" />
            </button>
            <div v-if="showPanel">
                <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            🔍
                        </div>
                        <span class="page-title">交互式探索</span>
                        <div class="ml-2 cursor-pointer" @click="handleOpenTour(true)">
                            <a-tooltip>
                                <template #title>点击查看帮助</template>
                                <QuestionCircleOutlined :size="20" />
                            </a-tooltip>
                        </div>
                        <div class="section-icon absolute right-0 cursor-pointer" @click="toNoCloud">
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
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <MapIcon :size="16" class="config-icon" />
                                                <span>空间位置</span>
                                            </div>
                                            <div class="config-control">
                                                <segmented class="!w-full scale-y-90 scale-x-90 origin-center"
                                                    :options="tabs" :active-tab="activeTab" @change="handleSelectTab" />
                                            </div>
                                            <div class="config-control mt-3 mb-3 flex flex-col !items-start">
                                                <div v-if="activeTab === 'region'"
                                                    class="config-control justify-center">
                                                    <RegionSelects v-model="selectedRegion"
                                                        :placeholder="[t('datapage.explore.data.intext_choose')]"
                                                        class="flex gap-2"
                                                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                                </div>
                                                <div v-if="activeTab === 'region'" class="flex flex-row mt-2 ml-2">
                                                    <div class="text-red-500">*</div>
                                                    <span class="text-xs text-gray-400">未选择行政区默认以全国范围检索</span>
                                                </div>
                                                <div v-else-if="activeTab === 'poi'"
                                                    class="config-control justify-center w-full">
                                                    <el-select v-model="selectedPOI" filterable remote reserve-keyword
                                                        value-key="id"
                                                        :placeholder="t('datapage.explore.data.intext_POI')"
                                                        :remote-method="fetchPOIOptions"
                                                        class="!w-[90%] bg-[#0d1526] text-white"
                                                        popper-class="bg-[#0d1526] text-white">
                                                        <el-option v-for="item in poiOptions" :key="item.id"
                                                            :label="item.name + '(' + item.pname + item.cityname + item.adname + item.address + ')'"
                                                            :value="item" />
                                                    </el-select>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="config-container">
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <!-- 格网分辨率 -->
                                                <span>{{ t('datapage.explore.data.subtitle2') }}</span>
                                            </div>
                                            <div class="config-control flex-col !items-start">
                                                <div class="flex flex-row gap-2 items-center w-full">
                                                    <!-- {{ t('datapage.explore.data.resolution') }} -->
                                                    <select v-model="selectedGridResolution"
                                                        class="w-40 scale-88 appearance-none rounded-lg border border-[#2c3e50] bg-[#0d1526] px-4 py-2 pr-8 text-white transition-all duration-200 hover:border-[#206d93] focus:border-[#3b82f6] focus:outline-none">
                                                        <option v-for="option in gridOptions" :key="option"
                                                            :value="option" class="bg-[#0d1526] text-white">
                                                            {{ option }}km
                                                        </option>
                                                    </select>
                                                    <a-button ref="ref1" class="a-button" type="primary" @click="getAllGrid">
                                                        {{ t('datapage.explore.data.button') }}
                                                    </a-button>
                                                </div>
                                                <div class="flex flex-row mt-2 ml-2">
                                                    <div class="text-red-500">*</div>
                                                    <span class="text-xs text-gray-400">{{
                                                        t('datapage.explore.data.advice')
                                                    }}</span>
                                                </div>
                                            </div>
                                        </div>
                                        <!-- <button @click="getAllGrid"
                                            class="cursor-pointer scale-98 rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-93">
                                            {{ t('datapage.explore.data.button') }}
                                        </button> -->
                                    </div>
                                    <div class="config-container">
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <CalendarIcon :size="16" class="config-icon" />
                                                <span>{{ t('datapage.explore.section_time.subtitle1') }}</span>
                                            </div>
                                            <div class="config-control">
                                                <a-range-picker class="custom-date-picker"
                                                    v-model:value="selectedDateRange" :presets="dateRangePresets"
                                                    :allow-clear="false"
                                                    :placeholder="t('datapage.explore.section_time.intext_date')" />
                                            </div>
                                        </div>
                                    </div>
                                    <div class="config-container">
                                        <div class="config-item-no-hover"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <ZapIcon :size="16" class="config-icon" />
                                                <span>快速操作</span>
                                            </div>
                                            <div class="config-control">
                                                <div class="control-info-container">
                                                    <a-space ref="ref2" class="control-info-item" @click="applyFilter" :class="{
                                                        'cursor-not-allowed': filterLoading,
                                                        'cursor-pointer': !filterLoading,
                                                    }">
                                                        <div class="result-info-icon">
                                                            🔍
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-value">
                                                                <span style="font-size: 1rem;">数据检索</span>
                                                                <Loader v-if="filterLoading" class="ml-2" />
                                                            </div>
                                                        </div>
                                                    </a-space>
                                                    <a-space ref="ref3" class="control-info-item" @click="toNoCloud" :class="{
                                                        'cursor-not-allowed': !isFilterDone,
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
                                                    </a-space>
                                                    <a-space ref="ref4" class="control-info-item" @click="toAnalysis" :class="{
                                                        'cursor-not-allowed': !isFilterDone,
                                                        'cursor-pointer': isFilterDone,
                                                    }">
                                                        <div class="result-info-icon">
                                                            📈
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-value">
                                                                <span style="font-size: 1rem;">展示分析</span>
                                                            </div>
                                                        </div>
                                                    </a-space>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>
                            <!-- 交互探索 -->
                            <section class="panel-section">
                                <div class="section-header flex-row">
                                    <div class="section-icon">
                                        <DatabaseIcon :size="18" />
                                    </div>
                                    <h2 class="section-title">{{ t('datapage.explore.section_interactive.sectiontitle')
                                    }}</h2>
                                    <div class="section-icon absolute right-0 cursor-pointer"
                                        @click="destroyExploreLayers">
                                        <a-tooltip>
                                            <template #title>{{ t('datapage.explore.section_interactive.clear')
                                            }}</template>
                                            <Trash2Icon :size="18" />
                                        </a-tooltip>
                                    </div>
                                </div>
                                <div class="section-content">
                                    <div class="stats"
                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
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
                                                                <div class="result-info-label">{{
                                                                    t('datapage.explore.section_time.resolution') }}
                                                                </div>
                                                                <div class="result-info-value">
                                                                    {{ selectedGridResolution }}km
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CalendarIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">{{
                                                                    t('datapage.explore.section_time.time') }}</div>
                                                                <div class="result-info-value date-range">
                                                                    <div class="date-item">
                                                                        {{
                                                                            formatTime(
                                                                                selectedDateRange[0],
                                                                                'day',
                                                                            )
                                                                        }}~
                                                                        {{
                                                                            formatTime(
                                                                                selectedDateRange[1],
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
                                                                <div class="result-info-label">{{
                                                                    t('datapage.explore.section_time.search') }}</div>
                                                                <div class="result-info-value">
                                                                    {{ sceneStats.total }}{{ t('datapage.explore.scene')
                                                                    }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CloudIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">{{
                                                                    t('datapage.explore.percent') }}</div>
                                                                <div class="result-info-value">
                                                                    {{
                                                                        sceneStats.coverage
                                                                    }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="stats-item-tree"
                                                v-for="(category, index) in sceneStats.category" :key="category" :class="{
                                                    'stats-item-rounded-t': index === 0,
                                                    'stats-item-rounded-b': index === sceneStats.category.length - 1,
                                                }">
                                                <div class="config-label relative">
                                                    <ChevronDown v-if="isRSItemExpand[index]" :size="22"
                                                        class="config-icon cursor-pointer"
                                                        @click="isRSItemExpand[index] = false" />
                                                    <ChevronRight v-else @click="isRSItemExpand[index] = true"
                                                        :size="22" class="config-icon cursor-pointer" />
                                                    <span>{{ sceneStats?.dataset?.[category]?.label }}</span>
                                                </div>
                                                <div class="stats-subtitile" v-show="!isRSItemExpand[index]">
                                                    <div class="flex flex-row gap-2 items-center ml-4">
                                                        <ImageIcon :size="12" class="text-[#7a899f]"
                                                            @click="isRSItemExpand[index] = false" />
                                                        <span class="text-xs text-[#7a899f]">{{
                                                            sceneStats?.dataset?.[category]?.total
                                                        }} 景</span>
                                                    </div>
                                                    <div class="flex flex-row gap-2 items-center">
                                                        <ChartColumnBig :size="12" class="text-[#7a899f]"
                                                            @click="isRSItemExpand[index] = false" />
                                                        <span class="text-xs text-[#7a899f]">
                                                            {{ sceneStats?.dataset?.[category]?.coverage }}
                                                        </span>
                                                    </div>
                                                </div>
                                                <div class="config-control flex w-full flex-col gap-4 h-full"
                                                    v-show="isRSItemExpand[index]">
                                                    <div class="result-info-container w-full h-full">
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CloudIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">{{
                                                                    t('datapage.explore.include') }}</div>
                                                                <div class="result-info-value">
                                                                    {{ sceneStats?.dataset?.[category]?.total }}{{
                                                                        t('datapage.explore.scene') }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CloudIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">{{
                                                                    t('datapage.explore.percent') }}</div>
                                                                <div v-if="!(sceneStats.total > 0)"
                                                                    class="result-info-value">
                                                                    {{ t('datapage.explore.section_interactive.intext1')
                                                                    }}
                                                                </div>
                                                                <div v-else class="result-info-value">
                                                                    {{ sceneStats?.dataset?.[category]?.coverage }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div v-if="(sceneStats?.dataset?.[category]?.total || 0) > 0"
                                                        class="!w-full ml-3">
                                                        <label class="mr-2 text-white">{{
                                                            t('datapage.explore.section_interactive.scene') }}</label>
                                                        <select
                                                            class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                                            v-model="selectedSensorName">
                                                            <option disabled selected value="">{{
                                                                t('datapage.explore.section_interactive.choose') }}
                                                            </option>
                                                            <!-- <option :value="'all'" class="truncate">全选</option> -->
                                                            <option
                                                                v-for="sensor in sceneStats?.dataset?.[category]?.dataList"
                                                                :value="sensor.sensorName" :key="sensor.sensorName"
                                                                class="truncate">
                                                                {{ sensor.platformName }}
                                                            </option>
                                                        </select>
                                                        <div class="flex flex-row items-center">
                                                            <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                                @click="showSceneResult(selectedSensorName)"
                                                                :disabled="!selectedSensorName">
                                                                {{ t('datapage.explore.section_interactive.button') }}
                                                            </a-button>
                                                            <a-tooltip>
                                                                <template #title>{{
                                                                    t('datapage.explore.section_interactive.clear')
                                                                }}</template>
                                                                <Trash2Icon :size="18"
                                                                    class="mt-4! ml-4! cursor-pointer"
                                                                    @click="destroyScene" />
                                                            </a-tooltip>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <a-empty v-if="sceneStats.total === 0" class="mt-2" />
                                        </div>
                                    </div>
                                    <div class="stats"
                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                        <div class="stats-header">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span>矢量数据</span>
                                            </div>
                                            <div class="absolute right-2 cursor-pointer">
                                                <ChevronDown v-if="isVectorExpand" :size="22"
                                                    @click="isVectorExpand = false" />
                                                <ChevronUp v-else @click="isVectorExpand = true" :size="22" />
                                            </div>
                                        </div>
                                        <div class="stats-content" v-show="isVectorExpand">
                                            <div class="config-label relative">
                                                <span class="result-info-label">共找到 {{ vectorStats.length }} 条记录</span>
                                            </div>
                                            <div class="max-h-[432px] overflow-y-auto">
                                                <div v-for="(item, index) in vectorStats" class="stats-item-tree" :class="{
                                                'stats-item-rounded-t': index === 0,
                                                'stats-item-rounded-b': index === vectorStats.length - 1,
                                            }" :key="item.tableName">
                                                <div class="config-label relative">
                                                    <ChevronDown v-if="isVectorItemExpand[index]" :size="22"
                                                        class="config-icon cursor-pointer"
                                                        @click="isVectorItemExpand[index] = false" />
                                                    <ChevronRight v-else @click="isVectorItemExpand[index] = true"
                                                        :size="22" class="config-icon cursor-pointer" />
                                                    <span>{{ item.vectorName }}</span>
                                                </div>
                                                <div v-if="isVectorItemExpand[index]"
                                                    class="space-y-3 p-4 rounded-md text-white w-full">
                                                    <div v-if="vectorSymbology[item.tableName].attrs.length"
                                                        class="flex items-center justify-between gap-2">
                                                        <el-checkbox v-model="vectorSymbology[item.tableName].checkAll"
                                                            :indeterminate="vectorSymbology[item.tableName].isIndeterminate"
                                                            @change="(val: boolean) => handleCheckAllChange(item.tableName, val)">
                                                            <template #default>
                                                                <span class="text-white">全选</span>
                                                            </template>
                                                        </el-checkbox>
                                                        <a-tooltip>
                                                            <template #title>{{ t('datapage.history.preview')
                                                                }}</template>
                                                            <Eye v-if="previewVectorList[index]"
                                                                @click="destroyVector(index)" :size="16"
                                                                class="cursor-pointer" />
                                                            <EyeOff v-else :size="16"
                                                                @click="showVectorResult(item.tableName, index)"
                                                                class="cursor-pointer" />
                                                        </a-tooltip>
                                                    </div>
                                                    <div class="w-full max-h-[248px] overflow-y-auto">
                                                        <el-checkbox-group
                                                            v-model="vectorSymbology[item.tableName].checkedAttrs"
                                                            @change="(val: string[]) => handleCheckedAttrsChange(item.tableName, val)">
                                                            <template
                                                                v-if="vectorSymbology[item.tableName].attrs.length">
                                                                <div v-for="(attr, attrIndex) in vectorSymbology[item.tableName].attrs"
                                                                    :key="attrIndex"
                                                                    class="flex items-center justify-between bg-[#01314e] px-3 mb-1.5 py-2 rounded">
                                                                    <div class="flex items-center gap-2">
                                                                        <el-checkbox class="config-label mt-1"
                                                                            :key="attr.type" :label="attr.label">
                                                                            <template default></template>
                                                                        </el-checkbox>
                                                                        <span class="config-label mt-1">{{ attr.label
                                                                            }}</span>
                                                                    </div>
                                                                    <el-color-picker v-model="attr.color" size="small"
                                                                        show-alpha :predefine="predefineColors" />
                                                                </div>
                                                            </template>
                                                        </el-checkbox-group>
                                                    </div>
                                                    <div v-if="vectorSymbology[item.tableName].attrs.length === 0"
                                                        class="flex justify-center items-center">
                                                        <a-space>
                                                            <a-spin size="large" />
                                                        </a-space>
                                                    </div>
                                                </div>
                                            </div>
                                            </div>
                                            
                                            <a-empty v-if="vectorStats.length === 0" />
                                        </div>
                                    </div>
                                    <div class="stats"
                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                        <div class="stats-header">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span>栅格产品</span>
                                            </div>
                                            <div class="absolute right-2 cursor-pointer">
                                                <ChevronDown v-if="isProductsExpand" :size="22"
                                                    @click="isProductsExpand = false" />
                                                <ChevronUp v-else @click="isProductsExpand = true" :size="22" />
                                            </div>
                                        </div>
                                        <div class="stats-content" v-show="isProductsExpand">
                                            <div class="config-label relative">
                                                <span class="result-info-label">共找到 {{ themeStats.total }} 条记录</span>
                                            </div>
                                            <div class="stats-item-tree"
                                                v-for="(category, index) in themeStats.category" :key="category" :class="{
                                                    'stats-item-rounded-t': index === 0,
                                                    'stats-item-rounded-b': index === themeStats.category.length - 1,
                                                }">
                                                <div class="config-label relative">
                                                    <ChevronDown v-if="isProductsItemExpand[index]" :size="22"
                                                        class="config-icon cursor-pointer"
                                                        @click="isProductsItemExpand[index] = false" />
                                                    <ChevronRight v-else @click="isProductsItemExpand[index] = true"
                                                        :size="22" class="config-icon cursor-pointer" />
                                                    <span>{{ themeStats?.dataset?.[category]?.label }}</span>
                                                    <div class="absolute right-0 flex flex-row gap-2 items-center">
                                                        <ImageIcon :size="12" class="text-[#7a899f]"
                                                            @click="isProductsItemExpand[index] = false" />
                                                        <span class="text-xs text-[#7a899f]">{{
                                                            themeStats?.dataset?.[category]?.total
                                                        }} 幅</span>
                                                    </div>
                                                </div>

                                                <div v-show="isProductsItemExpand[index]">
                                                    <div class="max-h-[432px] overflow-y-auto">
                                                        <div v-for="(themeName, idx) in themeStats?.dataset?.[category]?.dataList"
                                                        class="config-item mt-1 mb-2" :key="idx">
                                                        <div class="config-label relative">
                                                            <Image :size="16" class="config-icon" />
                                                            <span>{{ themeName }}</span>
                                                            <div class="absolute right-0 cursor-pointer">
                                                                <a-tooltip>
                                                                    <template #title>{{ t('datapage.history.preview')
                                                                        }}</template>
                                                                    <Eye v-if="shouldShowEyeOff(category, idx)"
                                                                        @click="toggleEye(category, idx, themeName)"
                                                                        :size="16" class="cursor-pointer" />
                                                                    <EyeOff v-else :size="16"
                                                                        @click="toggleEye(category, idx, themeName)"
                                                                        class="cursor-pointer" />
                                                                </a-tooltip>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    </div>
                                                    
                                                </div>
                                            </div>
                                            <a-empty v-if="themeStats.total === 0" />
                                        </div>
                                    </div>
                                </div>
                            </section>
                        </div>
                    </dv-border-box12>
                </div>
            </div>
        </div>
        <MapComp class="flex-1" :style="'local'" :proj="'globe'" :isPicking="isPicking" />
        <!-- MVT属性信息弹窗 - 已改用Mapbox原生弹窗 -->
        <!-- <MvtPop /> -->

        <teleport to="body">
            <div class="grid-popup-layer" v-show="gridPopupVisible">
                <div class="grid-popup-panel">
                    <PopContent />
                </div>
            </div>
        </teleport>
    </div>
</template>

<script setup lang="ts">
import bus from '@/store/bus'
import MapComp from '@/components/feature/map/mapComp.vue'
import PopContent from '@/components/feature/map/popContent/popContent.vue'
import segmented from '@/components/common/segmented.vue';
// import MvtPop from '@/components/feature/map/popContent/mvtPop.vue' // 已改用Mapbox原生弹窗
import { ref, computed, type Ref, watch, reactive, onMounted, provide, inject, onUnmounted, nextTick } from 'vue'
import { RegionSelects } from 'v-region'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { formatTime } from '@/util/common'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { mapManager } from '@/util/map/mapManager'
import subtitle from '../subtitle.vue';
import { ezStore } from '@/store'
import {
    DatabaseIcon,
    CalendarIcon,
    CloudIcon,
    ImageIcon,
    BoltIcon,
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
    ChartColumnBig,
    ChevronRightIcon,
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
import { useVisualize } from './useVisualize'
import { message, type TourProps } from 'ant-design-vue';
const selectedVectorTableName = ref('')
const {
    // ------------------------ 数据检索 1.遥感影像可视化 -------------------------- //
    showSceneResult, selectedSensorName, destroyScene, destroyExploreLayers,
    // ------------------------ 数据检索 2.矢量可视化 -------------------------- //
    previewVectorList, showVectorResult, destroyVector,
    // ------------------------ 数据检索 3.栅格产品可视化 -------------------------- //
    toggleEye, shouldShowEyeOff,
    predefineColors,
    vectorSymbology,
    handleCheckedAttrsChange,
    handleCheckAllChange,
} = useVisualize()
import {
    // ------------------------ 数据检索 1.空间位置 -------------------------- //
    selectedRegion, activeSpatialFilterMethod as activeTab, selectedPOI,
    // ------------------------ 数据检索 2.格网分辨率 -------------------------- //
    selectedGridResolution, curGridsBoundary,
    // ------------------------ 数据检索 3.时间范围 -------------------------- //
    selectedDateRange,
    // ------------------------ 数据检索 4.筛选 -------------------------- //
    sceneStats, vectorStats, themeStats
} from './shared'
import { tableProps } from 'ant-design-vue/es/table';
import { QuestionCircleOutlined } from '@ant-design/icons-vue';

const {
    // ------------------------ 数据检索 1.空间位置 -------------------------- //
    tabs, poiOptions, fetchPOIOptions, handleSelectTab,
    // ------------------------ 数据检索 2.格网分辨率 -------------------------- //
    gridOptions, allGrids, allGridCount, getAllGrid,
    // ------------------------ 数据检索 3.时间范围 -------------------------- //
    dateRangePresets,
    // ------------------------ 数据检索 4.筛选 -------------------------- //
    doFilter: applyFilter, filterLoading, isFilterDone,
} = useFilter()

/**
 * Tour Guide
 */
const ref1 = ref(null)
const ref2 = ref(null)
const ref3 = ref(null)
const ref4 = ref(null)
const current = ref(0)
const openTour = ref<boolean>(false)
const steps: TourProps['steps'] = [
    {
        title: '获取格网',
        description: '在这里获取您选取区域的格网',
        target: () => ref1.value && (ref1.value as any).$el,
    },
    {
        title: '数据检索',
        description: '基于获取到的格网，您可以点击这里检索平台数据。数据检索后，您可以进行下方导航栏的交互探索步骤',
        target: () => ref2.value && (ref2.value as any).$el,
    },
    {
        title: '数据准备',
        description: '您也可以进入数据准备页面，生成无云一版图和时序立方体',
        target: () => ref3.value && (ref3.value as any).$el,
    },
    {
        title: '展示分析',
        description: '或者进入展示分析页面，调用平台工具进行在线分析',
        target: () => ref4.value && (ref4.value as any).$el,
    },
]
const handleOpenTour = (val: boolean): void => {
    openTour.value = val
}
//显示左panel
const showPanel = ref(true)

const isExpand = ref<boolean>(true)
const isRSExpand = ref<boolean>(true)
const isVectorExpand = ref<boolean>(true)
const isProductsExpand = ref<boolean>(true)
const isRSItemExpand = ref<boolean[]>([])
const isVectorItemExpand = ref<boolean[]>([])
const isProductsItemExpand = ref<boolean[]>([])

const setInitialListExpand = () => {
    sceneStats.value.category.forEach(() => {
        isRSItemExpand.value.push(false)
    })
    themeStats.value.category.forEach(() => {
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

const toAnalysis = () => {
    if (!isFilterDone.value) return message.warning('请先进行数据检索')
    if (router.currentRoute.value.path !== '/analysis') {
        // TODO 跳转有Bug
        router.push('/analysis')
    }
}

// 地图展示
const isPicking = ref(false)

// Grid popup visibility
const gridPopupVisible = ref(false)
const handleGridPopupVisible = (visible: boolean) => {
    gridPopupVisible.value = !!visible
}

// Critical: when visibility turns true, schedule reset AFTER DOM shows
watch(gridPopupVisible, (v) => {
    if (v) {
        nextTick(() => {
            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    bus.emit('gridPopup:reset-position')
                })
            })
        })
    } else {
        // 弹窗关闭时，同时关闭时间轴（年份/月份选择）
        bus.emit('closeTimeline')
    }
})

onMounted(async () => {
    setInitialListExpand()

    if (!ezStore.get('statisticCache')) ezStore.set('statisticCache', new Map())
    if (!ezStore.get('sceneNodataMap')) ezStore.set('sceneNodataMap', new Map())

    // Add grid popup visibility listener
    bus.on('gridPopup:visible', handleGridPopupVisible)
    // Lock/unlock page scroll when popup visible
    bus.on('gridPopup:visible', (v: boolean) => {
        try {
            document.documentElement.style.overflow = v ? 'hidden' : ''
            document.body.style.overflow = v ? 'hidden' : ''
        } catch { }
    })

    await mapManager.waitForInit();

    // 最后添加边界（确保在最上层）
    setTimeout(() => {
        if (exploreData.load) {
            InteractiveExploreMapOps.map_addPolygonLayer({
                geoJson: exploreData.boundary,
                id: 'UniqueLayer',
                lineColor: '#8fffff'
            });
        }
    }, 2); // 适当延迟

    // setTimeout(() => {
    //     mapManager.withMap((m) => {
    //         m.showTileBoundaries = true
    //     })
    // }, 1)

})

onUnmounted(() => {
    destroyExploreLayers()
    bus.off('gridPopup:visible', handleGridPopupVisible)
    try { document.documentElement.style.overflow = '' } catch { }
    try { document.body.style.overflow = '' } catch { }
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

.config-item {
    background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);
}

.grid-popup-layer {
    position: fixed;
    inset: 0;
    z-index: 2200;
    pointer-events: none;
}

.grid-popup-panel {
    position: absolute;
    inset: 0;
    pointer-events: none;
}

:deep(.grid-popup-panel .vdr) {
    pointer-events: auto;
}

::deep(.grid-popup-panel .vdr *) {
    pointer-events: auto;
}

.grid-popup-layer {
    position: fixed;
    inset: 0;
    z-index: 2200;
    pointer-events: none;
    overflow: hidden;
    /* NEW: prevent body scrolling */
}

.grid-popup-panel {
    position: absolute;
    inset: 0;
    pointer-events: none;
    overflow: hidden;
    /* NEW: clip inner overflow */
}
</style>
