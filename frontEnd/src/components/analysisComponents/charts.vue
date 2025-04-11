<template>
    <div class="flex gap-4 overflow-x-auto p-4">
        <div v-for="(chart, index) in charts" :key="index"
            class="relative flex-shrink-0 w-[300px] h-[300px] border border-gray-300 bg-white rounded-xl resize overflow-hidden mt-8">
            <VChart class="w-full h-full" :option="chart.option" autoresize />
            <button @click="removeChart(index)"
                class="absolute top-2 right-2 bg-red-500 hover:bg-red-600 !text-white w-6 h-6 rounded-full flex items-center justify-center cursor-pointer">
                ×
            </button>
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
    PieChart,
} from 'echarts/charts';
import {
    TitleComponent,
    TooltipComponent,
    GridComponent,
    LegendComponent,
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
    CanvasRenderer,
]);

interface ChartData {
    labels: string[];
    values: number[];
}

const charts = ref<{ option: any }[]>([]);

const getChartOption = (type: 'bar' | 'line' | 'pie', data: ChartData) => {
    if (type === 'bar') {
        return {
            title: { text: 'Bar Chart' },
            tooltip: {},
            xAxis: { data: data.labels },
            yAxis: {},
            series: [{ type: 'bar', data: data.values }],
        };
    }

    if (type === 'line') {
        return {
            title: { text: 'Line Chart' },
            tooltip: {},
            xAxis: { data: data.labels },
            yAxis: {},
            series: [{ type: 'line', data: data.values }],
        };
    }

    if (type === 'pie') {
        return {
            title: { text: 'Pie Chart' },
            tooltip: { trigger: 'item' },
            series: [
                {
                    type: 'pie',
                    radius: '50%',
                    data: data.labels.map((label, i) => ({
                        name: label,
                        value: data.values[i],
                    })),
                },
            ],
        };
    }

    return {};
};

const addChart = (type: 'bar' | 'line' | 'pie', data: ChartData) => {
    const option = getChartOption(type, data);
    charts.value.push({ option });
};

const removeChart = (index: number) => {
    charts.value.splice(index, 1);
};

// 示例添加两个图表（可移除）
// addChart('bar', {
//     labels: ['A', 'B', 'C', 'D'],
//     values: [10, 20, 30, 40],
// });

// addChart('pie', {
//     labels: ['Apples', 'Bananas', 'Cherries'],
//     values: [5, 15, 25],
// });
defineExpose({ addChart, removeChart });

</script>
