import request from '../utils/request'

/**
 * 推广配置管理API
 */

// 获取所有推广配置
export const getPromotionConfigs = () => {
  return request({
    url: '/api/promotion/admin/configs',
    method: 'get'
  })
}

// 更新推广配置
export const updatePromotionConfig = (data) => {
  return request({
    url: '/api/promotion/admin/config/update',
    method: 'post',
    data
  })
}

// 批量更新推广配置
export const batchUpdatePromotionConfigs = (data) => {
  return request({
    url: '/api/promotion/admin/configs/batch',
    method: 'post',
    data
  })
}
