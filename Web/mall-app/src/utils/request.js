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

// ------------------------------------
// 基础请求（无缓存）
// ------------------------------------
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

// ------------------------------------
// 缓存 + 并发锁层
// ------------------------------------
const CACHE_PREFIX = '__req_cache__'

// 并发锁：key → Promise，同一接口在飞行中时复用同一个 Promise
const inflightMap = new Map()

const buildCacheKey = (url, method, data) => {
  const dataStr = data && Object.keys(data).length ? JSON.stringify(data) : ''
  return `${CACHE_PREFIX}${method}:${url}:${dataStr}`
}

const readCache = (key, maxAge) => {
  try {
    const cached = uni.getStorageSync(key)
    if (!cached || typeof cached !== 'object') return null
    if (!cached.timestamp) return null
    if (Date.now() - cached.timestamp > maxAge) return null
    return cached.data
  } catch (_) {
    return null
  }
}

const writeCache = (key, data) => {
  try {
    uni.setStorageSync(key, { timestamp: Date.now(), data })
  } catch (_) {
    // 存储写入失败静默处理
  }
}

/**
 * 带缓存 + 并发锁的请求方法
 *
 * @param {Object}  options
 * @param {string}  options.url           接口路径
 * @param {string}  [options.method]      HTTP 方法，默认 GET
 * @param {Object}  [options.data]        请求参数
 * @param {Object}  [options.header]      自定义请求头
 * @param {number}  [options.cacheTime]   缓存有效期（ms），传 0 或不传则不缓存
 * @param {boolean} [options.forceUpdate] 强制刷新，跳过缓存直接请求
 * @returns {Promise}
 */
export const cachedRequest = async ({
  url,
  method = 'GET',
  data = {},
  header = {},
  cacheTime = 0,
  forceUpdate = false
}) => {
  const useCache = cacheTime > 0
  const cacheKey = useCache ? buildCacheKey(url, method, data) : ''

  // 1. 读缓存（非强制刷新 & 有缓存时间 & 缓存未过期）
  if (useCache && !forceUpdate) {
    const cached = readCache(cacheKey, cacheTime)
    if (cached !== null) {
      return cached
    }
  }

  // 2. 并发锁：如果同一请求正在飞行，直接复用
  const lockKey = `${method}:${url}:${JSON.stringify(data)}`
  if (inflightMap.has(lockKey)) {
    return inflightMap.get(lockKey)
  }

  // 3. 发起真实请求
  const pending = request({ url, method, data, header })
    .then((result) => {
      // 请求成功 → 写入缓存
      if (useCache) {
        writeCache(cacheKey, result)
      }
      return result
    })
    .finally(() => {
      // 无论成功失败，释放锁
      inflightMap.delete(lockKey)
    })
  // 注意：请求失败时不写缓存，旧的有效缓存会保留

  inflightMap.set(lockKey, pending)
  return pending
}

/**
 * 手动清除指定接口的缓存
 *
 * @param {string} url     接口路径
 * @param {string} method  HTTP 方法
 * @param {Object} data    请求参数（需与调用时一致）
 */
export const clearRequestCache = (url, method = 'GET', data = {}) => {
  const key = buildCacheKey(url, method, data)
  try {
    uni.removeStorageSync(key)
  } catch (_) {
    // ignore
  }
}

/**
 * 批量清除所有请求缓存
 */
export const clearAllRequestCache = () => {
  try {
    const info = uni.getStorageInfoSync()
    const keys = Array.isArray(info?.keys) ? info.keys : []
    keys.forEach((key) => {
      if (key.startsWith(CACHE_PREFIX)) {
        uni.removeStorageSync(key)
      }
    })
  } catch (_) {
    // ignore
  }
}

export default request
