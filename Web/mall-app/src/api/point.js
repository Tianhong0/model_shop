import request from '../utils/request'

export const getPointAccountApi = () => {
  return request({
    url: '/api/points/account',
    method: 'GET'
  })
}

export const getPointLedgerPageApi = (data = {}) => {
  return request({
    url: '/api/points/ledger/page',
    method: 'POST',
    data
  })
}
