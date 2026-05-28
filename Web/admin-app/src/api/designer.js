import request from '../utils/request'

/**
 * 设计师分润相关API
 */

// 分页查询分润结算列表
export const getDesignerSettlements = (data) => {
  return request({
    url: '/api/designer/settlement/list',
    method: 'post',
    data
  })
}

// 重试失败的结算
export const retrySettlement = (id) => {
  return request({
    url: `/api/designer/settlement/retry/${id}`,
    method: 'post'
  })
}
