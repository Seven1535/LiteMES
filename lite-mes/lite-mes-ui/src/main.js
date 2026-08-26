import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import StatusTag from '@/components/StatusTag.vue'
import Pagination from '@/components/Pagination.vue'
import ConfirmButton from '@/components/ConfirmButton.vue'
import '@/assets/styles/global.scss'

const app = createApp(App)

// 全局注册 Element Plus 图标（菜单/导航按路由 meta.icon 动态渲染）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局注册公共组件（页面统一使用，保持风格一致）
app.component('StatusTag', StatusTag)
app.component('Pagination', Pagination)
app.component('ConfirmButton', ConfirmButton)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
