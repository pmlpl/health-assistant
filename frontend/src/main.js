import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import messageService from './services/messageService'
import ECharts from 'vue-echarts'
import { use } from "echarts/core"

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 手动导入ECharts模块
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

// 引入全局样式
import './assets/styles/global.css'
// 引入B站主题样式
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
]);

const app = createApp(App)
const pinia = createPinia()

// 注册 v-chart 全局组件
app.component('v-chart', ECharts)

// 注册全局消息服务
app.config.globalProperties.$message = messageService

// 也可以通过provide/inject方式提供
app.provide('$message', messageService)

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')