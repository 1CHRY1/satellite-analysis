<template>
    <!-- 数据准备页面主容器 -->
    <div class="relative flex flex-1 flex-row bg-black">
        <subtitle class="z-10 absolute" style="margin-top: 60px; " />
        <!-- 左侧面板栏 -->
        <div class=" absolute left-16 z-10 h-[calc(100vh-100px)] p-4 text-gray-200"
            :class="showPanel ? 'w-[545px]' : 'w-16 transition-all duration-300'">
            <button @click="showPanel = !showPanel" class="absolute top-1/2 right-0 -translate-y-1/2 h-12 w-6 text-white rounded-l-lg shadow-lg 
                 items-center justify-center transition-all z-10"
                :class="showPanel ? 'bg-blue-600 hover:bg-blue-500' : 'bg-gray-800 hover:bg-gray-700'">
                <ChevronRightIcon :size="16" class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': showPanel }" />
            </button>
            <div v-if="showPanel">
                <!--顶部标题+历史记录图标-->
                <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            🗺️
                        </div>
                        <span class="page-title">数据准备</span>
                        <div class="section-icon absolute right-2 cursor-pointer">
                            <a-tooltip>
                                <template #title>{{ t('datapage.history.his_recon') }}</template>
                                <History :size="18" @click="setCurrentPanel('history')" />
                            </a-tooltip>
                        </div>
                    </div>
                </section>
                <!-- 内容区域 -->
                <div class="custom-panel px-2">
                    <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
                        <!--无云数据合成和时空立方体合成-->
                        <div class="main-container">
                            <a-alert v-if="exploreData.grids.length===0 && currentPanel === 'noCloud'"
                                description="请先完成交互探索"
                                type="warning" show-icon class="status-alert">
                                <template #action>
                                    <a-button size="small" @click="router.push('/explore')">前往</a-button>
                                </template>
                            </a-alert>
                            <br v-if="exploreData.grids.length===0  && currentPanel === 'noCloud'"/>
                            <!--无云数据合成-->
                            <section class="panel-section" v-show="currentPanel === 'noCloud'" key="complex">
                                <!--无云数据合成标题-->
                                <div class="section-header">
                                    <div class="section-icon">
                                        <CloudIcon :size="18" />
                                    </div>
                                    <h2 class="section-title">无云数据合成</h2>
                                    <div class="absolute right-2 cursor-pointer">
                                        <ChevronDown v-if="isComplexExpand" :size="22"
                                            @click="isComplexExpand = false" />
                                        <ChevronUp v-else @click="isComplexExpand = true" :size="22" />
                                    </div>
                                </div>
                                <!-- 无云数据合成内容区域 -->
                                <div v-show="isComplexExpand" class="section-content">
                                    <div class="config-container">
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <FilterIcon :size="16" class="config-icon" />
                                                <span class="text-base">数据筛选</span>
                                            </div>
                                            <div class="config-item-no-hover">
                                                <div class="config-label relative">
                                                    <BoltIcon :size="16" class="config-icon" />
                                                    <span>{{ t('datapage.nocloud.section_chinese.subtitle') }}</span>
                                                </div>
                                                <div class="config-control flex-col !items-start">
                                                    <div class="flex w-full flex-col gap-2">
                                                        <label class="flex items-center gap-2">
                                                            <input type="checkbox" v-model="additionalData[0]"
                                                                class="h-4 w-4 rounded"
                                                                @click="handleAdd1mDemoticImage" />
                                                            {{ t('datapage.nocloud.section_chinese.text_national_image')
                                                            }}
                                                        </label>
                                                        <label class="flex items-center gap-2">
                                                            <input type="checkbox" v-model="dataReconstruction[0]"
                                                                @click="handleAdd2mDemoticImages"
                                                                class="h-4 w-4 rounded" />
                                                            {{ t('datapage.nocloud.section_chinese.text_national2m') }}
                                                        </label>
                                                        <!-- 传感器选择 -->
                                                        <label>
                                                            {{ t('datapage.nocloud.choose') }}
                                                        </label>
                                                        <select
                                                            class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                                            v-model="selectnation">
                                                            <option disabled selected value="">{{
                                                                t('datapage.explore.section_interactive.choose') }}
                                                            </option>
                                                            <option v-for="(platform, index) in nation2mPlatformList"
                                                                :key="platform.platformName" :value="platform"
                                                                @click="handleShowSensorImage(selectnation)">
                                                                {{ platform.platformName }}
                                                            </option>
                                                        </select>

                                                        <div v-if="showProgress[0]"
                                                            class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                                :style="{ width: `${progress[0]}%` }"></div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-container">
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <ImageIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{ t('datapage.nocloud.section_chinese.resolution')
                                                                    }}
                                                                </div>
                                                                <div class="result-info-value">
                                                                    {{ exploreData.gridResolution }}km
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CalendarIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{ t('datapage.nocloud.section_chinese.timerange')
                                                                    }}
                                                                </div>
                                                                <div class="result-info-value date-range">
                                                                    <div class="date-item">
                                                                        {{
                                                                            formatTime(
                                                                                exploreData.dataRange[0],
                                                                                'day',
                                                                            )
                                                                        }}~
                                                                        {{
                                                                            formatTime(
                                                                                exploreData.dataRange[1],
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
                                                                <div class="result-info-label">
                                                                    亚米级国产影像
                                                                </div>
                                                                <div class="result-info-value">
                                                                    {{ demotic1mImages.length }}{{
                                                                        t('datapage.explore.scene') }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CloudIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    2米级国产影像
                                                                </div>
                                                                <div class="result-info-value">
                                                                    {{ demotic2mImages.length }}{{
                                                                        t('datapage.explore.scene') }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="config-item-no-hover">
                                                <div class="config-label relative">
                                                    <CalendarIcon :size="16" class="config-icon" />
                                                    <span>{{ t('datapage.nocloud.section_international.subtitle')
                                                        }}</span>
                                                    <el-tooltip content="对于缺失数据的格网，采用国外光学影像进行填补，填补过程中基于AI算法进行超分辨率重建"
                                                        placement="top" effect="dark">
                                                        <CircleHelp :size="14" />
                                                    </el-tooltip>
                                                </div>
                                                <div class="config-control flex-col !items-start">
                                                    <div class="flex flex-col gap-2">
                                                        <label class="flex items-center gap-2">
                                                            <input type="checkbox" v-model="additionalData[1]"
                                                                @click="handleAddAbroadImages"
                                                                :disabled="!dataReconstruction[0]"
                                                                class="h-4 w-4 rounded" />
                                                            {{ t('datapage.nocloud.section_international.text_preview')
                                                            }}
                                                        </label>
                                                        <label class="flex items-center gap-2">
                                                            <input type="checkbox" v-model="dataReconstruction[1]"
                                                                @click="handleDataReconstructionChangeWrapper(1)"
                                                                :disabled="!dataReconstruction[0]"
                                                                class="h-4 w-4 rounded" />
                                                            {{
                                                                t('datapage.nocloud.section_international.text_overseaimage')
                                                            }}
                                                        </label>
                                                        <!-- 传感器选择 -->
                                                        <label>
                                                            {{ t('datapage.nocloud.choose') }}
                                                        </label>
                                                        <select
                                                            class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                                            v-model="selectinternation">
                                                            <option disabled selected value="">{{
                                                                t('datapage.explore.section_interactive.choose') }}
                                                            </option>
                                                            <option
                                                                v-for="(platform, index) in internationalLightPlatformList"
                                                                :key="platform.platformName" :value="platform"
                                                                @click="handleShowSensorImage(selectinternation)">
                                                                {{ platform.platformName }}
                                                            </option>
                                                        </select>

                                                        <div v-if="showProgress[1]"
                                                            class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                                :style="{ width: `${progress[1]}%` }"></div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-container w-full">
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CloudIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{
                                                                        t('datapage.nocloud.section_international.text_research')
                                                                    }}
                                                                </div>
                                                                <div class="result-info-value">
                                                                    {{ internationalImages.length }}{{
                                                                        t('datapage.explore.scene') }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="config-item-no-hover">
                                                <div class="config-label relative">
                                                    <CalendarIcon :size="16" class="config-icon" />
                                                    <span>{{ t('datapage.nocloud.section_SAR.subtitle') }}</span>
                                                    <el-tooltip content="勾选将使用雷达数据进行色彩变换，与光学数据配准，并补充重构。" placement="top"
                                                        effect="dark">
                                                        <CircleHelp :size="14" />
                                                    </el-tooltip>
                                                </div>
                                                <div class="config-control flex-col !items-start">
                                                    <div class="flex flex-col gap-2">
                                                        <label class="flex items-center gap-2">
                                                            <input type="checkbox" v-model="additionalData[2]"
                                                                @click="handleAddRadarImages" :disabled="!additionalData[1] || !dataReconstruction[1]
                                                                    " class="h-4 w-4 rounded" />
                                                            {{ t('datapage.nocloud.section_SAR.text_preview') }}
                                                        </label>
                                                        <label class="flex items-center gap-2">
                                                            <input type="checkbox" v-model="dataReconstruction[2]"
                                                                @click="handleDataReconstructionChangeWrapper(2)"
                                                                :disabled="!additionalData[1] || !dataReconstruction[1]
                                                                    " class="h-4 w-4 rounded" />
                                                            {{ t('datapage.nocloud.section_SAR.text_SARtrans') }}
                                                        </label>

                                                        <!-- 传感器选择 -->
                                                        <label>
                                                            {{ t('datapage.nocloud.choose') }}
                                                        </label>
                                                        <select
                                                            class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                                            v-model="selectsar">
                                                            <option disabled selected value="">{{
                                                                t('datapage.explore.section_interactive.choose') }}
                                                            </option>
                                                            <option v-for="(platform, index) in SARPlatformList"
                                                                :key="platform.platformName" :value="platform"
                                                                @click="handleShowSensorImage(selectsar)">
                                                                {{ platform.platformName }}
                                                            </option>
                                                        </select>

                                                        <div v-if="showProgress[2]"
                                                            class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                                :style="{ width: `${progress[2]}%` }"></div>
                                                        </div>
                                                    </div>
                                                    <div class="result-info-container w-full">
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CloudIcon :size="16" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{
                                                                        t('datapage.nocloud.section_SAR.text_SARresearch')
                                                                    }}
                                                                </div>
                                                                <div class="result-info-value">
                                                                    {{ radarImages.length }}{{
                                                                        t('datapage.explore.scene') }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="flex w-full flex-col justify-center">
                                                <div v-if="showProgress[3]"
                                                    class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                                    <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                        :style="{ width: `${progress[3]}%` }"></div>
                                                </div>
                                            </div>
                                        </div>
                                        <!--简单数据合成-->
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <LayersIcon :size="16" class="config-icon" />
                                                <span class="text-base">简单数据合成</span>
                                                <el-tooltip content="使用红、绿、蓝三个波段进行无云一版图生成" placement="top"
                                                    effect="dark">
                                                    <CircleHelp :size="14" />
                                                </el-tooltip>
                                            </div>
                                            <div class="config-control flex-col !item-start">
                                                <!--两个按钮 on-the-fly加载 一版图服务生成-->
                                                <div class="flex w-full flex-row gap-2">
                                                    <button @click="calNoClouds" :disabled="noCloudLoading"
                                                        class="flex justify-center w-1/2 rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95"
                                                        :class="{
                                                            'cursor-not-allowed': noCloudLoading,
                                                            'cursor-pointer': !noCloudLoading,
                                                        }">
                                                        <span>无云一版图生成</span>
                                                        <Loader v-if="noCloudLoading" class="ml-2" />
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                        <!-- 多源数据合成 -->
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <LayersIcon :size="16" class="config-icon" />
                                                <span class="text-base">复合数据合成</span>
                                                <el-tooltip content="使用任意数量任意波段进行无云一版图生成，支持计算NDVI、EVI等参数"
                                                    placement="top" effect="dark">
                                                    <CircleHelp :size="14" />
                                                </el-tooltip>
                                            </div>
                                            <div class="config-control flex-col !items-start">
                                                <div class="flex w-full flex-col gap-2">
                                                    <!-- 波段选择，写成循环格式 -->
                                                    <div class="ml-4 flex flex-col">
                                                        <div class="text-lg text-gray-400 mb-2">合成波段选择：</div>
                                                        <div class="grid grid-cols-3 gap-4">
                                                            <label
                                                                v-for="band in ['Red', 'Green', 'Blue', 'NIR', 'NDVI', 'EVI']"
                                                                :key="band" class="flex items-center gap-2">
                                                                <input type="checkbox" :value="band"
                                                                    v-model="multiSourceData.selectedBands"
                                                                    class="size-5 rounded" />
                                                                <span class="text-base">{{ band }}</span>
                                                            </label>
                                                        </div>
                                                    </div>

                                                    <!-- 可视化波段选择部分 -->
                                                    <div class="ml-4 mt-2 flex flex-row items-center gap-1">
                                                        <div class="text-lg text-gray-400">可视化波段：</div>

                                                        <!-- R通道（只读文本） -->
                                                        <div class="flex items-center gap-2">
                                                            <span class="text-sm text-red-400">R:</span>
                                                            <div class="w-17 rounded border border-[#2c3e50] bg-[#0d1526] text-[#38bdf8] 
                                                        flex items-center justify-center overflow-hidden">
                                                                {{ multiSourceData.visualization.red_band || "未分配" }}
                                                            </div>
                                                        </div>

                                                        <!-- G通道 -->
                                                        <div class="flex items-center gap-2">
                                                            <span class="text-sm text-green-400">G:</span>
                                                            <div class="w-17 rounded border border-[#2c3e50] bg-[#0d1526] text-[#38bdf8] 
                                                        flex items-center justify-center overflow-hidden">
                                                                {{ multiSourceData.visualization.green_band || "未分配" }}
                                                            </div>
                                                        </div>

                                                        <!-- B通道 -->
                                                        <div class="flex items-center gap-2">
                                                            <span class="text-sm text-blue-400">B:</span>
                                                            <div class="w-17 rounded border border-[#2c3e50] bg-[#0d1526] text-[#38bdf8] 
                                                        flex items-center justify-center overflow-hidden">
                                                                {{ multiSourceData.visualization.blue_band || "未分配" }}
                                                            </div>
                                                        </div>
                                                    </div>

                                                    <div v-if="showComplexProgress[0]"
                                                        class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                                        <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                            :style="{ width: `${complexProgress[0]}%` }"></div>
                                                    </div>
                                                </div>

                                                <!-- 结果信息 -->
                                                <div class="result-info-container w-full">
                                                    <div class="result-info-item">
                                                        <div class="result-info-icon">
                                                            <LayersIcon :size="16" />
                                                        </div>
                                                        <div class="result-info-content">
                                                            <div class="result-info-label">已选波段</div>
                                                            <div class="result-info-value">
                                                                <!-- 如果 selectedBands 为空，显示 "无" -->
                                                                <template
                                                                    v-if="multiSourceData.selectedBands.length === 0">
                                                                    无
                                                                </template>
                                                                <!-- 否则显示具体波段名称（用逗号分隔） -->
                                                                <template v-else>
                                                                    {{ multiSourceData.selectedBands.join(", ") }}
                                                                </template>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <button
                                                    class="w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95"
                                                    @click="handleMultiSourceData">
                                                    无云一版图生成
                                                </button>
                                            </div>
                                        </div>

                                        <!-- 多时相数据合成 -->
                                        <div class="config-item" v-show="false"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <CalendarIcon :size="16" class="config-icon opacity-50" />
                                                <span class="text-gray-500">多时相数据合成</span>
                                                <el-tooltip content="构建多时相波段数据集，融合不同时间的观测数据（暂时不可用）" placement="top"
                                                    effect="dark">
                                                    <CircleHelp :size="14" class="opacity-50" />
                                                </el-tooltip>
                                            </div>
                                            <div
                                                class="config-control flex-col !items-start opacity-50 pointer-events-none">
                                                <div class="flex w-full flex-col gap-2">
                                                    <!-- 时相配置 -->
                                                    <div class="ml-4 flex flex-col gap-2">
                                                        <div class="text-sm text-gray-500">时相配置：</div>
                                                        <div class="flex items-center gap-2">
                                                            <span class="text-sm text-gray-500">时相1：</span>
                                                            <a-date-picker v-model:value="multiTemporalData.date1"
                                                                size="small" placeholder="选择日期" disabled />
                                                            <span class="text-sm ml-2 text-gray-500">波段1-3</span>
                                                        </div>
                                                        <div class="flex items-center gap-2">
                                                            <span class="text-sm text-gray-500">时相2：</span>
                                                            <a-date-picker v-model:value="multiTemporalData.date2"
                                                                size="small" placeholder="选择日期" disabled />
                                                            <span class="text-sm ml-2 text-gray-500">波段4-6</span>
                                                        </div>

                                                        <button
                                                            class="w-full rounded-lg border border-gray-500 bg-gray-700 px-4 py-2 text-gray-400 cursor-not-allowed"
                                                            disabled>
                                                            合成
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!--时序立方体合成-->
                            <section class="panel-section" v-show="currentPanel === 'noCloud'" key="noCloud">
                                <!--标题-->
                                <div class="section-header">
                                    <div class="section-icon">
                                        <BoxIcon :size="18" />
                                    </div>
                                    <h2 class="section-title">时序立方体合成</h2>
                                    <div class="absolute right-2 cursor-pointer">
                                        <ChevronDown v-if="isNoCloudExpand" :size="22"
                                            @click="isNoCloudExpand = false" />
                                        <ChevronUp v-else @click="isNoCloudExpand = true" :size="22" />
                                    </div>
                                </div>

                                <!--简单合成内容区域-->
                                <div v-show="isNoCloudExpand" class="section-content">
                                    <div class="config-container">
                                        <a-alert
                                            :description="selectedGrid ? `已选择立方体${selectedGrid.rowId}-${selectedGrid.columnId}-${selectedGrid.resolution}` : '请先在地图中选择立方体'"
                                            :type="selectedGrid ? 'info' : 'warning'" show-icon class="status-alert" />
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span>立方体合成</span>
                                            </div>
                                            <div class="config-item">
                                                <a-form :model="formData" :label-col="{ span: 6 }"
                                                    :wrapper-col="{ span: 18 }" @finish="onFinish">
                                                    <!-- 传感器选择 -->
                                                    <a-form-item label="传感器选择" name="sensors"
                                                        :rules="[{ required: true, message: '请至少选择一个传感器' }]">
                                                        <a-select v-model:value="formData.sensors" mode="multiple"
                                                            placeholder="请选择传感器..." :options="sensorOptions" allow-clear
                                                            :max-tag-count="3" :max-tag-text-length="10"
                                                            @change="handleSensorChange">
                                                        </a-select>
                                                        <div>
                                                            支持多传感器数据融合，提高时间覆盖密度
                                                        </div>
                                                    </a-form-item>

                                                    <!-- 波段选择 -->
                                                    <a-form-item label="波段选择" name="bands"
                                                        :rules="[{ required: true, message: '请至少选择一个波段' }]">
                                                        <a-select v-model:value="formData.bands" mode="multiple"
                                                            placeholder="请选择波段..." allow-clear :max-tag-count="3"
                                                            :max-tag-text-length="8" @change="handleBandChange">
                                                            <a-select-option v-for="band in bandOptions"
                                                                :key="band.value" :value="band.value">
                                                                <span :style="{ color: band.color, fontSize: 'bold' }">
                                                                    {{ band.label }}</span>
                                                            </a-select-option>
                                                        </a-select>
                                                        <div>
                                                            选择需要的光谱波段进行分析
                                                        </div>
                                                    </a-form-item>

                                                    <!-- 时间范围 -->
                                                    <a-form-item label="时间范围" name="dates"
                                                        :rules="[{ required: true, message: '请选择时间节点' }]">
                                                        <a-select v-model:value="formData.dates" placeholder="请选择时间节点"
                                                            allow-clear :max-tag-count="5" :max-tag-text-length="10"
                                                            @change="handleDateChange" mode="multiple">
                                                            <a-select-option v-for="date in dateOptions" :key="date"
                                                                :value="date">
                                                                <span :style="{ fontSize: 'bold' }">
                                                                    {{ date }}</span>
                                                            </a-select-option>
                                                        </a-select>
                                                        <div>
                                                            立方体时间维度的采样时间
                                                        </div>
                                                    </a-form-item>

                                                </a-form>
                                            </div>

                                            <!-- 操作按钮区域 -->
                                            <div class="config-control justify-end" :bordered="false">
                                                <a-button size="large" style="margin-right: 1rem;" :disabled="!canSynthesize"
                                                    @click="handleReset">
                                                    重置
                                                </a-button>
                                                <a-button type="primary" size="large" :disabled="!canSynthesize"
                                                    @click="handleSynthesis">
                                                    合成立方体
                                                </a-button>

                                            </div>
                                            <a-modal v-model:open="showCubeContentDialog" title="时序立方体" @ok="() => showCubeContentDialog = false">
                                                <a-card style="max-height: 400px; overflow: auto; position: relative;">
                                                    <a-alert
                                                        :description="`请牢记时序立方体CacheKey: ${currentCacheKey}`"
                                                        type="warning" show-icon class="status-alert" />
                                                    <pre
                                                        style="white-space: pre-wrap; word-break: break-word; user-select: text;"
                                                        >
                                                        {{ cubeContent }}
                                                    </pre>
                                                </a-card>
                                            </a-modal>
                                            
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!--历史记录-->
                            <section class="panel-section" v-if="currentPanel === 'history'" key="history">
                                <noCloudHistory @toggle="setCurrentPanel" />
                            </section>

                        </div>
                    </dv-border-box12>
                </div>
            </div>
        </div>
        <MapComp class="flex-1" :style="'local'" :proj="'globe'" :isPicking="isPicking" />
    </div>
</template>

<script setup lang="ts">
import MapComp from '@/components/feature/map/mapComp.vue'
import { onMounted } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import noCloudHistory from '@/components/dataCenter/noCloud/noCloudHistory.vue'
import { formatTime } from '@/util/common'
import * as MapOperation from '@/util/map/operation'
import * as CommonMapOps from '@/util/map/operation/common'
import { mapManager } from '@/util/map/mapManager'
import router from '@/router'
import subtitle from '../subtitle.vue'
import { useI18n } from 'vue-i18n'
import bbox from '@turf/bbox'

import {
    Loader,
    DatabaseIcon,
    MapPinIcon,
    CircleHelp,
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
    History,
    CloudOffIcon,
    ChevronDown,
    ChevronRightIcon,
    ChevronLeftIcon,
    ChevronUp,
    FilterIcon,
    BoxIcon,
    CopyCheckIcon,
    CopyIcon,
} from 'lucide-vue-next'

// 导入组合式函数
import {
    showPanel,
    currentPanel,
    setCurrentPanel,
    isNoCloudExpand,
    isComplexExpand,
    isPicking,
    noCloudLoading,
    calTask,
    additionalData,
    dataReconstruction,
    progress,
    showProgress,
    complexProgress,
    showComplexProgress,
    selectnation,
    selectinternation,
    selectsar,
    exploreData
} from './composables/shared'

import { useDataPreparation } from './composables/useDataPreparation'
import { useGridRendering } from './composables/useGridRendering'
import { useSensorSelection } from './composables/useSensorSelection'
import { useNoCloudCalculation } from './composables/useNoCloudCalculation'
import { useComplexSynthesis } from './composables/useComplexSynthesis'
import { useBox } from './composables/useBox'
import { message } from 'ant-design-vue'


const { t } = useI18n()

// 所有的数据准备
const {
    demotic1mImages,
    demotic2mImages,
    internationalImages,
    radarImages,
    nation1mPlatformList,
    nation2mPlatformList,
    internationalLightPlatformList,
    SARPlatformList,
    allScenes,
    dataPrepare,
    getGridStatsByType
} = useDataPreparation()

// 格网渲染
const {
    generateGridId,
    shouldRenderGrid,
    createGridFeatureCollection,
    checkDataTypeEnabled,
    clearGridRenderingByType,
    clearAllGridRendering,
    renderGrids,
    reRenderAllGrids,
    initGridRendering
} = useGridRendering(getGridStatsByType)

//传感器选择
const {
    handleShowSensorImage,
    add1mDemoticImage,
    add2mDemoticImages,
    addAbroadImages,
    addRadarImages,
    handleDataReconstructionChange
} = useSensorSelection()

//无云计算
const {
    calImage,
    showingImageStrech,
    progressControl,
    controlProgress,
    cancelCheckbox,
    calNoClouds,
    previewNoCloud,
    handleCreateNoCloudTiles
} = useNoCloudCalculation(allScenes)


const {
    multiSourceData,
    multiTemporalData,
    handleMultiSourceData,
    handleMultitTemporalData,
    addTimePhase,
    controlComplexProgress
} = useComplexSynthesis(allScenes)

// 时空立方体合成
const {
    selectedGrid, updateGridLayer, formData, sensorOptions, bandOptions, dateOptions, canSynthesize, handleSensorChange, handleBandChange, handleDateChange, handleSynthesis, handleReset, onFinish, cubeContent, currentCacheKey, showCubeContentDialog
} = useBox()

const handleAdd1mDemoticImage = () => add1mDemoticImage(reRenderAllGrids, clearGridRenderingByType)
const handleAdd2mDemoticImages = () => add2mDemoticImages(renderGrids, clearGridRenderingByType)
const handleAddAbroadImages = () => addAbroadImages(renderGrids, clearGridRenderingByType)
const handleAddRadarImages = () => addRadarImages(renderGrids, clearGridRenderingByType)
const handleDataReconstructionChangeWrapper = (index: number) => handleDataReconstructionChange(index, reRenderAllGrids, clearGridRenderingByType)

onMounted(async () => {
    try {
        dataPrepare()
    } catch {
        console.log('获取数据失败')
    }

    if (!exploreData.load) {
        message.error(t('nav.disabled_message'))
        router.push('/')
    }

    // 初始化格网渲染
    await initGridRendering()

    // 显示已筛选的边界
    console.log(exploreData)
    console.log(exploreData.boundary, 'exploreData.boundary')
    setTimeout(() => {
        if (exploreData.load) {
            // 行政边界铺设
            MapOperation.map_addPolygonLayer({
                geoJson: exploreData.boundary,
                id: 'UniqueLayer',
                lineColor: '#8fffff',
                fillColor: '#a4ffff',
                fillOpacity: 0.2,
            })
            // 格网底色铺设
            updateGridLayer(exploreData.grids)
            
        }
    }, 2)
    // 缩放至研究区
    setTimeout(() => {
        const boundsArray = bbox(exploreData.boundary as any)
        const bounds = [
            [boundsArray[0], boundsArray[1]],
            [boundsArray[2], boundsArray[3]]
        ]
        CommonMapOps.map_fitView(bounds)
    },1500)
})
</script>

<style scoped src="../tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}
</style>