import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
  baseURL: 'http://localhost:9999', // 移除基础URL，避免与代理冲突
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true, // 允许跨域请求携带cookie
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    console.log('Request URL:', config.url)
    console.log('Request Method:', config.method)
    console.log('Request Data:', config.data)
    console.log('Request Headers:', config.headers)

    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('Auth Token:', token)
    }

    // 添加额外的调试信息
    console.log('Request Config:', {
      url: config.url,
      method: config.method,
      headers: config.headers,
      data: config.data
    })

    return config
  },
  error => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    console.log('Response Data:', res)
    console.log('Response Code:', res.code)
    console.log('Response Message:', res.message)
    console.log('Response Data Structure:', res.data)

    // 如果返回的 code 不是 200，则抛出错误
    if (res.code !== 200) {
      if (typeof ElMessage !== 'undefined') {
        ElMessage.error(res.message || '请求失败')
      } else {
        console.error('ElMessage is not defined, showing error in console:', res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    // 上传接口兼容：部分后端将URL放在message而非data
    if (response.config.url?.includes('/api/file/upload')) {
      return res.data || res.message
    }

    return res.data
  },
  error => {
    console.error('API Error:', error)
    console.error('Error Response:', error.response?.data)
    if (typeof ElMessage !== 'undefined') {
      ElMessage.error(error.response?.data?.message || '请求失败，请检查网络')
    } else {
      console.error('ElMessage is not defined, showing error in console:', error.response?.data?.message || '请求失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default request