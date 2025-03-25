<template>
    <div class=" w-full  bg-gray-800 rounded-lg overflow-hidden shadow-lg" style="height: 100%;">
        <!-- Header -->
        <!-- <div class="console-header flex justify-between items-center p-4 bg-gray-900 text-white">
            <span class="font-bold text-lg">控制台</span>
            <button class="clear-button px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded-md transition-colors"
                @click="clearConsole">
                Clear
            </button>
        </div> -->

        <!-- Content Area -->
        <!-- <div ref="consoleContent"
            class="console-content flex-1 p-4 overflow-y-auto text-sm text-gray-300 whitespace-pre-wrap">
            <p v-for="(message, index) in messages" :key="index">{{ message }}</p>
        </div> -->
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

// Messages to display in the console
const messages = ref<string[]>(['Welcome to the console!']);

// Reference to the content area for auto-scrolling
const consoleContent = ref<HTMLDivElement | null>(null);

// Function to add a new message to the console
const addMessage = (msg: string) => {
    messages.value.push(msg);
    scrollToBottom();
};

// Function to clear all messages
const clearConsole = () => {
    messages.value = [];
};

// Auto-scroll to the bottom of the console
const scrollToBottom = () => {
    if (consoleContent.value) {
        consoleContent.value.scrollTop = consoleContent.value.scrollHeight;
    }
};

// Simulate adding messages (for demonstration purposes)
// setInterval(() => {
//     addMessage(`Log: ${new Date().toLocaleTimeString()}`);
// }, 2000);
</script>

<style scoped lang="scss"></style>