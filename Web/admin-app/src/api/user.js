import request from '../utils/request'

/**
 * 用户管理相关API
 */

// 用户列表查询
export const getUserList = (params) => {
  return request({
    url: '/api/users/list',
    method: 'get',
    params
  })
}

// 用户详情查询
export const getUserDetail = (userId) => {
  return request({
    url: `/api/users/${userId}`,
    method: 'get'
  })
}

// 设计者列表查询
export const getDesignerList = () => {
  return request({
    url: '/api/users/designers',
    method: 'get'
  })
}

// 修改个人信息
export const updateUserProfile = (data) => {
  return request({
    url: '/api/users/profile',
    method: 'put',
    data
  })
}

// 修改密码
export const updatePassword = (data) => {
  return request({
    url: '/api/users/password',
    method: 'put',
    data
  })
}

export const sendChangePasswordEmailCode = () => {
  return request({
    url: '/api/users/password/email-code',
    method: 'post'
  })
}

export const sendChangeEmailCode = (email) => {
  return request({
    url: '/api/users/profile/email-code',
    method: 'post',
    data: { email }
  })
}

// 上传用户头像
export const uploadUserAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', 'avatar')
  return request({
    url: '/api/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 提交注销申请
export const submitDeletionRequest = (data) => {
  return request({
    url: '/api/users/deletion-request',
    method: 'post',
    data
  })
}

// 查看注销申请列表
export const getDeletionRequests = (params) => {
  return request({
    url: '/api/users/deletion-requests',
    method: 'get',
    params
  })
}

// 审批注销申请
export const approveDeletionRequest = (data) => {
  return request({
    url: '/api/users/deletion-approval',
    method: 'put',
    data
  })
}

export const getDesignerApplyRequests = (params) => {
  return request({
    url: '/api/users/designer-apply/requests',
    method: 'get',
    params
  })
}

export const reviewDesignerApplyRequest = (data) => {
  return request({
    url: '/api/users/designer-apply/review',
    method: 'put',
    data
  })
}

// 更新用户状态
export const updateUserStatus = (userId, status) => {
  return request({
    url: '/api/users/status',
    method: 'put',
    params: { userId, status }
  })
}
