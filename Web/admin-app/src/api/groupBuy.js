import request from '../utils/request'

// 创建拼团活动
export const createActivityApi = (data) => {
  return request({
    url: '/api/admin/group-buy/activity/create',
    method: 'post',
    data
  })
}

// 更新拼团活动
export const updateActivityApi = (data) => {
  return request({
    url: '/api/admin/group-buy/activity/update',
    method: 'put',
    data
  })
}

// 分页查询拼团活动
export const getActivityListApi = (data) => {
  return request({
    url: '/api/admin/group-buy/activity/list',
    method: 'post',
    data
  })
}

// 获取活动详情
export const getActivityDetailApi = (activityId) => {
  return request({
    url: `/api/admin/group-buy/activity/detail/${activityId}`,
    method: 'get'
  })
}

// 更新活动状态
export const updateActivityStatusApi = (activityId, status) => {
  return request({
    url: `/api/admin/group-buy/activity/status/${activityId}`,
    method: 'put',
    params: { status }
  })
}

// 获取批量打印折扣配置
export const getBatchDiscountListApi = () => {
  return request({
    url: '/api/admin/group-buy/batch-discount/list',
    method: 'get'
  })
}

// 保存批量打印折扣配置
export const saveBatchDiscountApi = (data) => {
  return request({
    url: '/api/admin/group-buy/batch-discount/save',
    method: 'post',
    data
  })
}
