import request, { cachedRequest } from '../utils/request'

const CACHE_30MIN = 30 * 60 * 1000
const CACHE_24H = 24 * 60 * 60 * 1000

/**
 * 首页运营配置（缓存 24 小时，Banner/公告很少变动）
 * @param {boolean} forceUpdate 强制刷新
 */
export const getHomeConfigApi = (forceUpdate = false) => {
  return cachedRequest({
    url: '/api/operation/home/config',
    method: 'GET',
    cacheTime: CACHE_24H,
    forceUpdate
  })
}

/**
 * 热门模型列表（缓存 30 分钟）
 * @param {number}  status      模型状态
 * @param {boolean} forceUpdate 强制刷新
 */
export const getHotModelsApi = (status, forceUpdate = false) => {
  const payload = {
    pageNum: 1,
    pageSize: 6,
    orderBy: 'create_time'
  }

  if (status !== undefined) {
    payload.status = status
  }

  return cachedRequest({
    url: '/api/model/list',
    method: 'POST',
    data: payload,
    cacheTime: CACHE_30MIN,
    forceUpdate
  })
}
