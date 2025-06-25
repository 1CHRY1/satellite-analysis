<template>
    <!-- 下拉语言菜单 -->
    <div class="relative" ref="dropdownContainer">
        <button 
        @click.stop="ToggleDropdown"
        class="flex items-center rounded-md space-x-1"
        >
            <span class="text-md  px-1 py-1 left-[-8px] text-white-300  transition-colors hover:bg-gray-700 hover:font-bold hover:text-sky-200"> 
                {{ CurrentLanguage }} 
            </span>
        </button>

        <div v-show="ShowDropdown "
        class="absolute right-0 mt-2 bg-gray-800 shadow-lg ring-1 ring-gray-700 "
        >
            <div class="py-1">
                <button v-for="locale in availableLocales"
                :key="locale" 
                @click="ChangeLanguage(locale)"
                class="w-full px-4 py-2 text-left text-white hover:bg-gray-700 flex items-center justify-between whitespace-nowrap"
                >
                    <span>{{ GetLanguageName(locale)}}</span>
                    <CheckIcon v-if="locale === CurrentLocale"></CheckIcon>
                </button>
            </div>

        </div>
    </div>
</template>

<script setup lang="ts">

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { CheckIcon } from 'lucide-vue-next';
import { useI18n } from 'vue-i18n'
import type { Locale } from 'vue-i18n';



const dropdownContainer = ref<HTMLElement | null>(null)

//  点击外部关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  if (dropdownContainer.value && !dropdownContainer.value.contains(event.target as Node)) {
    ShowDropdown.value = false
  }
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const {t, locale, availableLocales } = useI18n()

const ShowDropdown = ref(false)

const CurrentLocale = computed(() => locale.value as Locale)
const CurrentLanguage = computed(()=> GetLanguageName(CurrentLocale.value))

const LanguageName: Record<Locale,string> = {
  zh: '中文',
  en: 'English',
}

const ToggleDropdown = () => {
  ShowDropdown.value = !ShowDropdown.value
}

const CloseDropdown = () => {
  ShowDropdown.value = false
}

const GetLanguageName = (code : Locale) => LanguageName[code]||code

// 本地偏好储存
const ChangeLanguage = (newLocale: Locale) => {
  locale.value = newLocale  
  localStorage.setItem('lang', newLocale)  
  CloseDropdown()
}


</script>