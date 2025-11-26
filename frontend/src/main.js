import '@/assets/styles/main.css'
import vuetify from './plugins/vuetify'
import { createApp } from 'vue'
import App from '@/App.vue'

// 导入路由
import router from '@/router'

// 引入全局状态管理 Pinia
import pinia from '@/stores'

// 导入错误处理工具

const app = createApp(App)

// 应用路由
app.use(router)
// 应用 Pinia
app.use(pinia)

// 应用组件库
app.use(vuetify)

app.mount('#app')
