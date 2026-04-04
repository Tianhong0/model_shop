import axios from 'axios'
import { ElMessage } from 'element-plus'
import JSONBigFactory from 'json-bigint'

const JSONBig = JSONBigFactory({ storeAsString: true })

// 创建 axios 实例
const request = axios.create({
  // baseURL: 'http://120.48.50.30:9999',
    baseURL: 'http://127.0.0.1:9999',
  timeout: 600000, // 10分钟，大文件上传需要更长超时
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  // 使用 json-bigint 解析响应，避免雪花ID精度丢失
  transformResponse: [(data) => {
    if (typeof data === 'string') {
      try {
        return JSONBig.parse(data)
      } catch (_) {
        return data
      }
    }
    return data
  }]
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
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
    // 如果是 Blob 类型响应（文件下载），直接返回
    if (response.data instanceof Blob) {
      return response.data
    }

    const res = response.data

    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
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
    ElMessage.error(error.response?.data?.message || '请求失败，请检查网络')
    return Promise.reject(error)
  }
)

export default request
