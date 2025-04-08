<template>
    <!-- Dataset selection modal -->
    <a-modal v-model:open="modalVisible" title="选择数据集" :width="800" :footer="null" :destroyOnClose="true"
        class="dataset-modal">
        <div class="modal-content">
            <a-skeleton active :loading="dataFetching">
                <!-- Step indicator -->
                <div class="steps-container">
                    <a-steps :current="currentUIStep" size="small">
                        <a-step title="选择传感器" />
                        <a-step title="选择产品" />
                    </a-steps>
                </div>

                <!-- Satellite selection view -->
                <div v-if="currentUIStep === 0" class="satellites-container">
                    <div class="satellites-grid">
                        <div v-for="satellite in satellites" :key="satellite.id" class="satellite-card"
                            :class="{ selected: selectedSatellite?.id === satellite.id }"
                            @click="selectSatelliteHandler(satellite)">
                            <div class="satellite-icon">
                                <satellite-icon :size="24" :color="'#38bdf8'" />
                            </div>
                            <div class="satellite-info">
                                <h3 class="satellite-name">{{ satellite.name }}</h3>
                                <p class="satellite-description">{{ satellite.description }}</p>
                                <div class="satellite-meta">
                                    <span class="satellite-products-count">{{ satellite.products.length }} 个产品</span>
                                </div>
                            </div>
                            <div class="satellite-select-indicator">
                                <check-circle-icon v-if="selectedSatellite?.id === satellite.id" :size="20" />
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <a-button @click="closeModal">取消</a-button>
                        <a-button type="primary" @click="goToProductSelection" :disabled="!selectedSatellite">
                            下一步
                        </a-button>
                    </div>
                </div>

                <!-- Product selection view -->
                <div v-if="currentUIStep === 1 && selectedSatellite" class="products-container">
                    <div class="satellite-header">
                        <div class="satellite-badge">
                            <satellite-icon :size="16" />
                            <span>{{ selectedSatellite.name }}</span>
                        </div>
                        <a-button type="link" class="back-button" @click="backToSatellites">
                            <arrow-left-from-line-icon :size="14" />
                            <span>返回传感器选择</span>
                        </a-button>
                    </div>

                    <div class="products-list">
                        <a-radio-group v-model:value="selectedProductId" class="product-radio-group">
                            <div v-for="product in selectedSatellite.products" :key="product.id" class="product-item">
                                <a-radio :value="product.id">
                                    <div class="product-info">
                                        <h4 class="product-name">{{ product.name }}</h4>
                                        <p class="product-description">{{ product.description }}</p>
                                        <div class="product-meta">
                                            <span class="product-resolution">分辨率: {{ product.resolution }}</span>
                                            <span class="product-period">周期: {{ product.period }}</span>
                                        </div>
                                    </div>
                                </a-radio>
                            </div>
                        </a-radio-group>
                    </div>

                    <div class="modal-footer">
                        <a-button @click="backToSatellites">上一步</a-button>
                        <a-button type="primary" @click="confirmSelection" :disabled="!selectedProductId">
                            确认选择
                        </a-button>
                    </div>
                </div>
            </a-skeleton>
        </div>
    </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { SatelliteIcon, CheckCircleIcon, ArrowLeftFromLineIcon } from 'lucide-vue-next'
import { fetchAllSensorViews, type SensorView } from './apiAdapter/adapter'
import { message } from 'ant-design-vue'

/////// Modal Visible //////////////////////////////////
const modalVisible = defineModel<Boolean>({ required: true })
const closeModal = () => (modalVisible.value = false)

/////// UI Step //////////////////////////////////
const currentUIStep = ref<Number>(0)
const goToProductSelection = () => {
    currentUIStep.value = 1
}
const backToSatellites = () => {
    currentUIStep.value = 0
}

/////// Satellite and Products //////////////////////////////////
const emit = defineEmits(['update:selectedProductId'])
const satellites = ref<SensorView[]>([])
const selectedSatellite = ref<SensorView | null>()
const selectedProductId = ref<string>('')
const dataFetching = ref<Boolean>(false)
const fetchSatellitesData = async () => {
    dataFetching.value = true
    satellites.value = await fetchAllSensorViews()
    dataFetching.value = false
}

const selectSatelliteHandler = (satellite: any) => {
    selectedSatellite.value = satellite
}

const confirmSelection = () => {
    const pid = selectedProductId.value
    const productInfo = selectedSatellite.value?.products.find((p: any) => p.id === pid)
    emit('update:selectedProductId', productInfo)
    closeModal()
}

onMounted(() => {
    fetchSatellitesData().catch((_) => {
        message.error('遥感数据产品获取失败', _)
    })
})


onUnmounted(() => {
    // selectedSatellite.value = null
    // selectedProductId.value = null
})
</script>

<style scoped>
.dataset-selector {
    width: 100%;
}

.upload-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100px;
    border: 2px dashed #cbd5e1;
    border-radius: 0.5rem;
    cursor: pointer;
    transition: all 0.2s ease;
    background-color: #f8fafc;
    position: relative;
}

.upload-area:hover {
    border-color: #7dd3fc;
    background-color: #f0f9ff;
}

.upload-icon {
    color: #0284c7;
    margin-bottom: 0.5rem;
}

.upload-text {
    color: #334155;
    font-size: 0.9rem;
}

.selected-tags-preview {
    position: absolute;
    bottom: 8px;
    font-size: 0.8rem;
    color: #64748b;
}

.modal-content {
    display: flex;
    flex-direction: column;
    min-height: 550px;
}

.steps-container {
    margin-bottom: 24px;
}

.steps-container :deep(.ant-steps-item-title) {
    color: #e0f2fe !important;
}

/* waiting step */
.steps-container :deep(.ant-steps-item-icon) {
    background-color: rgba(255, 255, 255, 0.2);
    border-color: #38bdf8;
}

.steps-container :deep(.ant-steps-item-process .ant-steps-item-icon) {
    background-color: #00497e;
    box-shadow: 0 0 10px 0 rgba(56, 195, 255, 0.753) inset;
}

.steps-container :deep(.ant-steps-item-icon .ant-steps-icon) {
    color: #ffffff;
    font-weight: 500;
}

.satellites-container,
.products-container {
    display: flex;
    flex-direction: column;
    flex: 1;
}

.satellites-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
}

.satellite-card {
    display: flex;
    padding: 20px;
    border-radius: 12px;
    border: 1px solid rgba(56, 189, 248, 0.2);
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    background-color: rgba(15, 23, 42, 0.4);
    overflow: hidden;
}

.satellite-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, #38bdf8, transparent);
    opacity: 0;
    transition: opacity 0.3s ease;
}

.satellite-card:hover::before {
    opacity: 1;
}

.satellite-card:hover {
    border-color: #38bdf8;
    box-shadow: 0 4px 20px rgba(56, 189, 248, 0.15);
    transform: translateY(-2px);
}

.satellite-card.selected {
    border-color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.15);
    box-shadow: 0 0 10px 1px rgba(56, 195, 255, 0.3) inset;
}

.satellite-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 12px;
    margin-right: 20px;
    flex-shrink: 0;
    background: linear-gradient(135deg, rgba(56, 189, 248, 0.2), rgba(14, 165, 233, 0.1));
    border: 1px solid rgba(56, 189, 248, 0.3);
    transition: all 0.3s ease;
}

.satellite-card:hover .satellite-icon {
    transform: scale(1.05);
    background: linear-gradient(135deg, rgba(56, 189, 248, 0.3), rgba(14, 165, 233, 0.2));
}

.satellite-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.satellite-name {
    font-size: 1.1rem;
    font-weight: 600;
    margin: 0;
    color: #e0f2fe;
    display: flex;
    align-items: center;
    gap: 8px;
}

.satellite-name::after {
    content: '';
    display: block;
    width: 8px;
    height: 8px;
    background-color: #38bdf8;
    border-radius: 50%;
    opacity: 0.5;
}

.satellite-description {
    font-size: 0.9rem;
    color: #94a3b8;
    margin: 0;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.satellite-meta {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 4px;
}

.satellite-products-count {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 0.85rem;
    color: #38bdf8;
    background-color: rgba(56, 189, 248, 0.1);
    padding: 4px 8px;
    border-radius: 4px;
    border: 1px solid rgba(56, 189, 248, 0.2);
}

.satellite-select-indicator {
    position: absolute;
    top: 16px;
    right: 16px;
    color: #38bdf8;
    opacity: 0;
    transform: scale(0.8);
    transition: all 0.3s ease;
}

.satellite-card.selected .satellite-select-indicator {
    opacity: 1;
    transform: scale(1);
}

.satellite-card.selected .satellite-select-indicator::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 24px;
    height: 24px;
    background-color: rgba(56, 189, 248, 0.2);
    border-radius: 50%;
    z-index: -1;
}

/* 添加动画效果 */
@keyframes pulse {
    0% {
        box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.4);
    }

    70% {
        box-shadow: 0 0 0 10px rgba(56, 189, 248, 0);
    }

    100% {
        box-shadow: 0 0 0 0 rgba(56, 189, 248, 0);
    }
}

.satellite-card.selected {
    animation: pulse 2s infinite;
}

.satellite-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.satellite-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    border-radius: 16px;
    font-size: 0.9rem;
    font-weight: 500;
    color: #2d92be;
}

.back-button {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    color: #38bdf8;
}

.back-button :deep(svg) {
    display: block;
}

.products-list {
    margin-bottom: 24px;
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid rgba(56, 189, 248, 0.2);
    border-radius: 8px;
    background-color: rgba(15, 23, 42, 0.4);
}

.product-radio-group {
    width: 100%;
}

.product-item {
    padding: 16px;
    border-bottom: 1px solid rgba(56, 189, 248, 0.1);
    transition: all 0.3s ease;
    border-radius: 8px;
    margin-bottom: 8px;
}

.product-item:hover {
    background-color: rgba(56, 189, 248, 0.05);
}

.product-item:last-child {
    border-bottom: none;
    margin-bottom: 0;
}

.product-info {
    margin-left: 8px;
}

.product-name {
    font-size: 1rem;
    font-weight: 500;
    margin: 0 0 6px 0;
    color: #e0f2fe;
}

.product-description {
    font-size: 0.9rem;
    color: #94a3b8;
    margin: 0 0 10px 0;
    line-height: 1.5;
}

.product-meta {
    display: flex;
    gap: 20px;
    font-size: 0.85rem;
    color: #38bdf8;
}

.product-meta span {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    background-color: rgba(56, 189, 248, 0.1);
    border-radius: 4px;
    border: 1px solid rgba(56, 189, 248, 0.2);
}

:deep(.ant-radio-wrapper) {
    width: 100%;
    margin-right: 0;
}

:deep(.ant-radio) {
    top: 0;
}

:deep(.ant-radio-inner) {
    border-color: rgba(56, 189, 248, 0.3);
    background-color: rgba(15, 23, 42, 0.4);
}

:deep(.ant-radio-checked .ant-radio-inner) {
    border-color: #38bdf8;
    background-color: #38bdf8;
}

:deep(.ant-radio-wrapper:hover .ant-radio-inner) {
    border-color: #38bdf8;
}

:deep(.ant-radio-checked::after) {
    border-color: #38bdf8;
}

:deep(.ant-radio-disabled .ant-radio-inner) {
    background-color: rgba(15, 23, 42, 0.2);
    border-color: rgba(56, 189, 248, 0.1);
}

:deep(.ant-radio-disabled + span) {
    color: #64748b;
}

.modal-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 16px;
}

.modal-footer :deep(.ant-btn) {
    border-color: rgba(56, 189, 248, 0.3);
    color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.05);
}

.modal-footer :deep(.ant-btn:hover) {
    border-color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.15);
}

.modal-footer :deep(.ant-btn-primary) {
    background-color: rgba(56, 189, 248, 0.3);
    border-color: #38bdf8;
}

.modal-footer :deep(.ant-btn-primary:hover) {
    background-color: #0284c7;
    border-color: #0284c7;
}

.modal-footer :deep(.ant-btn[disabled]) {
    background-color: rgba(15, 23, 42, 0.4);
    border-color: rgba(56, 189, 248, 0.1);
    color: #64748b;
}
</style>
