import request from '../utils/request'

/**
 * 系统配置相关API
 */

// 获取所有配置
export const getAllConfigs = () => {
  return request({
    url: '/api/configs',
    method: 'get'
  })
}

// 获取分组配置
export const getConfigsByGroup = (group) => {
  return request({
    url: `/api/configs/group/${group}`,
    method: 'get'
  })
}

// 获取公开配置
export const getPublicConfigs = () => {
  return request({
    url: '/api/configs/public',
    method: 'get'
  })
}

// 获取配置详情
export const getConfigDetail = (key) => {
  return request({
    url: `/api/configs/${key}`,
    method: 'get'
  })
}

// 设置配置
export const setConfig = (data) => {
  return request({
    url: '/api/configs',
    method: 'post',
    data
  })
}

// 批量设置配置
export const setConfigs = (data) => {
  return request({
    url: '/api/configs/batch',
    method: 'post',
    data
  })
}

// 删除配置
export const deleteConfig = (key) => {
  return request({
    url: `/api/configs/${key}`,
    method: 'delete'
  })
}
