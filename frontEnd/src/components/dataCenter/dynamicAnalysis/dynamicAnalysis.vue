<template>
    <div class="relative flex flex-1 h-full flex-row bg-black ">
        <a-tour v-model:current="current" :open="openTour" :steps="steps" @close="handleOpenTour(false)" />
        <subtitle class="z-10 absolute" style="margin-top: 60px; " />
        <div class="absolute left-16 z-10 h-[calc(100vh-100px)] p-4 text-gray-200"
            :class="isToolbarOpen ? 'w-[545px]' : 'w-16 transition-all duration-300'">
            <button @click="isToolbarOpen = !isToolbarOpen" class="absolute top-1/2 right-0 -translate-y-1/2 h-12 w-6 text-white rounded-l-lg shadow-lg
                 items-center justify-center transition-all z-10"
                :class="isToolbarOpen ? 'bg-blue-600 hover:bg-blue-500' : 'bg-gray-800 hover:bg-gray-700'">
                <ChevronRightIcon :size="16" class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': isToolbarOpen }" />
            </button>
            <div v-if="isToolbarOpen">
                <!--顶部标题+历史记录图标-->
                <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            📈
                        </div>
                        <span class="page-title">展示分析</span>
                        <div class="ml-2 cursor-pointer" @click="handleOpenTour(true)"
                            v-if="currentPanel === 'analysis'">
                            <a-tooltip>
                                <template #title>点击查看帮助</template>
                                <QuestionCircleOutlined :size="20" />
                            </a-tooltip>
                        </div>
                        <div class="section-icon absolute right-12 cursor-pointer" @click="clearImages">
                            <a-tooltip>
                                <template #title>{{ t('datapage.analysis.section2.clear')
                                }}</template>
                                <Trash2Icon :size="20" />
                            </a-tooltip>
                        </div>
                        <a-space class="section-icon absolute right-2 cursor-pointer" ref="ref3">
                            <a-tooltip>
                                <template #title>历史记录</template>
                                <History :size="18" @click="setCurrentPanel('history')" />
                            </a-tooltip>
                        </a-space>
                    </div>
                </section>
                <!-- 内容区域 -->
                <div class="custom-panel px-2">
                    <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
                        <!--主容器-->
                        <div class="main-container">
                            <a-alert v-if="exploreData.grids.length === 0 && currentPanel !== 'history'"
                                description="请先完成交互探索" type="warning" show-icon class="status-alert">
                                <template #action>
                                    <a-button size="small" @click="router.push('/explore')">前往</a-button>
                                </template>
                            </a-alert>
                            <br v-if="exploreData.grids.length === 0 && currentPanel !== 'history'" />

                            <!-- 工具目录部分 -->
                            <section class="panel-section" v-if="currentPanel !== 'history'">
                                <!--工具目录标题-->
                                <a-space class="section-header" ref="ref2">
                                    <div class="section-icon">
                                        <LayersIcon :size="18" />
                                    </div>
                                    <h2 class="section-title">景级分析</h2>
                                    <div class="absolute right-2 cursor-pointer">
                                        <ChevronDown v-if="isSceneAnalysisExpand" :size="22"
                                            @click="isSceneAnalysisExpand = false" />
                                        <ChevronUp v-else @click="isSceneAnalysisExpand = true" :size="22" />
                                    </div>
                                </a-space>
                                <div class="section-content" v-show="isSceneAnalysisExpand" style="padding-bottom: 0%;">
                                    <div class="stats"
                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                        <div class="stats-header">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span class="text-sm">通用分析工具</span>
                                            </div>
                                            <div class="absolute right-2 cursor-pointer">
                                                <ChevronDown v-if="isMethLibExpand" :size="22"
                                                    @click="isMethLibExpand = false" />
                                                <ChevronUp v-else @click="isMethLibExpand = true" :size="22" />
                                            </div>
                                        </div>
                                        <div class="stats-content" v-if="isMethLibExpand">
                                            <!-- 空间位置配置 -->
                                            <div class="config-item-no-hover">
                                                <div class="config-label relative">
                                                    <MapIcon :size="16" class="config-icon" />
                                                    <span>数据筛选与配置</span>
                                                </div>
                                                <a-row :gutter="[16, 8]" align="middle">
                                                    <a-col :span="8">
                                                        <a-form-item label="当前格网分辨率" :label-col="{ span: 24 }"
                                                            :wrapper-col="{ span: 24 }" style="margin-bottom: 0;">
                                                            <a-statistic :value="exploreData.gridResolution" suffix="km"
                                                                :value-style="{ color: '#40a9ff', fontSize: '16px', fontWeight: 'bold' }"
                                                                :title-style="{ fontSize: '12px', color: '#a0a0a0' }" />
                                                        </a-form-item>
                                                    </a-col>

                                                    <a-col :span="16">
                                                        <a-form-item label="当前时间范围" :label-col="{ span: 24 }"
                                                            :wrapper-col="{ span: 24 }" style="margin-bottom: 0;">
                                                            <span style="font-size: 14px; color: #ffffff;">
                                                                {{ dayjs(exploreData.dataRange[0]).format("YYYY-MM-DD")
                                                                }}
                                                                <a-divider type="vertical"
                                                                    style="border-color: #6a6a6a;" />
                                                                {{ dayjs(exploreData.dataRange[1]).format("YYYY-MM-DD")
                                                                }}
                                                            </span>
                                                        </a-form-item>
                                                    </a-col>
                                                </a-row>
                                                <a-row>
                                                    <a-col :span="24">
                                                        <a-form-item label="传感器选择" name="sensors"
                                                            :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
                                                            <a-select v-model:value="selectedSensorName"
                                                                placeholder="请选择传感器..." :options="exploreData.sensors.map(sensor => ({
                                                                    label: sensor.platformName,
                                                                    value: sensor.sensorName
                                                                }))" allow-clear
                                                                @change="(value: string) => getPlatformDataFile(value)"
                                                                style="width: 100%;" :style="{
                                                                    backgroundColor: '#383838',
                                                                    borderColor: '#4d4d4d',
                                                                    color: '#ffffff'
                                                                }">
                                                            </a-select>
                                                        </a-form-item>
                                                    </a-col>
                                                </a-row>
                                            </div>
                                        </div>
                                        <div class="stats-content" v-show="isMethLibExpand">
                                            <div class="config-item-no-hover">
                                                <div class="config-label relative">
                                                    <BoltIcon :size="16" class="config-icon" />
                                                    <span>工具检索</span>
                                                </div>
                                                <div class="grid grid-cols-2 gap-3">
                                                    <a-checkable-tag v-for="(tag, index) in tagList" :key="tag.id"
                                                        v-model:checked="selectTags[index]">
                                                        {{ tag.name }}
                                                    </a-checkable-tag>
                                                </div>
                                                <div class="config-control">
                                                    <a-input-search v-model:value="searchQuery" placeholder="输入关键词..."
                                                        enter-button="搜索" @search="getMethLibList" />
                                                </div>
                                            </div>


                                            <div class="mt-5">
                                                <!-- 分类工具列表 -->
                                                <div v-for="(item, index) in methLibList" class="config-item mb-1"
                                                    :key="item.id">
                                                    <div class="config-label relative">
                                                        <Image :size="16" class="config-icon" />
                                                        <span>🛠️ {{ `${item.name}` }}</span>
                                                        <div class="absolute right-0 cursor-pointer">
                                                            <a-tooltip>
                                                                <template #title>调用</template>
                                                                <LogInIcon class="cursor-pointer" :size="16"
                                                                    @click="openModal(item)" />
                                                            </a-tooltip>
                                                        </div>
                                                    </div>
                                                    <div class="config-control flex-col !items-start">
                                                        <div class="flex w-full flex-col gap-2">
                                                            <div class="result-info-container">
                                                                <div class="result-info-value">
                                                                    <span class="text-sm">{{ item.description }}</span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="flex h-[60px] justify-around">
                                                    <el-pagination v-if="methLibTotal > 0" background
                                                        layout="prev, pager, next"
                                                        v-model:current-page="currentMethLibPage" :total="methLibTotal"
                                                        :page-size="methLibPageSize" @current-change="getMethLibList"
                                                        @next-click="" @prev-click="">
                                                    </el-pagination>
                                                </div>
                                                <a-empty v-if="methLibTotal === 0" />
                                                <!-- ✅ 调用弹窗组件 -->
                                                <invoke-modal v-if="showModal" v-model="showModal"
                                                    @invoke-method="(params) => handleInvoke(params)"
                                                    :method-item="selectedItem as any" @close="showModal = false" />
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="section-content" v-show="isSceneAnalysisExpand">
                                    <div class="stats"
                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%); margin-top: -2%;">
                                        <div class="stats-header">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span class="text-sm">UDF分析工具</span>
                                            </div>
                                            <div class="absolute right-2 cursor-pointer">
                                                <ChevronDown v-if="isToolsExpand" :size="22"
                                                    @click="isToolsExpand = false" />
                                                <ChevronUp v-else @click="isToolsExpand = true" :size="22" />
                                            </div>
                                        </div>
                                        <div class="stats-content" v-show="isToolsExpand">
                                            <div class="config-control relative">

                                                <!-- 数据集配置 -->
                                                <div class="config-container w-full">
                                                    <div class="config-item"
                                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                                        <div class="config-label relative">
                                                            <ChartColumn :size="16" class="config-icon" />
                                                            <span>数据集</span>
                                                        </div>
                                                        <div class="config-control">
                                                            <button @click="showHistory = !showHistory"
                                                                class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-4 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] truncate">
                                                                前序数据
                                                            </button>
                                                            <el-dialog v-model="showHistory"
                                                                class="max-w-[90vw] md:max-w-[80vw] lg:max-w-[70vw] xl:max-w-[60vw]"
                                                                style="background-color: #111827; color: white;">
                                                                <div class="mb-6 text-gray-100">前序数据集</div>

                                                                <div v-if="completedCases.length > 0"
                                                                    class="max-h-[500px] overflow-y-auto">
                                                                    <div v-for="item in completedCases"
                                                                        :key="item.caseId" class="p-4 mb-3 border border-gray-200 rounded-md
                                                            cursor-pointer transition-all duration-300
                                                            hover:bg-gray-50 hover:shadow-md"
                                                                        @click="getAndShowResult(item.caseId, item.regionId)">
                                                                        <h3 class="mt-0 text-blue-500">{{ item.address
                                                                        }}无云一版图</h3>
                                                                        <p class="my-1 text-blue-300">分辨率: {{
                                                                            item.resolution }}km
                                                                        </p>
                                                                        <p class="my-1 text-blue-300">创建时间: {{
                                                                            formatTimeToText(item.createTime) }}</p>
                                                                        <p class="my-1 text-blue-300">数据集: {{
                                                                            item.dataSet
                                                                        }}</p>
                                                                    </div>
                                                                </div>
                                                                <div v-else>
                                                                    <p class="item-center text-center text-gray-100">
                                                                        暂无数据
                                                                    </p>
                                                                </div>
                                                            </el-dialog>
                                                        </div>
                                                    </div>

                                                    <div class="config-item mt-4"
                                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #05375d 0%, #07243a 97%);">
                                                        <div class="config-label relative pr-16">
                                                            <CommandIcon :size="16" class="config-icon" />
                                                            <span>发布的工具</span>
                                                            <span
                                                                class="absolute right-4 top-1/2 -translate-y-1/2 text-xs text-gray-400">{{
                                                                    publishedSceneTools.length }} 个可用
                                                            </span>
                                                        </div>
                                                        <div class="config-control flex-col !items-start w-full">
                                                            <div v-if="publishedSceneTools.length"
                                                                class="flex w-full flex-col gap-3">
                                                                <div v-for="tool in publishedSceneTools" :key="tool.id"
                                                                    class="w-full rounded-xl border border-[#1c2a3f] bg-[#0b1221]/80 px-4 py-3 shadow-inner transition-colors duration-200 hover:border-[#38bdf8] cursor-pointer"
                                                                    @click="handleInvokeSceneTool(tool.id)">
                                                                    <div class="flex items-start justify-between gap-3">
                                                                        <div class="min-w-0">
                                                                            <p
                                                                                class="m-0 text-sm font-medium text-gray-100 truncate">
                                                                                {{ tool.name }}</p>
                                                                            <span class="text-xs text-gray-400">{{
                                                                                tool.category
                                                                                ||
                                                                                '未分类' }}</span>
                                                                        </div>
                                                                        <a-button size="small" type="primary" ghost
                                                                            @click.stop="handleInvokeSceneTool(tool.id)">
                                                                            调用
                                                                        </a-button>
                                                                    </div>
                                                                    <p
                                                                        class="mt-2 text-xs text-gray-300 leading-relaxed min-h-[32px]">
                                                                        {{ tool.description || '暂无描述，点击调用以查看工具详情。'
                                                                        }}
                                                                    </p>
                                                                    <div
                                                                        class="mt-2 flex flex-wrap items-center gap-2 text-[10px] text-gray-400">
                                                                        <template v-if="tool.tags && tool.tags.length">
                                                                            <span v-for="tag in tool.tags" :key="tag"
                                                                                class="rounded-full border border-[#1e2d44] bg-[#0f172a] px-2 py-0.5 text-[#38bdf8]">
                                                                                {{ tag }}
                                                                            </span>
                                                                        </template>
                                                                        <span class="ml-auto text-[10px] text-gray-500"
                                                                            v-if="tool.updatedAt || tool.createdAt">
                                                                            更新 {{ formatTimeToText(tool.updatedAt ||
                                                                                tool.createdAt) }}
                                                                        </span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                            <div v-else
                                                                class="flex w-full flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-[#1f2e44] bg-[#0b1221]/40 px-6 py-8 text-gray-400">
                                                                <p class="m-0 text-sm font-medium">暂无发布的工具</p>
                                                                <p class="m-0 text-xs text-gray-500">在在线编程中发布后即可在此调用。
                                                                </p>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="section-content" v-show="isSceneAnalysisExpand">
                                    <div class="stats"
                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%); margin-top: -5%;">
                                        <div class="stats-header">
                                            <div class="config-label relative">
                                                <BoltIcon :size="16" class="config-icon" />
                                                <span class="text-sm">平台内置分析工具</span>
                                            </div>
                                            <div class="absolute right-2 cursor-pointer">
                                                <ChevronDown v-if="isDefaultToolsExpand" :size="22"
                                                    @click="isDefaultToolsExpand = false" />
                                                <ChevronUp v-else @click="isDefaultToolsExpand = true" :size="22" />
                                            </div>
                                        </div>

                                        <div class="stats-content" v-show="isDefaultToolsExpand">
                                            <div class="config-control relative">
                                                <section>
                                                    <!-- 空间位置配置 -->
                                                    <div class="config-item"
                                                        style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                                        <div class="config-label relative">
                                                            <MapIcon :size="16" class="config-icon" />
                                                            <span>{{ t('datapage.analysis.section1.area') }}</span>
                                                        </div>
                                                        <div class="config-control justify-center">
                                                            <RegionSelects v-model="region" class="flex gap-2"
                                                                select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                                        </div>
                                                    </div>
                                                    <div class="mt-4 w-full mr-4">
                                                        <div v-for="category in builtinToolCategories"
                                                            :key="category.name" class="mb-4">
                                                            <div class="flex items-center cursor-pointer px-2 py-1 hover:bg-gray-800 rounded"
                                                                @click="toggleCategory(category.name)">
                                                                <ChevronRightIcon :size="16"
                                                                    class="mr-2 transition-transform duration-200"
                                                                    :class="{ 'transform rotate-90': expandedCategories.includes(category.name) }" />
                                                                <span class="text-gray-300 font-medium">{{ category.name
                                                                    }}</span>
                                                            </div>

                                                            <div v-show="expandedCategories.includes(category.name) || searchQuery"
                                                                class="ml-6 mt-2 grid grid-cols-2 gap-2">
                                                                <div v-for="tool in category.tools" :key="tool.value"
                                                                    @click="selectedTask = tool.value" :class="{
                                                                        'bg-[#1e3a8a] text-white': selectedTask === tool.value,
                                                                        'bg-[#0d1526] text-gray-300 hover:bg-[#1e293b]': selectedTask !== tool.value && !tool.disabled,
                                                                        'opacity-50 cursor-not-allowed': tool.disabled,
                                                                        'cursor-pointer': !tool.disabled
                                                                    }"
                                                                    class="px-3 py-1 rounded-lg transition-colors w-full text-left flex items-center justify-between"
                                                                    :disabled="tool.disabled">

                                                                    <a-tooltip :title="tool.label"
                                                                        class="flex-grow min-w-0">
                                                                        <span class="truncate block text-sm">{{
                                                                            tool.label
                                                                            }}</span>
                                                                    </a-tooltip>

                                                                    <CircleX v-if="tool.value.startsWith('dynamic:')"
                                                                        :size="16"
                                                                        class="text-gray-400 hover:text-gray-300 flex-shrink-0 ml-1"
                                                                        @click.stop="handleRemoveDynamicTool(tool.value)" />
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </section>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!-- 前序数据分析部分 -->
                            <section class="panel-section" v-if="currentPanel !== 'history'">
                                <!--标题-->
                                <div class="section-header">
                                    <div class="section-icon">
                                        <Settings :size="18" />
                                    </div>
                                    <h2 class="section-title">Cube 级分析</h2>
                                    <div class="absolute right-2 cursor-pointer">
                                        <ChevronDown v-if="isCubeExpand" :size="22" @click="isCubeExpand = false" />
                                        <ChevronUp v-else @click="isCubeExpand = true" :size="22" />
                                    </div>
                                </div>

                                <!--内容区域-->
                                <div v-show="isCubeExpand" class="section-content">
                                    <div class="config-container">


                                        <!-- 立方体配置 -->
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <BoxIcon :size="16" class="config-icon" />
                                                <span>时序立方体</span>
                                            </div>
                                            <a-alert v-if="exploreData.grids.length === 0"
                                                :message="`已选择${cubeList.filter(item => item.isSelect).length}个时序立方体`"
                                                type="info" show-icon class="status-alert">
                                            </a-alert>
                                            <a-form layout="inline">
                                                <a-form-item class="w-full">
                                                    <a-input v-model:value="inputCacheKey"
                                                        placeholder="键入CacheKey（按Enter以选择）"
                                                        @keyup.enter="handleSelectCube(inputCacheKey)">
                                                        <template #prefix>
                                                            <CommandIcon :size="14"
                                                                style="color: rgba(255, 255, 255, 0.25)" />
                                                        </template>
                                                    </a-input>
                                                </a-form-item>
                                            </a-form>
                                            <div class="max-h-[268px] overflow-y-auto">
                                                <a-modal v-model:open="currentCacheKey" title="时序立方体"
                                                    @ok="() => { cubeList.filter(cube => cube.cacheKey === currentCacheKey)[0].isShow = false; currentCacheKey = undefined }"
                                                    @cancel="() => { cubeList.filter(cube => cube.cacheKey === currentCacheKey)[0].isShow = false; currentCacheKey = undefined }">
                                                    <a-card
                                                        style="max-height: 400px; overflow: auto; position: relative;">
                                                        <pre
                                                            style="white-space: pre-wrap; word-break: break-word; user-select: text;">
                                            {{cubeList.filter(cube => cube.cacheKey === currentCacheKey)[0]}}
                                        </pre>
                                                    </a-card>
                                                </a-modal>
                                                <a-list item-layout="horizontal" class="w-full" :data-source="cubeList">
                                                    <template #renderItem="{ item }">
                                                        <a-list-item>
                                                            <template #actions>
                                                                <div>
                                                                    <Eye v-if="item.isShow" :size="16"
                                                                        class="cursor-pointer"
                                                                        @click="currentCacheKey = undefined; item.isShow = false">
                                                                    </Eye>
                                                                    <EyeOff v-else :size="16" class="cursor-pointer"
                                                                        @click="currentCacheKey = item.cacheKey; item.isShow = true">
                                                                    </EyeOff>
                                                                </div>
                                                                <div>
                                                                    <Square v-if="!item.isSelect" :size="16"
                                                                        class="cursor-pointer"
                                                                        @click="handleSelectCube(item.cacheKey)">
                                                                    </Square>
                                                                    <SquareCheck v-else :size="16"
                                                                        class="cursor-pointer"
                                                                        @click="handleSelectCube(item.cacheKey)">
                                                                    </SquareCheck>
                                                                </div>
                                                            </template>
                                                            <a-list-item-meta
                                                                :description="`${item.dimensionDates.length}维时序立方体, 包含${item.dimensionSensors.length}类传感器, ${item.dimensionScenes.length}景影像`">
                                                                <template #title>
                                                                    {{ formatTimeToText(item.cacheTime) }}
                                                                </template>
                                                                <template #avatar>
                                                                    <div class="section-icon">
                                                                        <BoxIcon :size="14" />
                                                                    </div>
                                                                </template>
                                                            </a-list-item-meta>
                                                        </a-list-item>
                                                    </template>
                                                </a-list>
                                            </div>
                                        </div>
                                        <!-- 发布的工具（与上方样式一致） -->
                                        <div class="config-item mt-4"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #05375d 0%, #07243a 97%);">
                                            <div class="config-label relative pr-16">
                                                <CommandIcon :size="16" class="config-icon" />
                                                <span>发布的工具</span>
                                                <span
                                                    class="absolute right-4 top-1/2 -translate-y-1/2 text-xs text-gray-400">{{
                                                        publishedCubeTools.length }} 个可用
                                                </span>
                                            </div>
                                            <div class="config-control flex-col !items-start w-full">
                                                <div v-if="publishedCubeTools.length"
                                                    class="flex w-full flex-col gap-3">
                                                    <div v-for="tool in publishedCubeTools" :key="tool.id" :class="[
                                                        'w-full rounded-xl border border-[#1c2a3f] bg-[#0b1221]/80 px-4 py-3 shadow-inner transition-colors duration-200 hover:border-[#38bdf8] cursor-pointer',
                                                        { 'border-[#38bdf8] bg-[#14243f]': activeCubeToolId === tool.id }
                                                    ]" @click="handleInvokeCubeTool(tool.id)">
                                                        <div class="flex items-start justify-between gap-3">
                                                            <div class="min-w-0">
                                                                <p
                                                                    class="m-0 text-sm font-medium text-gray-100 truncate">
                                                                    {{ tool.name }}</p>
                                                                <span class="text-xs text-gray-400">{{ tool.category ||
                                                                    '未分类'
                                                                    }}</span>
                                                            </div>
                                                            <a-button size="small" type="primary" ghost
                                                                @click.stop="handleInvokeCubeTool(tool.id)">调用</a-button>
                                                        </div>
                                                        <p
                                                            class="mt-2 text-xs text-gray-300 leading-relaxed min-h-[32px]">
                                                            {{ tool.description || '暂无描述，点击调用以查看工具详情。' }}
                                                        </p>
                                                        <div
                                                            class="mt-2 flex flex-wrap items-center gap-2 text-[10px] text-gray-400">
                                                            <template v-if="tool.tags && tool.tags.length">
                                                                <span v-for="tag in tool.tags" :key="tag"
                                                                    class="rounded-full border border-[#1e2d44] bg-[#0f172a] px-2 py-0.5 text-[#38bdf8]">
                                                                    {{ tag }}
                                                                </span>
                                                            </template>
                                                            <span class="ml-auto text-[10px] text-gray-500"
                                                                v-if="tool.updatedAt || tool.createdAt">
                                                                更新 {{ formatTimeToText(tool.updatedAt || tool.createdAt)
                                                                }}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div v-else
                                                    class="flex w-full flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-[#1f2e44] bg-[#0b1221]/40 px-6 py-8 text-gray-400">
                                                    <p class="m-0 text-sm font-medium">暂无发布的 Cube 工具</p>
                                                    <p class="m-0 text-xs text-gray-500">发布 Cube 级分析工具后即可在此调用。</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>
                            <!-- <section>即将废弃的部分：
                                分类工具列表
                                <div class="mt-4 w-full mr-4">
                                    <div v-for="category in builtinToolCategories" :key="category.name" class="mb-4">
                                        <div class="flex items-center cursor-pointer px-2 py-1 hover:bg-gray-800 rounded"
                                            @click="toggleCategory(category.name)">
                                            <ChevronRightIcon :size="16" class="mr-2 transition-transform duration-200"
                                                :class="{ 'transform rotate-90': expandedCategories.includes(category.name) }" />
                                            <span class="text-gray-300 font-medium">{{ category.name
                                                }}</span>
                                        </div>

                                        <div v-show="expandedCategories.includes(category.name) || searchQuery"
                                            class="ml-6 mt-2 grid grid-cols-2 gap-2">
                                            <div v-for="tool in category.tools" :key="tool.value"
                                                @click="selectedTask = tool.value" :class="{
                                                    'bg-[#1e3a8a] text-white': selectedTask === tool.value,
                                                    'bg-[#0d1526] text-gray-300 hover:bg-[#1e293b]': selectedTask !== tool.value && !tool.disabled,
                                                    'opacity-50 cursor-not-allowed': tool.disabled,
                                                    'cursor-pointer': !tool.disabled
                                                }"
                                                class="px-3 py-1 rounded-lg transition-colors w-full text-left flex items-center justify-between"
                                                :disabled="tool.disabled">

                                                <a-tooltip :title="tool.label" class="flex-grow min-w-0">
                                                    <span class="truncate block text-sm">{{
                                                        tool.label
                                                        }}</span>
                                                </a-tooltip>

                                                <CircleX v-if="tool.value.startsWith('dynamic:')" :size="16"
                                                    class="text-gray-400 hover:text-gray-300 flex-shrink-0 ml-1"
                                                    @click.stop="handleRemoveDynamicTool(tool.value)" />
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section> -->
                            <!--历史记录-->
                            <section class="panel-section" v-if="currentPanel === 'history'" key="history">
                                <InvokeHistory @toggle="setCurrentPanel" />
                            </section>
                        </div>
                    </dv-border-box12>
                </div>
            </div>
        </div>
        <!-- <ImageSearcher class="h-full w-[28vw] mt-10" /> -->

        <!-- 展示页面 -->
        <div class="absolute right-0 top-0 h-full flex items-center z-10 ">
            <!-- Toggle button -->
            <button @click="showPanel = !showPanel" class="mt-10 h-12 w-6 bg-gray-800 hover:bg-gray-700 text-white rounded-l-lg shadow-lg 
                flex items-center justify-center transition-all z-10" :class="{ '!bg-blue-600': showPanel }">
                <ChevronLeftIcon :size="16" class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': showPanel }" />
            </button>
            <!-- <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            📈
                        </div>
                        <span class="page-title">展示分析</span>
                    </div>
                </section> -->
            <div v-if="showPanel">
                <div class="custom-panel px-2 mt-20">
                    <!-- <dv-border-box12 class=" !h-full"> -->
                    <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]"
                        style="width:426px; background-color: rgba(20, 20, 21, 0.6); border-radius: 3%;">
                        <DynamicCubeServicePanel v-if="activeCubeToolMeta" :tool-meta="activeCubeToolMeta"
                            :selected-cubes="selectedCubeItems" />
                        <component v-else-if="currentTaskComponent" :is="currentTaskComponent"
                            v-bind="currentTaskProps" />
                        <ResultComponent @response="handleResultLoaded" />
                    </dv-border-box12>
                </div>
            </div>
        </div>
        <MapComp class="!flex-1" :style="'local'" :proj="'globe'" :isPicking="isPicking" />
    </div>
</template>

<script setup lang="ts">
import { ref, type PropType, computed, type Ref, nextTick, onUpdated, onMounted, reactive, onBeforeUnmount, watch, defineAsyncComponent, type ComponentPublicInstance, onUnmounted } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import * as MapOperation from '@/util/map/operation'
import { useViewHistoryModule } from '../noCloud/viewHistory'
import InvokeModal from "@/components/dataCenter/dynamicAnalysis/InvokeModal.vue"
import router from '@/router'


import {
    ChartColumn,
    LayersIcon,
    MapIcon,
    Trash2Icon,
    ChevronLeftIcon,
    ChevronRightIcon,
    ChevronDown,
    ChevronUp,
    SearchIcon,
    Settings,
    BoxIcon,
    Eye,
    EyeOff,
    Square,
    SquareCheck,
    CommandIcon,
    CircleX,
    LogInIcon,
    History
} from 'lucide-vue-next'
import { dayjs } from 'element-plus'
import { mapManager } from '@/util/map/mapManager'
import { formatTimeToText } from '@/util/common';
import { ElDialog } from 'element-plus'
import { type Case } from '@/api/http/satellite-data'
import subtitle from '../subtitle.vue'
import { useSettings } from './composables/useSettings'
import { useI18n } from 'vue-i18n'
import { useTool } from './composables/useTool'
import MapComp from '@/components/feature/map/mapComp.vue'
import DynamicCubeServicePanel from '../thematic/DynamicCubeServicePanel.vue'
import { useCube } from './composables/useCube'
import { useMethLib } from './composables/useMethLib'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import InvokeHistory from './invokeHistory.vue'
import { currentPanel, setCurrentPanel } from './shared'
import { message } from 'ant-design-vue'
import { useToolRegistryStore } from '@/store'
import type { DynamicToolMeta } from '@/store/toolRegistry'
import { RegionSelects } from 'v-region'

const { t } = useI18n()
const isPicking = ref(false)

/**
 * 显示与折叠控制
 */
const showPanel = ref(false) // 左模块
const isToolbarOpen = ref(true)
const isSceneAnalysisExpand = ref(true) // 控制景级分析折叠

/**
 * 景级分析 - 通用分析工具（方法库）钩子
 */
const {
    // ------------------- 显示相关 -----------------------
    isMethLibExpand, showModal, openModal, selectedItem,
    // ------------------- 检索相关 -----------------------
    searchQuery, getMethLibList,
    // ------------------- 标签相关 -----------------------
    tagList, getTagList, selectTags,
    // ------------------- 列表相关 -----------------------
    currentPage: currentMethLibPage, pageSize: methLibPageSize, total: methLibTotal, methLibList,
    // ------------------- 调用相关 -----------------------
    handleInvoke,
    // ------------------- Tour 相关 -----------------------
    ref1, ref2, ref3, current, steps, handleOpenTour, openTour
} = useMethLib()

/**
 * 景级分析 - UDF分析工具 - 前序无云一版图数据
 */
// 前序无云一版图数据
const historyComponent = ref(null)
const showHistory = ref(false)
const {
    caseList,
    currentPage,
    pageSize,
    total,
    getCaseList,
    activeTab,
    handleSelectTab,
    getAndShowResult,
    onResultSelected
} = useViewHistoryModule();
const completedCases = ref<any[]>([]); // 仅存储已完成的任务
const loadCompletedCases = async () => {
    activeTab.value = 'COMPLETE';
    await getCaseList();
    completedCases.value = caseList.value;
};

/**
 * Cube数据分析 - 获取数据与可视化钩子
 */
const isCubeExpand = ref(true)
const {
    // ------------------- 可视化相关 -----------------------
    updateGridLayer,
    // ------------------- 列表相关 -----------------------
    cubeObj, cubeList,
    // ------------------- 操作相关 -----------------------
    inputCacheKey, handleSelectCube, currentCacheKey, getCubeObj
} = useCube()
const selectedCubeItems = computed(() => cubeList.value.filter((item) => item.isSelect))

/**
 * 设置section
 */
const {
    isSettingExpand,
    region,
    thematicConfig,
    originImages,
    exploreData,
    selectedResult,
    displayLabel,
    getOriginImages,
    getPlatformDataFile,
    selectedSensorName } = useSettings()

/**
 * 原来的指数分析等工具（DEPRECATED）
 */
const isDefaultToolsExpand = ref(false)
// 自定义工具
const { builtinToolCategories, expandedCategories, allToolCategories, selectedTask, isToolsExpand, currentTaskComponent,
    currentTaskProps,
    handleResultLoaded,
    handleRemoveDynamicTool,
    toggleCategory,
    dynamicToolCategories } = useTool()

const toolRegistryStore = useToolRegistryStore()
const isSceneToolMeta = (tool: DynamicToolMeta) => (tool?.level ?? 'scene') === 'scene'
const sortPublishedTools = (tools: DynamicToolMeta[]) => {
    return [...tools].sort((a, b) => {
        const aTime = new Date(a.updatedAt ?? a.createdAt ?? 0).getTime()
        const bTime = new Date(b.updatedAt ?? b.createdAt ?? 0).getTime()
        return bTime - aTime
    })
}
const publishedSceneTools = computed(() => {
    const sceneTools = toolRegistryStore.tools.filter((tool) => isSceneToolMeta(tool))
    return sortPublishedTools(sceneTools)
})
const publishedCubeTools = computed(() => {
    const cubeTools = toolRegistryStore.tools.filter((tool) => !isSceneToolMeta(tool))
    return sortPublishedTools(cubeTools)
})
const activeCubeToolId = ref<string | null>(null)
const activeCubeToolMeta = computed(() => {
    if (!activeCubeToolId.value) return null
    const meta = toolRegistryStore.getToolById(activeCubeToolId.value)
    if (!meta || isSceneToolMeta(meta)) return null
    return meta
})

const handleInvokeSceneTool = (toolId: string) => {
    if (!toolId) return
    selectedTask.value = `dynamic:${toolId}`
    showPanel.value = true
    activeCubeToolId.value = null
}

const handleInvokeCubeTool = (toolId: string) => {
    if (!toolId) return
    const toolMeta = toolRegistryStore.getToolById(toolId)
    if (!toolMeta) {
        message.error('未找到该 Cube 工具，请刷新列表')
        return
    }
    if (isSceneToolMeta(toolMeta)) {
        message.warning('该工具为景级工具，请从景级分析区域调用')
        return
    }
    activeCubeToolId.value = toolId
    showPanel.value = true
}

watch(
    () => toolRegistryStore.tools.map((tool) => tool.id),
    () => {
        if (activeCubeToolId.value && !toolRegistryStore.getToolById(activeCubeToolId.value)) {
            activeCubeToolId.value = null
        }
    },
)

/**
 * 通用方法
 */
const clearImages = () => {
    MapOperation.map_destroyTerrain()
    MapOperation.map_destroyRGBImageTileLayer()
    MapOperation.map_destroyOneBandColorLayer()
    mapManager.withMap((map) => {
        if (map.getLayer('UniqueSceneLayer-fill')) map.removeLayer('UniqueSceneLayer-fill')
        if (map.getLayer('UniqueSceneLayer-line')) map.removeLayer('UniqueSceneLayer-line')
        if (map.getSource('UniqueSceneLayer-source')) map.removeSource('UniqueSceneLayer-source')
    })
}

watch(displayLabel, getOriginImages, { immediate: true })

// const addLocalInternalLayer = () => {
//     mapManager.withMap((map) => {
//         const sourceId = 'Local-Interal-Source'
//         const layerId = 'Local-Interal-Layer'

//         // 防止重复添加
//         if (map.getLayer(layerId)) {
//             map.removeLayer(layerId)
//         }
//         if (map.getSource(sourceId)) {
//             map.removeSource(sourceId)
//         }

//         // 添加 source
//         map.addSource(sourceId, {
//             type: 'raster',
//             tiles: [
//                 `http://${window.location.host}${ezStore.get('conf')['fk_url']}`
//             ],
//             tileSize: 256,
//         })

//         // 添加 layer
//         map.addLayer({
//             id: layerId,
//             type: 'raster',
//             source: sourceId,
//         })
//     })
// }

onMounted(async () => {
    // 前序无云一版图数据加载
    onResultSelected.value = (result) => {
        selectedResult.value = result
        // 立即更新 thematicConfig
        thematicConfig.value = {
            ...thematicConfig.value,
            dataset: result.data  // 注意这里用 result.data
        }
        // 关闭弹窗
        showHistory.value = false
        message.success('已选择数据集')
    }
    loadCompletedCases();

    // Cube前序数据加载
    await getCubeObj()

    // 渲染立方体所有格网（未勾选的状态）
    updateGridLayer(cubeList.value)

    // 方法库数据加载
    await getTagList()
    await getMethLibList()

    // 未知？
    await getPlatformDataFile(exploreData.sensors?.[0].sensorName)
})

</script>

<style scoped src="../tabStyle.css">
html,
body,
#app {
    margin: 0;
    padding: 0;
    height: 100vh;
    /* 确保根元素占满视口 */
    overflow: hidden;
    /* 防止滚动条导致异常 */
    overscroll-behavior-y: none;
}

.chart-wrapper {
    margin-top: 10px;
}

.chart {
    width: 100%;
    height: 400px !important;
    border: 1px solid #ddd;
    border-radius: 6px;
}

:fullscreen .chart {
    width: 100vw !important;
    height: 100vh !important;
}

button {
    margin-top: 5px;
}
</style>
