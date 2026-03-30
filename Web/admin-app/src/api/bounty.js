import request from '../utils/request'

export const getBountyTaskPage = (data) => {
  return request({
    url: '/api/bounty/admin/task/page',
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

// 申诉相关API
export const getBountyAppealList = (params) => {
  return request({
    url: '/api/bounty/admin/rating/appeal/list',
    method: 'get',
    params
  })
}

export const reviewBountyAppeal = (data) => {
  return request({
    url: '/api/bounty/admin/rating/appeal/review',
    method: 'post',
    data
  })
}

export const reviewBountyCancelTask = (data) => {
  return request({
    url: '/api/bounty/admin/task/cancel-review',
    method: 'post',
    data
  })
}

// 信誉相关API
export const getDesignerReputation = (designerId) => {
  return request({
    url: `/api/bounty/reputation/${designerId}`,
    method: 'get'
  })
}
