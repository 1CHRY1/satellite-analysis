<template>
    <div>
        <PopupContent class="absolute" :gridData="mk"  v-draggable="configuration" ></PopupContent>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, type Ref } from 'vue'
import PopupContent, { type GridData } from '@/components/feature/map/popoverContent.vue'

import useDraggable from 'vue-draggable-anywhere'

const { vDraggable, position } = useDraggable()
const configuration = {
    boundaryElement: '.parentClass',
    boundary: true,
    boundaryOffset: {
        x: 30,
        y: 100
    },
    onDragging: test,
    afterDragEnd: test
}

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
            sensorName: '',
            productName: '',
            resolution: '',
            images: [
                {
                    'band': '1',
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