import request from '../utils/request'

/**
 * 角色管理相关API
 */

// 分页查询角色
export const getRolePage = (data) => {
  return request({
    url: '/api/roles/page',
    method: 'post',
    data
  })
}

// 获取所有启用的角色
export const getAllEnabledRoles = () => {
  return request({
    url: '/api/roles/enabled',
    method: 'get'
  })
}

// 获取角色详情
export const getRoleDetail = (id) => {
  return request({
    url: `/api/roles/${id}`,
    method: 'get'
  })
}

// 创建角色
export const createRole = (data) => {
  return request({
    url: '/api/roles',
    method: 'post',
    data
  })
}

// 更新角色
export const updateRole = (data) => {
  return request({
    url: '/api/roles',
    method: 'put',
    data
  })
}

// 删除角色
export const deleteRole = (id) => {
  return request({
    url: `/api/roles/${id}`,
    method: 'delete'
  })
}

// 获取所有权限（树形）
export const getAllPermissions = () => {
  return request({
    url: '/api/roles/permissions',
    method: 'get'
  })
}

// 获取角色的权限ID列表
export const getRolePermissionIds = (roleId) => {
  return request({
    url: `/api/roles/${roleId}/permissions`,
    method: 'get'
  })
}

// 获取用户的角色ID列表
export const getUserRoleIds = (userId) => {
  return request({
    url: `/api/roles/user/${userId}`,
    method: 'get'
  })
}

// 分配用户角色
export const assignUserRoles = (data) => {
  return request({
    url: '/api/roles/assign',
    method: 'post',
    data
  })
}
