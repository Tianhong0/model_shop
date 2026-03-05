import request from '../utils/request'
import { getApiBaseUrl, getApiReachabilityHint } from '../utils/apiBase'

export const getUserDetailApi = (userId) => {
  return request({
    url: `/api/users/${userId}`,
    method: 'GET'
  })
}

export const getDesignerListApi = () => {
  return request({
    url: '/api/users/designers',
    method: 'GET'
  })
}

export const updateUserProfileApi = (data) => {
  return request({
    url: '/api/users/profile',
    method: 'PUT',
    data
  })
}

export const changePasswordApi = (data) => {
  return request({
    url: '/api/users/password',
    method: 'PUT',
    data
  })
}

export const sendChangePasswordEmailCodeApi = () => {
  return request({
    url: '/api/users/password/email-code',
    method: 'POST'
  })
}

export const sendChangeEmailCodeApi = (email) => {
  return request({
    url: '/api/users/profile/email-code',
    method: 'POST',
    data: { email }
  })
}

export const requestAccountDeletionApi = (data) => {
  return request({
    url: '/api/users/deletion-request',
    method: 'POST',
    data
  })
}

export const submitDesignerApplyApi = (data) => {
  return request({
    url: '/api/users/designer-apply',
    method: 'POST',
    data
  })
}

export const getMyDesignerApplyStatusApi = () => {
  return request({
    url: '/api/users/designer-apply/my-status',
    method: 'GET'
  })
}

export const uploadDesignerApplyAttachmentApi = (filePath, type = 'others') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/file/upload`,
      filePath,
      name: 'file',
      formData: {
        type
      },
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          const url = typeof body?.data === 'string' ? body.data.trim() : ''
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

export const uploadAvatarApi = (filePath) => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveAvatarUrl = (body) => {
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
      url: `${baseUrl}/api/file/upload`,
      filePath,
      name: 'file',
      formData: {
        type: 'avatar'
      },
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          const avatarUrl = resolveAvatarUrl(body)
          if (body?.code === 200 && avatarUrl) {
            resolve(avatarUrl)
            return
          }
          reject(new Error(body?.message || '头像上传失败：未获取到图片地址'))
        } catch (error) {
          reject(new Error('头像上传失败'))
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
