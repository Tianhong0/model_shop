import { getApiBaseUrl, getApiReachabilityHint } from './apiBase'
import JSONBigFactory from 'json-bigint'

const JSONBig = JSONBigFactory({ storeAsString: true })

const parseBody = (rawData) => {
  if (rawData == null || rawData === '') return {}
  if (typeof rawData === 'string') {
    try {
      return JSONBig.parse(rawData)
    } catch (error) {
      return {}
    }
  }
  if (typeof rawData === 'object') {
    return rawData
  }
  return {}
}

const request = ({ url, method = 'GET', data = {}, header = {} }) => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl}${url}`,
      method,
      data,
      dataType: 'text',
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...header
      },
      success: (res) => {
        const body = parseBody(res.data)
        if (body.code === 200) {
          resolve(body.data)
          return
        }
        reject(new Error(body.message || '请求失败'))
      },
      fail: (err) => {
        const errMsg = err?.errMsg || '网络异常'
        if (String(errMsg).includes('fail')) {
          reject(new Error(`${errMsg}；${getApiReachabilityHint()}`))
          return
        }
        reject(new Error(errMsg))
      }
    })
  })
}

export default request
