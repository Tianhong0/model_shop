import { defineStore } from 'pinia'

// 从 localStorage 加载持久化的配置
const loadPersistedConfig = () => {
  try {
    const saved = localStorage.getItem('admin-config')
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.warn('加载配置失败:', e)
  }
  return {}
}

const defaultConfig = {
  siteName: '3D打印定制商城后台管理系统',
  siteStatus: true,
  themeColor: '#4f46e5',
  sidebarDark: true,
  siteIcon: 'Cpu', // Element Plus 图标名称
  siteIconUrl: '', // 自定义图标 URL
  miniBanners: [],
  miniNotices: ['新品上线，欢迎选购']
}

const persistedConfig = loadPersistedConfig()

export const useConfigStore = defineStore('config', {
  state: () => ({
    ...defaultConfig,
    ...persistedConfig
  }),
  actions: {
    updateConfig(newConfig) {
      Object.assign(this.$state, newConfig)
      this.saveToStorage()
    },
    saveToStorage() {
      try {
        const dataToSave = {
          siteName: this.siteName,
          themeColor: this.themeColor,
          sidebarDark: this.sidebarDark,
          siteIcon: this.siteIcon,
          siteIconUrl: this.siteIconUrl
        }
        localStorage.setItem('admin-config', JSON.stringify(dataToSave))
      } catch (e) {
        console.warn('保存配置失败:', e)
      }
    },
    applyTheme() {
      // 应用主题颜色到 CSS 变量
      const root = document.documentElement
      root.style.setProperty('--primary-color', this.themeColor)

      // 计算衍生颜色
      const hex = this.themeColor
      if (hex && hex.startsWith('#') && hex.length === 7) {
        const r = parseInt(hex.slice(1, 3), 16)
        const g = parseInt(hex.slice(3, 5), 16)
        const b = parseInt(hex.slice(5, 7), 16)

        // 浅色版本
        root.style.setProperty('--primary-light', `rgba(${r}, ${g}, ${b}, 0.8)`)
        root.style.setProperty('--primary-lighter', `rgba(${r}, ${g}, ${b}, 0.15)`)

        // 深色版本
        const darken = (val, amount) => Math.max(0, val - amount)
        root.style.setProperty('--primary-dark', `rgb(${darken(r, 30)}, ${darken(g, 30)}, ${darken(b, 30)})`)
      }

      // 保存到 localStorage
      this.saveToStorage()
    },
    $reset() {
      Object.assign(this.$state, defaultConfig)
      this.saveToStorage()
    }
  }
})
