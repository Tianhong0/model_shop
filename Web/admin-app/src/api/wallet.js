import request from '../utils/request'

export const getAdminWithdrawPage = (data = {}) => {
  return request({
    url: '/api/wallet/admin/withdraw/page',
    method: 'POST',
    data
  })
}

export const auditWithdraw = (data) => {
  return request({
    url: '/api/wallet/admin/withdraw/audit',
    method: 'POST',
    data
  })
}

export const payWithdraw = (data) => {
  return request({
    url: '/api/wallet/admin/withdraw/pay',
    method: 'POST',
    data
  })
}
