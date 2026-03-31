import request from '../utils/request'

// 获取优惠券模板列表
export const getCouponTemplateList = (data) => {
  return request({
    url: '/api/coupons/admin/templates/list',
    method: 'post',
    data
  })
}

// 获取优惠券模板详情
export const getCouponTemplateDetail = (id) => {
  return request({
    url: `/api/coupons/admin/templates/${id}`,
    method: 'get'
  })
}

// 创建优惠券模板
export const createCouponTemplate = (data) => {
  return request({
    url: '/api/coupons/admin/templates',
    method: 'post',
    data
  })
}

// 更新优惠券模板状态
export const updateCouponTemplateStatus = (id, status) => {
  return request({
    url: `/api/coupons/admin/templates/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 更新优惠券模板
export const updateCouponTemplate = (id, data) => {
  return request({
    url: `/api/coupons/admin/templates/${id}`,
    method: 'put',
    data
  })
}

// 删除优惠券模板
export const deleteCouponTemplate = (id) => {
  return request({
    url: `/api/coupons/admin/templates/${id}`,
    method: 'delete'
  })
}
