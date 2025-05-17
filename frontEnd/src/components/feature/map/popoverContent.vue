<template>
    <div class="popup-content">
        <div class="grid-id">
            <p>Grid: {{ gridID }}</p>
        </div>

        <div class="band-selection">
            <label for="band-select">传感器:</label>
            <select id="band-select" v-model="selectedSensor" class="band-select">
                <option disabled value="">请选择</option>
                <option v-for="sensor in sensors" :key="sensor" :value="sensor">
                    {{ sensor }}
                </option>
            </select>
        </div>

        <div class="band-selection">
            <label for="band-select">波段:</label>
            <select id="band-select" v-model="selectedBand" class="band-select">
                <option disabled value="">请选择</option>
                <option v-for="band in bands" :key="band" :value="band">
                    {{ band }}
                </option>
            </select>
        </div>

        <div class="btns">
            <button class="visualize-btn" @click="handleVisualize" :disabled="!selectedBand">
                <span class="btn-icon">
                    <GalleryHorizontalIcon :size="18" />
                </span>
                网格影像可视化
            </button>
            <button class="delete-btn" @click="handleRemove">
                <span class="btn-icon">
                    <Trash2Icon :size="18" />
                </span>
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref } from 'vue';
import { GalleryHorizontalIcon, Trash2Icon } from 'lucide-vue-next'
import bus from '@/store/bus';
import { map_removeGridPreviewLayer } from '@/util/map/operation'
type Image = {
    bucket: string
    tifPath: string
    band: string
}

type Scene = {
    bucket: string
    cloudPath: string
    sceneId: string
    sceneTime: string
    sensorName: string
    productName: string
    images: Image[]
}

export type GridData = {
    rowId: number
    columnId: number
    resolution: number
    scenes: Scene[]
}

type ImageInfoType = {
    sceneId: string
    time: string
    tifFullPath: string
}

type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number
}

const props = defineProps<{
    gridData: Ref<GridData>;
}>()


const gridID = computed(() => {
    const { rowId, columnId, resolution } = props.gridData.value;
    return `${rowId}-${columnId}-${resolution}`;
})

const sensors = computed(() => {
    let result: string[] = []

    props.gridData.value.scenes.forEach((scene: Scene) => {
        if (!result.includes(scene.sensorName)) {
            result.push(scene.sensorName)
        }
    })

    return result
})

const bands = computed(() => {

    let result: string[] = []

    if (selectedSensor.value != '') {
        props.gridData.value.scenes.forEach((scene: Scene) => {
            if (scene.sensorName === selectedSensor.value) {
                scene.images.forEach((bandImg: Image) => {
                    if (!result.includes(bandImg.band)) {
                        result.push(bandImg.band)
                    }
                })
            }
        })
    }
    result.sort((a, b) => {
        return a.localeCompare(b)
    })

    return result
})



const selectedBand = ref('')
const selectedSensor = ref('')


// Handle visualization button click
const handleVisualize = () => {

    const { rowId, columnId, resolution } = props.gridData.value;
    const gridData: GridInfoType = {
        rowId,
        columnId,
        resolution
    }

    const imageData: ImageInfoType[] = []
    for (let scene of props.gridData.value.scenes) {

        if (scene.sensorName == selectedSensor.value) {

            scene.images.forEach((bandImg: Image) => {

                if (bandImg.band === selectedBand.value) {
                    imageData.push({
                        tifFullPath: bandImg.bucket + '/' + bandImg.tifPath,
                        sceneId: scene.sceneId,
                        time: scene.sceneTime,
                    })
                }
            })

        }

    }

    bus.emit('cubeVisualize', imageData, gridData)
    bus.emit('openTimeline')
}

const handleRemove = () => {
    const { rowId, columnId, resolution } = props.gridData.value;
    const prefix = rowId + '' + columnId
    map_removeGridPreviewLayer(prefix)
}

onMounted(() => {
    bus.on('closeTimeline', () => {
        selectedBand.value = ''
        selectedSensor.value = ''
    })
})

</script>

<style scoped>
.popup-content {
    background-color: #0a1929;
    color: #e6f1ff;
    padding: 0.75rem;
    border-radius: 0.5rem;
    width: 100%;
    max-width: 200px;
    user-select: none;
}

.grid-id {
    margin-bottom: 1rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid #1e3a5f;
    text-align: center;
}

.grid-id p {
    font-size: 1.125rem;
    font-weight: 600;
    margin: 0;
    color: #7eb3dd;
}

.band-selection {
    display: grid;
    grid-template-columns: 40% 60%;
    margin-bottom: 1rem;
}

.band-selection label {
    display: inline-block;
    margin-right: 1 rem;
    height: 2.75rem;
    line-height: 2.75rem;
    font-size: 1rem;
    color: #a5d8ff;
}


.band-select {

    width: 100%;
    padding: 0.625rem;
    background-color: #132f4c;
    color: #e6f1ff;
    border: 1px solid #1e3a5f;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%234dabf7' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 0.625rem center;
    padding-right: 2rem;
}

.band-select:focus {
    outline: none;
    border-color: #4dabf7;
    box-shadow: 0 0 0 2px rgba(77, 171, 247, 0.25);
}

.btns {
    display: flex;
}

.visualize-btn {
    width: 80%;
    padding: 0.75rem 0.5rem;
    padding-right: 0;
    background-color: #0c4a6e;
    color: #e6f1ff;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    transition: background-color 0.2s ease;
}

.visualize-btn:hover:not(:disabled) {
    background-color: #075985;
}

.visualize-btn:disabled,
.delete-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.delete-btn {
    width: 20%;
    color: #e6f1ff;
    /* padding: 0.75rem 1rem; */
    padding-left: 0.75rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease;
}

.btn-icon {
    margin-right: 0.5rem;
    display: flex;
    align-items: center;
}

@media (max-width: 640px) {
    .popup-content {
        padding: 1rem;
    }
}
</style>