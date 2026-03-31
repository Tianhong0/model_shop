import request from '../utils/request'

/**
 * 获取可兑换优惠券列表
 */
export const getAvailableTemplatesApi = () => {
  return request({
    url: '/api/coupons/templates/available',
    method: 'GET'
  })
}

/**
 * 积分兑换优惠券
 */
export const exchangeCouponApi = (templateId) => {
  return request({
    url: `/api/coupons/exchange/${templateId}`,
    method: 'POST'
  })
}

/**
 * 获取我的优惠券列表
 */
export const getMyCouponsApi = (data = {}) => {
  return request({
    url: '/api/coupons/my/list',
    method: 'POST',
    data
  })
}

/**
 * 获取订单可用优惠券列表
 */
export const getAvailableCouponsForOrderApi = (orderAmount) => {
  return request({
    url: '/api/coupons/available-for-order',
    method: 'GET',
    params: { orderAmount: orderAmount || 0 }
  })
}

/**
 * 计算优惠券折扣金额
 */
export const calculateCouponDiscountApi = (couponId, orderAmount) => {
  return request({
    url: '/api/coupons/calculate-discount',
    method: 'POST',
    params: { couponId, orderAmount }
  })
}
