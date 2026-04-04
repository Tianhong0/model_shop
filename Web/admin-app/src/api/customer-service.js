import request from '@/utils/request'

/**
 * 获取客服会话列表
 */
export function getCsConversationsApi(params) {
  return request({
    url: '/api/cs/admin/conversations',
    method: 'get',
    params
  })
}

/**
 * 客服接单
 */
export function acceptCsConversationApi(id) {
  return request({
    url: `/api/cs/admin/conversation/accept/${id}`,
    method: 'post'
  })
}

/**
 * 客服结束会话
 */
export function endCsConversationApi(id, reason) {
  return request({
    url: `/api/cs/admin/conversation/end/${id}`,
    method: 'post',
    params: { reason }
  })
}

/**
 * 获取会话消息
 */
export function getCsMessagesApi(conversationId, params) {
  return request({
    url: `/api/cs/messages/${conversationId}`,
    method: 'get',
    params
  })
}

/**
 * 客服发送消息
 */
export function sendCsMessageApi(data) {
  return request({
    url: '/api/cs/admin/message/send',
    method: 'post',
    data
  })
}

/**
 * 设置客服在线状态
 */
export function setCsAdminStatusApi(online) {
  return request({
    url: '/api/cs/admin/status',
    method: 'post',
    params: { online }
  })
}

/**
 * 客服心跳
 */
export function csHeartbeatApi() {
  return request({
    url: '/api/cs/admin/heartbeat',
    method: 'post'
  })
}

/**
 * 获取会话详情（历史）
 */
export function getCsConversationHistoryApi(id) {
  return request({
    url: `/api/cs/admin/history/${id}`,
    method: 'get'
  })
}

/**
 * 标记消息已读
 */
export function markCsAdminReadApi(conversationId) {
  return request({
    url: `/api/cs/admin/message/read/${conversationId}`,
    method: 'put'
  })
}

/**
 * 上传客服聊天媒体文件
 */
export function uploadCsMediaApi(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/cs/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取客服统计数据
 */
export function getCsStatsApi() {
  return request({
    url: '/api/cs/admin/stats',
    method: 'get'
  })
}
