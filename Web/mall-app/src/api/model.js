import request, { cachedRequest } from '../utils/request'

// 缓存时间常量
const CACHE_30MIN = 30 * 60 * 1000
const CACHE_24H = 24 * 60 * 60 * 1000

/**
 * 模型列表（缓存 30 分钟）
 * @param {Object}  data        请求参数
 * @param {boolean} forceUpdate 强制刷新（下拉刷新时传 true）
 */
export const getModelListApi = (data, forceUpdate = false) => {
  return cachedRequest({
    url: '/api/model/list',
    method: 'POST',
    data,
    cacheTime: CACHE_30MIN,
    forceUpdate
  })
}

/**
 * 模型详情（缓存 30 分钟）
 * @param {string}  id          模型 ID
 * @param {boolean} forceUpdate 强制刷新
 */
export const getModelDetailApi = (id, forceUpdate = false) => {
  return cachedRequest({
    url: `/api/model/detail/${id}`,
    method: 'GET',
    cacheTime: CACHE_30MIN,
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
