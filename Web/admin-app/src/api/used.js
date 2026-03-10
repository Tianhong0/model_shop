import request from '../utils/request'

export const getAdminUsedListingPage = (data) => request({
  url: '/api/used/admin/listing/page',
  method: 'post',
  data
})

export const getAdminUsedListingDetail = (listingId) => request({
  url: `/api/used/admin/listing/detail/${listingId}`,
  method: 'get'
})

export const updateAdminUsedListingStatus = (data) => request({
  url: '/api/used/admin/listing/status',
  method: 'post',
  data
})

export const getAdminUsedOrderPage = (data) => request({
  url: '/api/used/admin/order/page',
  method: 'post',
  data
})

export const getAdminUsedOrderDetail = (orderId) => request({
  url: `/api/used/admin/order/detail/${orderId}`,
  method: 'get'
})

export const adminShipUsedOrder = (data) => request({
  url: '/api/used/admin/order/ship',
  method: 'post',
  data
})

export const getAdminUsedReportPage = (data) => request({
  url: '/api/used/admin/report/page',
  method: 'post',
  data
})

export const handleAdminUsedReport = (data) => request({
  url: '/api/used/admin/report/handle',
  method: 'post',
  data
})
