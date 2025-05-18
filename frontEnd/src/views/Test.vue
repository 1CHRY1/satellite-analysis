<template>
    <div>
        
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, type Ref } from 'vue'
import PopupContent, { type GridData } from '@/components/feature/map/popoverContent.vue'



function test(position) {
    console.log(position)
}


const mk = ref({
    rowId: 0,
    columnId: 0,
    resolution: 0,
    scenes: [
        {
            bucket: '',
            cloudPath: '',
            sceneId: '',
            sceneTime: '',
            sensorName: 'GF-1',
            productName: 'GF-1',
            resolution: '10m',
            images: [
                {
                    'band': '1',
                    'bucket': '1',
                    'tifPath': '2'
                },
                {
                    'band': '2',
                    'bucket': '1',
                    'tifPath': '2'
                },
                {
                    'band': '3',
                    'bucket': '1',
                    'tifPath': '2'
                }
            ]
        },
        {
            bucket: '',
            cloudPath: '',
            sceneId: '',
            sceneTime: '',
            sensorName: 'GF-2',
            productName: 'GF-2',
            resolution: '120m',
            images: [
                {
                    'band': '1',
                    'bucket': '1',
                    'tifPath': '2'
                },
                {
                    'band': '2',
                    'bucket': '1',
                    'tifPath': '2'
                },
                {
                    'band': '3',
                    'bucket': '1',
                    'tifPath': '2'
                }
            ]
        },
        {
            bucket: '',
            cloudPath: '',
            sceneId: '',
            sceneTime: '',
            sensorName: 'GF-1',
            productName: 'GF-1',
            resolution: '10m',
            images: [
                {
                    'band': '1',
                    'bucket': '1',
                    'tifPath': '2'
                },
                {
                    'band': '2',
                    'bucket': '1',
                    'tifPath': '2'
                },
                {
                    'band': '3',
                    'bucket': '1',
                    'tifPath': '2'
                }
            ]
        }
    ]
})
const a = ref(1)

onMounted(() => {
    console.log(mk, a)
})


</script>

<style scoped></style>