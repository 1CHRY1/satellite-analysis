<template>
    <a-range-picker class="custom-date-picker" v-model:value="dateRangeValue" picker="day" :placeholder="['开始日期', '结束日期']" />
</template>

<script setup lang="ts">
import dayjs, { type Dayjs } from 'dayjs'
type RangeValue = [Dayjs, Dayjs]
const dateRangeValue = defineModel<RangeValue>('value', { default: [dayjs(), dayjs()] })
</script>

<style scoped>
.custom-date-picker {
    width: 100%;
}

.custom-date-picker :deep(.ant-picker) {
    border-color: #7dd3fc;
}

.custom-date-picker:hover :deep(.ant-picker) {
    border-color: #0284c7;
}
</style>
