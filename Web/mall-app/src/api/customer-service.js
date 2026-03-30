import request from '../utils/request'
import { getApiBaseUrl } from '../utils/apiBase'

// 用户端

/**
 * 发起客服会话
 */
export const startCsConversationApi = () => {
  return request({
    url: '/api/cs/conversation/start',
    method: 'POST'
  })
}

/**
 * 获取会话状态
 */
export const getCsConversationStatusApi = () => {
  return request({
    url: '/api/cs/conversation/status',
    method: 'GET'
  })
}

/**
 * 获取会话消息
 */
export const getCsMessagesApi = (conversationId, pageNum = 1, pageSize = 20) => {
  // 添加时间戳防止缓存
  const t = Date.now()
  return request({
    url: `/api/cs/messages/${conversationId}`,
    method: 'GET',
    data: { pageNum, pageSize, _t: t }
  })
}

/**
 * 发送消息
 */
export const sendCsMessageApi = (data) => {
  return request({
    url: '/api/cs/message/send',
    method: 'POST',
    data
  })
}

/**
 * 标记消息已读
 */
export const markCsMessageReadApi = (conversationId) => {
  return request({
    url: `/api/cs/message/read/${conversationId}`,
    method: 'PUT'
  })
}

/**
 * 结束会话
 */
export const endCsConversationApi = (conversationId) => {
  return request({
    url: `/api/cs/conversation/end/${conversationId}`,
    method: 'POST'
  })
}

/**
 * 获取在线客服列表
 */
export const getOnlineAdminsApi = () => {
  return request({
    url: '/api/cs/online-admins',
    method: 'GET'
  })
}

/**
 * 上传聊天媒体文件
 */
export const uploadCsMediaApi = (filePath, mediaType) => {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: getApiBaseUrl() + '/api/cs/upload',
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${uni.getStorageSync('token')}` },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data.data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}
