import { defineStore } from 'pinia'

export const useConfigStore = defineStore('config', {
  state: () => ({
    siteName: '3D打印定制商城后台管理系统',
    siteStatus: true,
    themeColor: '#4f46e5',
    sidebarDark: true,
    miniBanners: [],
    miniNotices: ['新品上线，欢迎选购']
  }),
  actions: {
    updateConfig(newConfig) {
      Object.assign(this.$state, newConfig)
    }
  }
})
