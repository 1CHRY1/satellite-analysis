<template>
    <div
        class="bg-black bg-opacity-70 p-4 rounded-lg shadow-md hover:bg-opacity-90 transition duration-300 cursor-pointer">
        <div class="flex items-center justify-between mb-2">
            <h3 class="text-xl font-bold text-green-400">{{ project.name }}</h3>
            <button class="bg-green-500 text-white px-2 py-1 rounded">Edit</button>
        </div>
        <p class="text-sm text-gray-400 mb-2">{{ project.description }}</p>
        <div class="flex items-center space-x-2 mb-2">
            <span class="text-xs text-gray-400">About:</span>
            <span class="text-xs text-green-400">{{ project.about }}</span>
        </div>
        <div class="flex items-center space-x-2 mb-2">
            <span class="text-xs text-gray-400">Contact:</span>
            <a :href="`mailto:${project.contact}`" class="text-xs text-green-400">{{ project.contact }}</a>
        </div>
        <div class="flex items-center space-x-2">
            <span class="text-xs text-gray-400">Created:</span>
            <span class="text-xs text-green-400">{{ project.created }}</span>
        </div>
    </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
    props: {
        project: {
            type: Object,
            required: true,
        },
    },
});
</script>