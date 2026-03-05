import request from '../utils/request'

export const getModelListApi = (data) => {
  return request({
    url: '/api/model/list',
    method: 'POST',
    data
  })
}

export const getModelDetailApi = (id) => {
  return request({
    url: `/api/model/detail/${id}`,
    method: 'GET'
  })
}

export const getCategoryTreeApi = () => {
  return request({
    url: '/api/model/categories/tree',
    method: 'GET'
  })
}

export const toggleModelFavoriteApi = (modelId) => {
  return request({
    url: '/api/model/favorite/toggle',
    method: 'POST',
    data: { modelId }
  })
}

export const getMyFavoriteModelsApi = (data = {}) => {
  return request({
    url: '/api/model/favorite/my/list',
    method: 'POST',
    data
  })
}

export const getMyFavoriteModelIdsApi = () => {
  return request({
    url: '/api/model/favorite/my/ids',
    method: 'GET'
  })
}
