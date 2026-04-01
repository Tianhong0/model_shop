import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/variables.css'
import './styles/page-agent.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { setupDirectives } from './directives'
import { PageAgent } from 'page-agent'

// PageAgent 实例管理
// 注意：page-agent 的关闭按钮会调用 dispose() 销毁实例
// 所以每次打开时需要检查实例是否已被销毁
let pageAgentInstance = null

const createPageAgent = () => {
  // 如果实例存在且未被销毁，直接返回
  if (pageAgentInstance && !pageAgentInstance.disposed) {
    return pageAgentInstance
  }

  // 创建新实例
  pageAgentInstance = new PageAgent({
    model: 'glm-4.5-air',
    baseURL: 'https://open.bigmodel.cn/api/paas/v4',
    apiKey: '94a0955cfdb6470f8d694afcd20d86f1.oNNLQ7IED90Y17pH',
    language: 'zh-CN',
    customFetch: async (url, init) => {
      const body = JSON.parse(init.body)
      // Adapt parameters for your provider
      delete body.stream_options
      return fetch(url, { ...init, body: JSON.stringify(body) })
    },
  })

  // 默认隐藏面板
  pageAgentInstance.panel.hide()

  return pageAgentInstance
}

// 将 page-agent 实例挂载到 window 对象，方便全局访问
window.createPageAgent = createPageAgent
window.getPageAgent = () => pageAgentInstance

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册自定义指令
setupDirectives(app)

app.mount('#app')
