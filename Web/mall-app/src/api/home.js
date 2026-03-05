import request from '../utils/request'

export const getHomeConfigApi = () => {
  return request({
    url: '/api/operation/home/config',
    method: 'GET'
  })
}

export const getHotModelsApi = (status) => {
  const payload = {
    pageNum: 1,
    pageSize: 6,
    orderBy: 'create_time'
  }

  if (status !== undefined) {
    payload.status = status
  }

  return request({
    url: '/api/model/list',
    method: 'POST',
    data: payload
  })
}
