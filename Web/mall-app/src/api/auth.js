import request from '../utils/request'

export const loginApi = (data) => {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data
  })
}

export const registerApi = (data) => {
  return request({
    url: '/api/auth/register',
    method: 'POST',
    data
  })
}

export const sendRegisterEmailCodeApi = (data) => {
  return request({
    url: '/api/auth/email-code/register',
    method: 'POST',
    data
  })
}

export const sendForgotPasswordEmailCodeApi = (data) => {
  return request({
    url: '/api/auth/email-code/forgot-password',
    method: 'POST',
    data
  })
}

export const resetPasswordByEmailApi = (data) => {
  return request({
    url: '/api/auth/password/reset-by-email',
    method: 'POST',
    data
  })
}

export const logoutApi = () => {
  return request({
    url: '/api/auth/logout',
    method: 'POST'
  })
}
