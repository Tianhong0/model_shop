import request from '../utils/request'

export const getAdminOrderList = (data) => {
  return request({
    url: '/api/orders/admin/list',
    method: 'post',
    data
  })
}

export const getAdminOrderDetail = (orderId) => {
  return request({
    url: `/api/orders/admin/detail/${orderId}`,
    method: 'get'
  })
}

export const updateAdminOrderStatus = (data) => {
  return request({
    url: '/api/orders/admin/status',
    method: 'put',
    data
  })
}

export const getDeliveryAdminList = (data) => {
  return request({
    url: '/api/orders/delivery/admin/list',
    method: 'post',
    data
  })
}

export const getDeliveryAdminDetail = (deliveryId) => {
  return request({
    url: `/api/orders/delivery/admin/detail/${deliveryId}`,
    method: 'get'
  })
}

export const shipOrder = (data) => {
  return request({
    url: '/api/orders/delivery/admin/ship',
    method: 'post',
    data
  })
}

export const updateDeliveryStatus = (data) => {
  return request({
    url: '/api/orders/delivery/admin/status',
    method: 'post',
    data
  })
}

export const addDeliveryTrack = (data) => {
  return request({
    url: '/api/orders/delivery/admin/track/add',
    method: 'post',
    data
  })
}

export const simulateDeliveryTrack = (data) => {
  return request({
    url: '/api/orders/delivery/admin/track/simulate',
    method: 'post',
    data
  })
}

export const retryAutoShip = (data) => {
  return request({
    url: '/api/orders/delivery/admin/retry-ship',
    method: 'post',
    data
  })
}

export const getCommentAdminList = (data) => {
  return request({
    url: '/api/orders/comment/admin/list',
    method: 'post',
    data
  })
}

export const getCommentAdminDetail = (commentId) => {
  const safeCommentId = encodeURIComponent(String(commentId || '').trim())
  return request({
    url: `/api/orders/comment/admin/detail/${safeCommentId}`,
    method: 'get'
  })
}

export const getCommentAdminReplyList = (commentId, pageNum = 1, pageSize = 50) => {
  const safeCommentId = encodeURIComponent(String(commentId || '').trim())
  return request({
    url: `/api/orders/comment/admin/reply/list/${safeCommentId}`,
    method: 'get',
    params: { pageNum, pageSize }
  })
}

export const updateCommentAdminStatus = (data) => {
  return request({
    url: '/api/orders/comment/admin/status',
    method: 'post',
    data
  })
}

export const getAfterSaleAdminList = (data) => {
  return request({
    url: '/api/orders/after-sale/admin/list',
    method: 'post',
    data
  })
}

export const getAfterSaleAdminDetail = (afterSaleId) => {
  return request({
    url: `/api/orders/after-sale/admin/detail/${afterSaleId}`,
    method: 'get'
  })
}

export const auditAfterSale = (data) => {
  return request({
    url: '/api/orders/after-sale/admin/audit',
    method: 'post',
    data
  })
}

export const refundAfterSale = (data) => {
  return request({
    url: '/api/orders/after-sale/admin/refund',
    method: 'post',
    data
  })
}

export const getAfterSaleMessages = (data) => {
  return request({
    url: '/api/orders/after-sale/admin/message/page',
    method: 'post',
    data
  })
}

export const sendAfterSaleMessage = (data) => {
  return request({
    url: '/api/orders/after-sale/admin/message/send',
    method: 'post',
    data
  })
}

// 导出订单数据
export const exportOrders = (data) => {
  return request({
    url: '/api/orders/export',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
