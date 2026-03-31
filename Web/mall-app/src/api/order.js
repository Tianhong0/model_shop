import request from '../utils/request'
import { getApiBaseUrl, getApiReachabilityHint } from '../utils/apiBase'

export const getMyOrdersApi = (data = {}) => {
  return request({
    url: '/api/orders/list',
    method: 'POST',
    data
  })
}

export const cancelOrderApi = (orderId) => {
  return request({
    url: `/api/orders/cancel/${orderId}`,
    method: 'PUT'
  })
}

export const deleteOrderApi = (orderId) => {
  return request({
    url: `/api/orders/delete/${orderId}`,
    method: 'DELETE'
  })
}

export const createOrderApi = (data) => {
  return request({
    url: '/api/orders/create',
    method: 'POST',
    data
  })
}

export const createAlipayAppPayApi = (data) => {
  return request({
    url: '/api/orders/pay/app/create',
    method: 'POST',
    data
  })
}

export const createAlipayBatchPayApi = (data) => {
  return request({
    url: '/api/orders/pay/app/create-batch',
    method: 'POST',
    data
  })
}

export const payOrderByWalletApi = (data) => {
  return request({
    url: '/api/orders/pay/wallet/pay',
    method: 'POST',
    data
  })
}

export const payBatchByWalletApi = (data) => {
  return request({
    url: '/api/orders/pay/wallet/pay-batch',
    method: 'POST',
    data
  })
}

export const getOrderPayStatusApi = (orderId) => {
  return request({
    url: `/api/orders/pay/status/${orderId}`,
    method: 'GET'
  })
}

export const syncOrderPayStatusApi = (orderId) => {
  return request({
    url: `/api/orders/pay/sync/${orderId}`,
    method: 'POST'
  })
}

export const getBatchPayStatusApi = (batchId) => {
  return request({
    url: `/api/orders/pay/status/batch/${batchId}`,
    method: 'GET'
  })
}

export const syncBatchPayStatusApi = (batchId) => {
  return request({
    url: `/api/orders/pay/sync/batch/${batchId}`,
    method: 'POST'
  })
}

export const getOrderDetailApi = (orderId) => {
  return request({
    url: `/api/orders/detail/${orderId}`,
    method: 'GET'
  })
}

export const getOrderDetailBySnApi = (orderSn) => {
  return request({
    url: `/api/orders/detail/by-sn/${encodeURIComponent(orderSn)}`,
    method: 'GET'
  })
}

export const getMyDeliveryDetailApi = (orderSn) => {
  return request({
    url: '/api/orders/delivery/my/detail',
    method: 'POST',
    data: { orderSn }
  })
}

export const confirmOrderReceiveApi = (orderSn) => {
  return request({
    url: `/api/orders/delivery/my/sign/${encodeURIComponent(orderSn)}`,
    method: 'POST'
  })
}

export const createOrderCommentApi = (data) => {
  return request({
    url: '/api/orders/comment/create',
    method: 'POST',
    data
  })
}

export const uploadOrderCommentMediaApi = (filePath, mediaKind = 'image') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveMediaUrl = (body) => {
    if (!body || typeof body !== 'object') return ''
    if (typeof body.data === 'string' && body.data.trim()) {
      return body.data.trim()
    }
    if (body.data && typeof body.data === 'object') {
      const nestedUrl = body.data.url || body.data.fileUrl || body.data.path
      if (typeof nestedUrl === 'string' && nestedUrl.trim()) {
        return nestedUrl.trim()
      }
    }
    if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim())) {
      return body.message.trim()
    }
    return ''
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/orders/comment/my/media/upload`,
      filePath,
      name: 'file',
      formData: {
        type: mediaKind === 'video' ? 'video' : 'image'
      },
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
          reject(new Error(body?.message || '评价媒体上传失败：未获取到地址'))
        } catch (_) {
          reject(new Error('评价媒体上传失败'))
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

export const getMyOrderCommentsApi = (data = {}) => {
  return request({
    url: '/api/orders/comment/my/list',
    method: 'POST',
    data
  })
}

export const getModelOrderCommentsApi = (data = {}) => {
  return request({
    url: '/api/orders/comment/model/list',
    method: 'POST',
    data
  })
}

export const toggleModelCommentLikeApi = (data = {}) => {
  return request({
    url: '/api/orders/comment/model/like/toggle',
    method: 'POST',
    data
  })
}

export const getModelCommentRepliesApi = (data = {}) => {
  return request({
    url: '/api/orders/comment/reply/list',
    method: 'POST',
    data
  })
}

export const createModelCommentReplyApi = (data = {}) => {
  return request({
    url: '/api/orders/comment/reply/create',
    method: 'POST',
    data
  })
}

export const toggleModelCommentReplyLikeApi = (data = {}) => {
  return request({
    url: '/api/orders/comment/reply/like/toggle',
    method: 'POST',
    data
  })
}

export const getModelOrderCommentStatsApi = (modelId) => {
  return request({
    url: `/api/orders/comment/model/stats/${modelId}`,
    method: 'GET'
  })
}

export const createAfterSaleApi = (data) => {
  return request({
    url: '/api/orders/after-sale/create',
    method: 'POST',
    data
  })
}

export const getMyAfterSaleListApi = (data = {}) => {
  return request({
    url: '/api/orders/after-sale/my/list',
    method: 'POST',
    data
  })
}

export const getMyAfterSaleDetailApi = (afterSaleId) => {
  return request({
    url: `/api/orders/after-sale/my/detail/${afterSaleId}`,
    method: 'GET'
  })
}

export const getMyAfterSaleDetailBySnApi = (afterSaleSn) => {
  return request({
    url: `/api/orders/after-sale/my/detail/by-sn/${encodeURIComponent(afterSaleSn)}`,
    method: 'GET'
  })
}

export const cancelAfterSaleApi = (afterSaleId) => {
  return request({
    url: `/api/orders/after-sale/my/cancel/${afterSaleId}`,
    method: 'DELETE'
  })
}

export const cancelAfterSaleBySnApi = (afterSaleSn) => {
  return request({
    url: `/api/orders/after-sale/my/cancel/by-sn/${encodeURIComponent(afterSaleSn)}`,
    method: 'DELETE'
  })
}

export const getAfterSaleMessagesApi = (data) => {
  return request({
    url: '/api/orders/after-sale/my/message/page',
    method: 'POST',
    data
  })
}

export const sendAfterSaleMessageApi = (data) => {
  return request({
    url: '/api/orders/after-sale/my/message/send',
    method: 'POST',
    data
  })
}

export const uploadAfterSaleMediaApi = (filePath, mediaKind = 'image') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveMediaUrl = (body) => {
    if (!body || typeof body !== 'object') return ''
    if (typeof body.data === 'string' && body.data.trim()) {
      return body.data.trim()
    }
    if (body.data && typeof body.data === 'object') {
      const nestedUrl = body.data.url || body.data.fileUrl || body.data.path
      if (typeof nestedUrl === 'string' && nestedUrl.trim()) {
        return nestedUrl.trim()
      }
    }
    if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim())) {
      return body.message.trim()
    }
    return ''
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/orders/after-sale/my/evidence/upload`,
      filePath,
      name: 'file',
      formData: {
        type: mediaKind === 'video' ? 'video' : 'image'
      },
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
          reject(new Error(body?.message || '凭证上传失败：未获取到地址'))
        } catch (_) {
          reject(new Error('凭证上传失败'))
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

// 打印故障诊断
export const getPrintFaultDiagnosisApi = (orderId) => {
  return request({
    url: `/api/orders/print/fault/diagnosis/${orderId}`,
    method: 'GET'
  })
}

// 用户重试打印
export const userRetryPrintApi = (orderId) => {
  return request({
    url: `/api/orders/print/fault/retry/${orderId}`,
    method: 'POST'
  })
}
