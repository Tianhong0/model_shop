import request from '../utils/request'

export const getBannerAdminList = (data) => {
  return request({
    url: '/api/operation/banner/admin/list',
    method: 'post',
    data
  })
}

export const getBannerDetail = (id) => {
  return request({
    url: `/api/operation/banner/admin/detail/${id}`,
    method: 'get'
  })
}

export const createBanner = (data) => {
  return request({
    url: '/api/operation/banner/admin/create',
    method: 'post',
    data
  })
}

export const updateBanner = (data) => {
  return request({
    url: '/api/operation/banner/admin/update',
    method: 'put',
    data
  })
}

export const updateBannerStatus = (data) => {
  return request({
    url: '/api/operation/banner/admin/status',
    method: 'put',
    data
  })
}

export const deleteBanner = (id) => {
  return request({
    url: `/api/operation/banner/admin/delete/${id}`,
    method: 'delete'
  })
}

export const getNoticeAdminList = (data) => {
  return request({
    url: '/api/operation/notice/admin/list',
    method: 'post',
    data
  })
}

export const getNoticeDetail = (id) => {
  return request({
    url: `/api/operation/notice/admin/detail/${id}`,
    method: 'get'
  })
}

export const createNotice = (data) => {
  return request({
    url: '/api/operation/notice/admin/create',
    method: 'post',
    data
  })
}

export const updateNotice = (data) => {
  return request({
    url: '/api/operation/notice/admin/update',
    method: 'put',
    data
  })
}

export const updateNoticeStatus = (data) => {
  return request({
    url: '/api/operation/notice/admin/status',
    method: 'put',
    data
  })
}

export const deleteNotice = (id) => {
  return request({
    url: `/api/operation/notice/admin/delete/${id}`,
    method: 'delete'
  })
}

export const getHomeConfig = () => {
  return request({
    url: '/api/operation/home/config',
    method: 'get'
  })
}

export const getAdminOperationStatus = () => {
  return request({
    url: '/api/operation/admin/status',
    method: 'get'
  })
}

export const updateAdminOperationStatus = (data) => {
  return request({
    url: '/api/operation/admin/status',
    method: 'put',
    data
  })
}
