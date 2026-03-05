import request from '../utils/request'

export const getBountyTaskPage = (data) => {
  return request({
    url: '/api/bounty/task/page',
    method: 'post',
    data
  })
}

export const getBountyTaskDetail = (taskId) => {
  return request({
    url: `/api/bounty/admin/task/detail/${taskId}`,
    method: 'get'
  })
}

export const reviewBountyTask = (data) => {
  return request({
    url: '/api/bounty/admin/task/review',
    method: 'post',
    data
  })
}
