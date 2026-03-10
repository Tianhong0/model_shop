import request from '../utils/request'

export const getNotificationPageApi = (data = {}) => request({
  url: '/api/notification/page',
  method: 'POST',
  data
})

export const getNotificationUnreadSummaryApi = () => request({
  url: '/api/notification/unread/summary',
  method: 'GET'
})

export const readNotificationApi = (notificationId) => request({
  url: `/api/notification/read/${notificationId}`,
  method: 'POST'
})

export const readAllNotificationsApi = (category) => request({
  url: `/api/notification/read/all${category ? `?category=${encodeURIComponent(String(category))}` : ''}`,
  method: 'POST'
})

export const getPendingPopupNotificationsApi = (limit = 10) => request({
  url: `/api/notification/popup/pending?limit=${encodeURIComponent(String(limit))}`,
  method: 'GET'
})

export const ackPopupNotificationsApi = (ids = []) => request({
  url: '/api/notification/popup/ack',
  method: 'POST',
  data: { ids }
})
