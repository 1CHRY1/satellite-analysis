<template>
    <div class="cuz-card">
        <div class="cuz-card-header">
            <div class="cuz-card-header-icon">
                <slot name="icon"></slot>
            </div>
            <h2 class="cuz-card-header-title">
                <slot name="title"></slot>
            </h2>
        </div>
        <div class="cuz-card-content">
            <slot name="content"></slot>
        </div>
    </div>
</template>

<script setup lang="ts"></script>

<style scoped>
@reference 'tailwindcss';

.cuz-card {
    @apply mb-2 overflow-hidden rounded-md border border-sky-500 bg-white transition-all duration-300 ease-in-out;
}

.cuz-card:hover {
    @apply border-sky-800 shadow-md shadow-sky-100;
}

.cuz-card-header {
    @apply flex items-center justify-between border-b-2 border-solid border-sky-200 bg-gradient-to-r from-sky-50 to-white px-4 py-1;
}

.cuz-card-header-icon {
    @apply mr-3 flex h-6 w-6 items-center justify-center rounded-sm bg-sky-50 text-sky-600;
}

.cuz-card-header-title {
    @apply text-lg font-bold text-sky-950;
}

.cuz-card-content {
    @apply px-4 py-3;
}
</style>
