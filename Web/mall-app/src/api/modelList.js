import request from '../utils/request'

export const createModelListApi = (data) => {
  return request({
    url: '/api/model-list/create',
    method: 'POST',
    data
  })
}

export const updateModelListApi = (data) => {
  return request({
    url: '/api/model-list/update',
    method: 'POST',
    data
  })
}

export const deleteModelListApi = (listId) => {
  return request({
    url: `/api/model-list/delete/${listId}`,
    method: 'POST'
  })
}

export const publishModelListApi = (listId) => {
  return request({
    url: `/api/model-list/publish/${listId}`,
    method: 'POST'
  })
}

export const addModelListItemsApi = (data) => {
  return request({
    url: '/api/model-list/item/add',
    method: 'POST',
    data
  })
}

export const removeModelListItemApi = (data) => {
  return request({
    url: '/api/model-list/item/remove',
    method: 'POST',
    data
  })
}

export const sortModelListItemsApi = (data) => {
  return request({
    url: '/api/model-list/item/sort',
    method: 'POST',
    data
  })
}

export const getModelListPageApi = (data) => {
  return request({
    url: '/api/model-list/page',
    method: 'POST',
    data
  })
}

export const getModelListDetailApi = (listId) => {
  return request({
    url: `/api/model-list/detail/${listId}`,
    method: 'GET'
  })
}

export const toggleModelListInteractionApi = (data) => {
  return request({
    url: '/api/model-list/interaction/toggle',
    method: 'POST',
    data
  })
}

export const getMyModelListPageApi = (data) => {
  return request({
    url: '/api/model-list/my/page',
    method: 'POST',
    data
  })
}

export const getMyModelListInteractionPageApi = (data) => {
  return request({
    url: '/api/model-list/interaction/my/page',
    method: 'POST',
    data
  })
}
