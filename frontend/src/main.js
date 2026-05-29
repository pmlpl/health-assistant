import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import messageService from './services/messageService'
import ECharts from 'vue-echarts'
import { use } from 'echarts/core'
import { notifyError } from './utils/notify'

import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

import './assets/styles/global.css'
import './assets/styles/bilibili-theme.css'

use([
    CanvasRenderer,
    BarChart,
    LineChart,
    PieChart,
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent
])

const app = createApp(App)
const pinia = createPinia()

app.component('v-chart', ECharts)
app.config.globalProperties.$message = messageService
app.provide('$message', messageService)

// 全局 Vue 错误处理：捕获未处理的组件错误
app.config.errorHandler = (err, instance, info) => {
    console.error('Vue 错误:', err, info)
    notifyError('页面出现异常，请刷新后重试', '系统错误')
}

app.use(ElementPlus, { locale: zhCn })
app.use(pinia)
app.use(router)
app.mount('#app')
