import request, { cachedRequest } from '../utils/request'

// 缓存时间常量
const CACHE_5MIN = 5 * 60 * 1000
const CACHE_24H = 24 * 60 * 60 * 1000

/**
 * 模型列表（缓存 5 分钟）
 * @param {Object}  data        请求参数
 * @param {boolean} forceUpdate 强制刷新（下拉刷新时传 true）
 */
export const getModelListApi = (data, forceUpdate = false) => {
  return cachedRequest({
    url: '/api/model/list',
    method: 'POST',
    data,
    cacheTime: CACHE_5MIN,
    forceUpdate
  })
}

/**
 * 模型详情（缓存 5 分钟）
 * @param {string}  id          模型 ID
 * @param {boolean} forceUpdate 强制刷新
 */
export const getModelDetailApi = (id, forceUpdate = false) => {
  return cachedRequest({
    url: `/api/model/detail/${id}`,
    method: 'GET',
    cacheTime: CACHE_5MIN,
    forceUpdate
  })
}

/**
 * 分类树（缓存 24 小时，极少变动）
 * @param {boolean} forceUpdate 强制刷新
 */
export const getCategoryTreeApi = (forceUpdate = false) => {
  return cachedRequest({
    url: '/api/model/categories/tree',
    method: 'GET',
    cacheTime: CACHE_24H,
    forceUpdate
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

const CACHE_2MIN = 2 * 60 * 1000

/**
 * 我的收藏 ID 列表（缓存 2 分钟，切页回来不重复请求）
 * @param {boolean} forceUpdate 强制刷新
 */
export const getMyFavoriteModelIdsApi = (forceUpdate = false) => {
  return cachedRequest({
    url: '/api/model/favorite/my/ids',
    method: 'GET',
    cacheTime: CACHE_2MIN,
    forceUpdate
  })
}

/**
 * 语义搜索模型（基于 AI 向量的智能搜索）
 * @param {Object} data 请求参数
 * @param {string} data.query 搜索查询文本
 * @param {number} data.pageNum 页码
 * @param {number} data.pageSize 每页数量
 */
export const semanticSearchModelsApi = (data) => {
  return request({
    url: '/api/semantic-search/models',
    method: 'GET',
    data
  })
}

/**
 * 获取语义搜索索引状态
 */
export const getSemanticSearchStatusApi = () => {
  return request({
    url: '/api/semantic-search/status',
    method: 'GET'
  })
}

/**
 * 设计者创建模型
 */
export const createModelApi = (data) => {
  return request({
    url: '/api/model/create',
    method: 'POST',
    data
  })
}

/**
 * 设计者更新模型
 */
export const updateModelApi = (data) => {
  return request({
    url: '/api/model/update',
    method: 'POST',
    data
  })
}

/**
 * 设计者查询自己的模型列表
 */
export const getMyModelsApi = (data) => {
  return request({
    url: '/api/model/my/list',
    method: 'POST',
    data
  })
}
