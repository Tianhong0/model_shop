import request from '../utils/request'

export const getWalletAccountApi = () => {
  return request({
    url: '/api/wallet/account',
    method: 'GET'
  })
}

export const getWalletLedgerPageApi = (data = {}) => {
  return request({
    url: '/api/wallet/ledger/page',
    method: 'POST',
    data
  })
}

export const applyWalletWithdrawApi = (data) => {
  return request({
    url: '/api/wallet/withdraw/apply',
    method: 'POST',
    data
  })
}

export const createWalletRechargePayApi = (data) => {
  return request({
    url: '/api/wallet/recharge/app/create',
    method: 'POST',
    data
  })
}

export const syncWalletRechargeApi = (outTradeNo) => {
  return request({
    url: `/api/wallet/recharge/sync/${outTradeNo}`,
    method: 'POST',
  })
}

export const getMyWalletWithdrawPageApi = (data = {}) => {
  return request({
    url: '/api/wallet/withdraw/my/page',
    method: 'POST',
    data
  })
}
