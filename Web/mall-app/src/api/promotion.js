import request from '../utils/request'

// 获取推广中心首页数据
export const getPromotionCenterApi = () => {
  return request({
    url: '/api/promotion/center',
    method: 'GET'
  })
}

// 获取邀请码
export const getInviteCodeApi = () => {
  return request({
    url: '/api/promotion/invite-code',
    method: 'GET'
  })
}

// 记录分享行为
export const recordShareApi = (data) => {
  return request({
    url: '/api/promotion/share',
    method: 'POST',
    data
  })
}

// 获取被邀请人列表
export const getInviteesPageApi = (data) => {
  return request({
    url: '/api/promotion/invitees/page',
    method: 'POST',
    data
  })
}

// 获取推广奖励记录
export const getRewardsPageApi = (data) => {
  return request({
    url: '/api/promotion/rewards/page',
    method: 'POST',
    data
  })
}

// 获取推广排行榜
export const getRankListApi = (limit = 10, period = 'week') => {
  return request({
    url: '/api/promotion/rank',
    method: 'GET',
    params: { limit, period }
  })
}

// 生成推广海报
export const generatePosterApi = () => {
  return request({
    url: '/api/promotion/poster',
    method: 'GET'
  })
}

// 获取海报配置
export const getPosterConfigApi = () => {
  return request({
    url: '/api/promotion/poster/config',
    method: 'GET'
  })
}
