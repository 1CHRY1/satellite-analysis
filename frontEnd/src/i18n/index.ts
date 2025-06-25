import { createI18n } from 'vue-i18n'
import { type App } from 'vue' 
import en from './locales/en.js'
import zh from './locales/zh.js'

const i18n = createI18n({
    legacy : false,
    locale : localStorage.getItem('lang')||'zh',
    fallbackFormat : 'zh',
    messages: { 
    en,
    zh
  }
})

export const setupI18n = (app: App) => {
  app.use(i18n)
}

export default i18n