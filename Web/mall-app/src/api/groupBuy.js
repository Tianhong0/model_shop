import request from '../utils/request'

// 获取拼团活动列表
export const getGroupBuyActivitiesApi = (data = {}) => {
  return request({
    url: '/api/group-buy/activities',
    method: 'POST',
    data
  })
}

// 获取活动详情
export const getActivityDetailApi = (activityId) => {
  return request({
    url: `/api/group-buy/activity/${activityId}`,
    method: 'GET'
  })
}

// 获取活动下进行中的拼团列表
export const getOngoingGroupsApi = (activityId, limit = 10) => {
  return request({
    url: `/api/group-buy/activity/${activityId}/ongoing-groups`,
    method: 'GET',
    data: { limit }
  })
}

// 发起拼团
export const createGroupBuyApi = (data) => {
  return request({
    url: '/api/group-buy/create',
    method: 'POST',
    data
  })
}

// 参与拼团
export const joinGroupBuyApi = (data) => {
  return request({
    url: '/api/group-buy/join',
    method: 'POST',
    data
  })
}

// 获取拼团详情
export const getGroupDetailApi = (groupId) => {
  return request({
    url: `/api/group-buy/group/${groupId}`,
    method: 'GET'
  })
}

// 通过分享码获取拼团详情
export const getGroupByShareCodeApi = (shareCode) => {
  return request({
    url: `/api/group-buy/group/by-code/${shareCode}`,
    method: 'GET'
  })
}

// 获取我参与的拼团列表
export const getMyGroupsApi = (data = {}) => {
  return request({
    url: '/api/group-buy/my-groups',
    method: 'POST',
    data
  })
}

// 取消拼团
export const cancelGroupBuyApi = (groupId) => {
  return request({
    url: `/api/group-buy/cancel/${groupId}`,
    method: 'DELETE'
  })
}

// 计算批量打印价格
export const calculateBatchPriceApi = (data) => {
  return request({
    url: '/api/group-buy/calculate-batch-price',
    method: 'POST',
    data
  })
}

// 为参与者创建订单
export const createOrderForParticipantApi = (participantId) => {
  return request({
    url: `/api/group-buy/participant/${participantId}/create-order`,
    method: 'POST'
  })
}

// 获取批量打印折扣配置
export const getBatchDiscountListApi = () => {
  return request({
    url: '/api/group-buy/batch-discount/list',
    method: 'GET'
  })
}
