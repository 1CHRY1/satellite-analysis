import { createApp } from 'vue'
import { createPinia } from 'pinia'
import mapboxgl from 'mapbox-gl'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import './style.css'
import App from './App.vue'
import router from './router'
import ezStore from '@/util/ezStore'

fetch('/app.conf.json').then((response) => {
    response.json().then((config) => {
        console.log(config)
        mapboxgl.accessToken = 'pk.eyJ1IjoieWNzb2t1IiwiYSI6ImNrenozdWdodDAza3EzY3BtdHh4cm5pangifQ.ZigfygDi2bK4HXY1pWh-wg'
        const app = createApp(App)
        const pinia = createPinia()
        app.use(Antd)
        app.use(router)
        app.use(pinia)
        app.mount('#app')
        ezStore.set('conf', config) // Inject config
    })
})
