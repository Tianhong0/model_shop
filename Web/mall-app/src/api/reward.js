import request from '../utils/request'
import { getApiBaseUrl, getApiReachabilityHint } from '../utils/apiBase'

export const getBountyTaskPageApi = (data = {}) => {
  return request({
    url: '/api/bounty/task/page',
    method: 'POST',
    data
  })
}

export const getBountyTaskDetailApi = (taskId) => {
  return request({
    url: `/api/bounty/task/detail/${taskId}`,
    method: 'GET'
  })
}

export const createBountyTaskApi = (data) => {
  return request({
    url: '/api/bounty/task/create',
    method: 'POST',
    data
  })
}

export const createBountyBidApi = (data) => {
  return request({
    url: '/api/bounty/bid/create',
    method: 'POST',
    data
  })
}

export const updateBountyBidApi = (data) => {
  return request({
    url: '/api/bounty/bid/update',
    method: 'POST',
    data
  })
}

export const withdrawBountyBidApi = (bidId) => {
  return request({
    url: `/api/bounty/bid/withdraw/${bidId}`,
    method: 'POST'
  })
}

export const pickBountyBidApi = (data) => {
  return request({
    url: '/api/bounty/bid/pick',
    method: 'POST',
    data
  })
}

export const submitBountyDeliveryApi = (data) => {
  return request({
    url: '/api/bounty/delivery/submit',
    method: 'POST',
    data
  })
}

export const acceptBountyDeliveryApi = (data) => {
  return request({
    url: '/api/bounty/delivery/accept',
    method: 'POST',
    data
  })
}

export const sendBountyMessageApi = (data) => {
  return request({
    url: '/api/bounty/message/send',
    method: 'POST',
    data
  })
}

export const getBountyMessagePageApi = (taskId, pageNum = 1, pageSize = 20) => {
  return request({
    url: `/api/bounty/message/page?taskId=${encodeURIComponent(String(taskId))}&pageNum=${pageNum}&pageSize=${pageSize}`,
    method: 'GET'
  })
}

export const createBountyPriceIncreasePayApi = (priceChangeId) => {
  return request({
    url: `/api/bounty/price/change/pay/create/${priceChangeId}`,
    method: 'POST'
  })
}

export const getBountyPriceIncreasePayStatusApi = (priceChangeId) => {
  return request({
    url: `/api/bounty/price/change/pay/status/${priceChangeId}`,
    method: 'GET'
  })
}

export const syncBountyPriceIncreasePayStatusApi = (priceChangeId) => {
  return request({
    url: `/api/bounty/price/change/pay/sync/${priceChangeId}`,
    method: 'POST'
  })
}

export const getBountyPriceIncreasePayStatusByTaskApi = (taskId) => {
  return request({
    url: `/api/bounty/price/change/pay/status/by-task/${taskId}`,
    method: 'GET'
  })
}

export const syncBountyPriceIncreasePayStatusByTaskApi = (taskId) => {
  return request({
    url: `/api/bounty/price/change/pay/sync/by-task/${taskId}`,
    method: 'POST'
  })
}

export const uploadBountyAttachmentApi = (filePath, type = 'postImg') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveUploadUrl = (body) => {
    if (!body || typeof body !== 'object') return ''
    if (typeof body.data === 'string' && body.data.trim()) {
      return body.data.trim()
    }
    if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim())) {
      return body.message.trim()
    }
    return ''
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/community/file/upload`,
      filePath,
      name: 'file',
      formData: { type },
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          const url = resolveUploadUrl(body)
          if (body?.code === 200 && url) {
            resolve(url)
            return
          }
          reject(new Error(body?.message || '附件上传失败'))
        } catch (_) {
          reject(new Error('附件上传失败'))
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

export const uploadBountyBidAssetApi = (fileSource, assetType = 'image', fileName = '') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveUploadUrl = (body) => {
    if (!body || typeof body !== 'object') return ''
    if (typeof body.data === 'string' && body.data.trim()) {
      return body.data.trim()
    }
    if (typeof body.message === 'string' && /^https?:\/\//.test(body.message.trim())) {
      return body.message.trim()
    }
    return ''
  }

  const uploadConfig = assetType === 'model'
    ? { url: `${baseUrl}/api/file/upload`, type: 'modelFile' }
    : {
        url: `${baseUrl}/api/community/file/upload`,
        type: assetType === 'video' ? 'postVideo' : 'postImg'
      }

  const isBrowserFile = fileSource && typeof fileSource === 'object' && typeof fileSource.arrayBuffer === 'function'

  if (isBrowserFile) {
    return new Promise((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', fileSource, fileName || fileSource.name || 'upload-file')
      formData.append('type', uploadConfig.type)

      fetch(uploadConfig.url, {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      })
        .then(async (res) => {
          if (!res.ok) {
            reject(new Error(`上传失败（status:${res.status}）`))
            return
          }
          const body = await res.json().catch(() => ({}))
          const url = resolveUploadUrl(body)
          if (body?.code === 200 && url) {
            resolve(url)
            return
          }
          reject(new Error(body?.message || '竞标附件上传失败'))
        })
        .catch((error) => {
          reject(new Error(error?.message || `网络异常；${getApiReachabilityHint()}`))
        })
    })
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: uploadConfig.url,
      filePath: fileSource,
      name: 'file',
      formData: { type: uploadConfig.type },
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          const url = resolveUploadUrl(body)
          if (body?.code === 200 && url) {
            resolve(url)
            return
          }
          reject(new Error(body?.message || '竞标附件上传失败'))
        } catch (_) {
          reject(new Error('竞标附件上传失败'))
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
