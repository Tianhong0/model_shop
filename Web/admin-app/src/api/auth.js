import request from '../utils/request'

export const adminLoginApi = (data) => {
  return request({
    url: '/api/auth/admin/login',
    method: 'post',
    data
  })
}

export const sendAdminRegisterEmailCodeApi = (data) => {
  return request({
    url: '/api/auth/email-code/admin-register',
    method: 'post',
    data
  })
}

export const submitAdminRegisterRequestApi = (data) => {
  return request({
    url: '/api/auth/admin/register-request',
    method: 'post',
    data
  })
}

export const getAdminRegisterRequestsApi = (params) => {
  return request({
    url: '/api/auth/admin/register-requests',
    method: 'get',
    params
  })
}

export const reviewAdminRegisterRequestApi = (data) => {
  return request({
    url: '/api/auth/admin/register-request/review',
    method: 'put',
    data
  })
}

export const sendForgotPasswordEmailCodeApi = (data) => {
  return request({
    url: '/api/auth/email-code/forgot-password',
    method: 'post',
    data
  })
}

export const resetPasswordByEmailApi = (data) => {
  return request({
    url: '/api/auth/password/reset-by-email',
    method: 'post',
    data
  })
}
