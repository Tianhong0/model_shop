import request from '../utils/request'

// 管理端API

export const getModelListAdminPage = (data) => {
  return request({
    url: '/api/model-list/admin/page',
    method: 'post',
    data
  })
}

export const getModelListAdminDetail = (listId) => {
  return request({
    url: `/api/model-list/admin/detail/${listId}`,
    method: 'get'
  })
}

export const updateModelListStatus = (data) => {
  return request({
    url: '/api/model-list/admin/status',
    method: 'post',
    data
  })
}

export const deleteModelList = (listId) => {
  return request({
    url: `/api/model-list/admin/delete/${listId}`,
    method: 'post'
  })
}

export const batchUpdateModelListStatus = (data) => {
  return request({
    url: '/api/model-list/admin/batch/status',
    method: 'post',
    data
  })
}

export const batchDeleteModelList = (data) => {
  return request({
    url: '/api/model-list/admin/batch/delete',
    method: 'post',
    data
  })
}

export const getModelListStatistics = () => {
  return request({
    url: '/api/model-list/admin/statistics',
    method: 'get'
  })
}
