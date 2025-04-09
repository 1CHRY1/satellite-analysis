<template>
    <div class="charts-container">
        <div v-for="(chart, index) in charts" :key="index" class="chart-item">
            <v-chart :option="chart.option" autoresize class="chart" />
            <button class="delete-btn" @click="removeChart(index)">×</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import VChart from 'vue-echarts';
import * as echarts from 'echarts/core';
import {
    BarChart,
    LineChart,
    PieChart
} from 'echarts/charts';
import {
    TitleComponent,
    TooltipComponent,
    GridComponent,
    LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
    TitleComponent,
    TooltipComponent,
    GridComponent,
    LegendComponent,
    BarChart,
    LineChart,
    PieChart,
    CanvasRenderer
]);

const charts = ref<{ option: any }[]>([]);

const addChart = (type: 'bar' | 'line' | 'pie', data: any) => {
    const option = getChartOption(type, data);
    charts.value.push({ option });
};

const removeChart = (index: number) => {
    charts.value.splice(index, 1);
};

const getChartOption = (type: string, data: any) => {
    switch (type) {
        case 'bar':
            return {
                title: { text: 'Bar Chart' },
                tooltip: {},
                xAxis: { data: data.labels },
                yAxis: {},
                series: [{ type: 'bar', data: data.values }]
            };
        case 'line':
            return {
                title: { text: 'Line Chart' },
                tooltip: {},
                xAxis: { data: data.labels },
                yAxis: {},
                series: [{ type: 'line', data: data.values }]
            };
        case 'pie':
            return {
                title: { text: 'Pie Chart' },
                tooltip: { trigger: 'item' },
                series: [
                    {
                        type: 'pie',
                        radius: '50%',
                        data: data.labels.map((label: string, i: number) => ({
                            name: label,
                            value: data.values[i]
                        }))
                    }
                ]
            };
        default:
            return {};
    }
};

// 示例：创建几个图表（实际可暴露给父组件来动态添加）
addChart('bar', {
    labels: ['A', 'B', 'C', 'D'],
    values: [10, 20, 30, 40]
});
addChart('line', {
    labels: ['Jan', 'Feb', 'Mar'],
    values: [15, 25, 35]
});
</script>

<style scoped lang="scss">
.charts-container {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    gap: 12px;
    padding: 10px;
}

.chart-item {
    position: relative;
    flex: 0 0 auto;
    width: 300px;
    height: 300px;
    resize: both;
    overflow: hidden;
    border: 1px solid #ccc;
    border-radius: 6px;
    background: #fff;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.chart {
    width: 100%;
    height: 100%;
}

.delete-btn {
    position: absolute;
    top: 4px;
    right: 6px;
    background: #f56c6c;
    color: white;
    border: none;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    font-size: 16px;
    line-height: 20px;
    cursor: pointer;
}
</style>