import { createApp } from 'vue'
import { createPinia } from 'pinia'
import mapboxgl from 'mapbox-gl'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

import './style.css'
import App from './App.vue'
import router from './router'
import ezStore from '@/store/ezStore'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

///// Fetching Static Config Json /////////////////////////
fetch('/app.conf.json').then((response) => {
    response.json().then((config) => {
        //// 3rd Party Config
        dayjs.locale('zh-cn')
        mapboxgl.accessToken =
            'pk.eyJ1IjoieWNzb2t1IiwiYSI6ImNrenozdWdodDAza3EzY3BtdHh4cm5pangifQ.ZigfygDi2bK4HXY1pWh-wg'

        //// Vue Application
        const app = createApp(App)
        const pinia = createPinia()
        app.use(Antd)
        app.use(router)
        app.use(pinia)
        app.use(ElementPlus)
        app.mount('#app')

        ////  Inject config
        ezStore.set('conf', config)
    })
})
