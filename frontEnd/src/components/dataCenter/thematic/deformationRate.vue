<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">形变速率</h2>
        </div>
        <div class="section-content">
            <!-- 自定义输入组件结构（如地图选点、影像选择等） -->
            ...
        </div>
    </section>

    <!-- Section: 结果展示 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <ChartColumn :size="18" />
            </div>
            <h2 class="section-title">计算结果</h2>
        </div>
        <div class="section-content">
            <!-- ECharts 渲染区 / 数据列表等 -->
            ...
        </div>
    </section>
</template>

<script setup>
import {
    ChartColumn,
    Earth,
    MapPinIcon,
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
} from 'lucide-vue-next'
</script>

<style scoped src="../tabStyle.css"></style>
