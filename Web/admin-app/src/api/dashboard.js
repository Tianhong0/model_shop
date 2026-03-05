import request from '../utils/request'

export const getDashboardOverview = () => {
  return request({
    url: '/api/dashboard/admin/overview',
    method: 'get'
  })
}

export const getDashboardMessages = () => {
  return request({
    url: '/api/dashboard/admin/messages',
    method: 'get'
  })
}

export const markAllDashboardMessagesRead = () => {
  return request({
    url: '/api/dashboard/admin/messages/read-all',
    method: 'post'
  })
}
