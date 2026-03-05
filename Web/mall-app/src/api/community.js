import request from '../utils/request'
import { getApiBaseUrl, getApiReachabilityHint } from '../utils/apiBase'

export const getPostCategoryListApi = () => {
  return request({
    url: '/api/community/category/list',
    method: 'GET'
  })
}

export const getPostPageApi = (data) => {
  return request({
    url: '/api/community/post/page',
    method: 'POST',
    data
  })
}

export const getPostDetailApi = (postId) => {
  return request({
    url: `/api/community/post/detail/${postId}`,
    method: 'GET'
  })
}

export const createPostApi = (data) => {
  return request({
    url: '/api/community/post/create',
    method: 'POST',
    data
  })
}

export const updatePostApi = (data) => {
  return request({
    url: '/api/community/post/update',
    method: 'POST',
    data
  })
}

export const deletePostApi = (postId) => {
  return request({
    url: `/api/community/post/delete/${postId}`,
    method: 'POST'
  })
}

export const getMyPostPageApi = (data) => {
  return request({
    url: '/api/community/post/my/page',
    method: 'POST',
    data
  })
}

export const createReplyApi = (data) => {
  return request({
    url: '/api/community/reply/create',
    method: 'POST',
    data
  })
}

export const deleteReplyApi = (replyId) => {
  return request({
    url: `/api/community/reply/delete/${replyId}`,
    method: 'POST'
  })
}

export const adoptReplyApi = (data) => {
  return request({
    url: '/api/community/reply/adopt',
    method: 'POST',
    data
  })
}

export const toggleReplyLikeApi = (data) => {
  return request({
    url: '/api/community/reply/like/toggle',
    method: 'POST',
    data
  })
}

export const togglePostInteractionApi = (data) => {
  return request({
    url: '/api/community/interaction/toggle',
    method: 'POST',
    data
  })
}

export const getMyInteractionPageApi = (data) => {
  return request({
    url: '/api/community/interaction/my/page',
    method: 'POST',
    data
  })
}

export const uploadCommunityMediaApi = (filePath, type = 'postImg') => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl()

  const resolveMediaUrl = (body) => {
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
          const mediaUrl = resolveMediaUrl(body)
          if (body?.code === 200 && mediaUrl) {
            resolve(mediaUrl)
            return
          }
          reject(new Error(body?.message || '媒体上传失败：未获取到地址'))
        } catch (error) {
          reject(new Error('媒体上传失败'))
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
