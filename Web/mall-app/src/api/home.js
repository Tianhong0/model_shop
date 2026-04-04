import request, { cachedRequest } from '../utils/request'

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
