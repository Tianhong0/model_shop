const DEFAULT_WEB_BASE_URL = 'http://192.168.1.7:9999'

export const getApiBaseUrl = () => {
  const customBaseUrl = String(uni.getStorageSync('api_base_url') || '').trim()
  if (customBaseUrl) {
    return customBaseUrl.replace(/\/+$/, '')
  }
  return DEFAULT_WEB_BASE_URL
}

export const getApiReachabilityHint = () => {
  return '当前后端地址不可达，请在“设置 api_base_url”中改为电脑局域网IP，如 http://192.168.x.x:9999'
}

export default getApiBaseUrl
