<template>
    <a-modal v-model:open="modalVisible" title="选择项目" :width="700" :footer="null" :destroyOnClose="true"
        class="dataset-modal">
        <div class="h-[350px] w-[620px] overflow-y-auto">
            <a-skeleton active :loading="projectList.length === 0">
                <div class="flex flex-row flex-wrap gap-x-8 gap-y-4 relative justify-start">
                    <ProjectCard v-for="project in projectList" :key="project.projectId" :project="project"
                        :checked="selectedProject?.projectId === project.projectId"
                        @select="handleSelect" />
                </div>
            </a-skeleton>
        </div>
        <div class="flex justify-end">
            <a-button type="primary" @click="handleConfirm">确定</a-button>
        </div>
    </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, defineModel } from 'vue'
import ProjectCard from './smallProjectCard.vue'
import { getProjects } from '@/api/http/analysis'
import type { Project } from './type'

const modalVisible = defineModel<Boolean>({ required: true })
const projectList = ref<Project[]>([])
const selectedProject = ref<Project | null>(null)

const handleSelect = (project: Project) => {
    if (selectedProject.value && selectedProject.value.projectId === project.projectId) {
        selectedProject.value = null
    } else {
        selectedProject.value = project
    }
}

const emit = defineEmits(['select_project'])
const handleConfirm = () => {
    emit('select_project', selectedProject.value)
    modalVisible.value = false
}

onMounted(async () => {
    projectList.value = await getProjects()
})

</script>