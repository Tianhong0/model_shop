import request from '../utils/request'
import { getApiBaseUrl, getApiReachabilityHint } from '../utils/apiBase'

export const getUsedListingPageApi = (data = {}) => request({
  url: '/api/used/listing/page',
  method: 'POST',
  data
})

export const getMyUsedListingPageApi = (data = {}) => request({
  url: '/api/used/listing/my/page',
  method: 'POST',
  data
})

export const getUsedListingDetailApi = (listingId) => request({
  url: `/api/used/listing/detail/${listingId}`,
  method: 'GET'
})

export const createUsedListingApi = (data) => request({
  url: '/api/used/listing/create',
  method: 'POST',
  data
})

export const updateUsedListingApi = (data) => request({
  url: '/api/used/listing/update',
  method: 'POST',
  data
})

export const updateUsedListingStatusApi = (data) => request({
  url: '/api/used/listing/status',
  method: 'POST',
  data
})

export const createUsedOfferApi = (data) => request({
  url: '/api/used/offer/create',
  method: 'POST',
  data
})

export const respondUsedOfferApi = (data) => request({
  url: '/api/used/offer/respond',
  method: 'POST',
  data
})

export const createUsedOrderApi = (data) => request({
  url: '/api/used/order/create',
  method: 'POST',
  data
})

export const payUsedOrderApi = (orderId) => request({
  url: `/api/used/order/pay/${orderId}`,
  method: 'POST'
})

export const payUsedOrderByWalletApi = (orderId) => request({
  url: `/api/used/order/pay/wallet/${orderId}`,
  method: 'POST'
})

export const getUsedOrderPayStatusApi = (orderId) => request({
  url: `/api/used/order/pay/status/${orderId}`,
  method: 'GET'
})

export const syncUsedOrderPayStatusApi = (orderId) => request({
  url: `/api/used/order/pay/sync/${orderId}`,
  method: 'POST'
})

export const cancelUsedOrderApi = (orderId) => request({
  url: `/api/used/order/cancel/${orderId}`,
  method: 'POST'
})

export const getMyBuyUsedOrderPageApi = (data = {}) => request({
  url: '/api/used/order/buy/page',
  method: 'POST',
  data
})

export const getMySellUsedOrderPageApi = (data = {}) => request({
  url: '/api/used/order/sell/page',
  method: 'POST',
  data
})

export const getUsedOrderDetailApi = (orderId) => request({
  url: `/api/used/order/detail/${orderId}`,
  method: 'GET'
})

export const shipUsedOrderApi = (data) => request({
  url: '/api/used/order/ship',
  method: 'POST',
  data
})

export const confirmUsedOrderApi = (orderId) => request({
  url: `/api/used/order/confirm/${orderId}`,
  method: 'POST'
})

export const sendUsedMessageApi = (data) => request({
  url: '/api/used/message/send',
  method: 'POST',
  data
})

export const getUsedMessageSessionListApi = (listingId) => request({
  url: `/api/used/message/session/list?listingId=${encodeURIComponent(String(listingId))}`,
  method: 'GET'
})

export const getUsedMessagePageApi = (listingId, counterpartId, pageNum = 1, pageSize = 100) => request({
  url: `/api/used/message/page?listingId=${encodeURIComponent(String(listingId))}&counterpartId=${encodeURIComponent(String(counterpartId))}&pageNum=${pageNum}&pageSize=${pageSize}`,
  method: 'GET'
})

export const createUsedAfterSaleApi = (data) => request({
  url: '/api/used/after-sale/create',
  method: 'POST',
  data
})

export const getMyUsedAfterSalePageApi = (data = {}) => request({
  url: '/api/used/after-sale/buy/page',
  method: 'POST',
  data
})

export const auditUsedAfterSaleBySellerApi = (data) => request({
  url: '/api/used/after-sale/seller/audit',
  method: 'POST',
  data
})

export const createUsedReportApi = (data) => request({
  url: '/api/used/report/create',
  method: 'POST',
  data
})

export const uploadUsedImageApi = (filePath) => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveMediaUrl = (body) => {
    if (!body || typeof body !== 'object') return ''
    if (typeof body.data === 'string' && body.data.trim()) return body.data.trim()
    if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim())) return body.message.trim()
    return ''
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/file/upload`,
      filePath,
      name: 'file',
      formData: { type: 'modelImg' },
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          const mediaUrl = resolveMediaUrl(body)
          if (body?.code === 200 && mediaUrl) {
            resolve(mediaUrl)
            return
          }
          reject(new Error(body?.message || '图片上传失败'))
        } catch (_) {
          reject(new Error('图片上传失败'))
        }
      },
      fail: (error) => {
        const statusCode = error?.statusCode
        const errMsg = error?.errMsg || '网络异常'
        if (statusCode == null) {
          reject(new Error(`${errMsg}（statusCode:null）；${getApiReachabilityHint()}`))
          return
        }
        reject(new Error(`${errMsg}（statusCode:${statusCode}）`))
      }
    })
  })
}

export const uploadUsedChatMediaApi = (filePath, mediaType = 'image') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveMediaUrl = (body) => {
    if (!body || typeof body !== 'object') return ''
    if (typeof body.data === 'string' && body.data.trim()) return body.data.trim()
    if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim())) return body.message.trim()
    return ''
  }

  const uploadType = mediaType === 'video' ? 'postVideo' : 'postImg'

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/community/file/upload`,
      filePath,
      name: 'file',
      formData: { type: uploadType },
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          const mediaUrl = resolveMediaUrl(body)
          if (body?.code === 200 && mediaUrl) {
            resolve(mediaUrl)
            return
          }
          reject(new Error(body?.message || `${mediaType === 'video' ? '视频' : '图片'}上传失败`))
        } catch (_) {
          reject(new Error(`${mediaType === 'video' ? '视频' : '图片'}上传失败`))
        }
      },
      fail: (error) => {
        const statusCode = error?.statusCode
        const errMsg = error?.errMsg || '网络异常'
        if (statusCode == null) {
          reject(new Error(`${errMsg}（statusCode:null）；${getApiReachabilityHint()}`))
          return
        }
        reject(new Error(`${errMsg}（statusCode:${statusCode}）`))
      }
    })
  })
}
