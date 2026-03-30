const DEFAULT_WEB_BASE_URL = 'http://120.48.50.30:9999'

export const getApiBaseUrl = () => {
  const customBaseUrl = String(uni.getStorageSync('api_base_url') || '').trim()
  if (customBaseUrl) {
    return customBaseUrl.replace(/\/+$/, '')
  }
  return DEFAULT_WEB_BASE_URL
}

export const getApiReachabilityHint = () => {
  return '当前后端地址不可达，请设置IP，如 http://192.168.x.x:9999'
}

export default getApiBaseUrl
