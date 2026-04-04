import request from '@/utils/request'

/**
 * 获取订单统计
 */
export const getOrderStatistics = (startDate, endDate) => {
  return request({
    url: '/api/statistics/orders',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取用户统计
 */
export const getUserStatistics = (startDate, endDate) => {
  return request({
    url: '/api/statistics/users',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取模型统计
 */
export const getModelStatistics = (startDate, endDate) => {
  return request({
    url: '/api/statistics/models',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取财务统计
 */
export const getFinanceStatistics = (startDate, endDate) => {
  return request({
    url: '/api/statistics/finance',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取悬赏统计
 */
export const getBountyStatistics = (startDate, endDate) => {
  return request({
    url: '/api/statistics/bounty',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 导出统计报表
 */
export const exportStatistics = (module, startDate, endDate) => {
  return request({
    url: '/api/statistics/export',
    method: 'get',
    params: { module, startDate, endDate },
    responseType: 'blob'
  })
}
