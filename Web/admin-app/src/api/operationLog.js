import request from '../utils/request'

/**
 * 操作日志相关API
 */

// 分页查询操作日志
export const getOperationLogPage = (data) => {
  return request({
    url: '/api/operation-logs/page',
    method: 'post',
    data
  })
}

// 获取日志详情
export const getOperationLogDetail = (id) => {
  return request({
    url: `/api/operation-logs/${id}`,
    method: 'get'
  })
}

// 清理历史日志
export const cleanOldLogs = (days) => {
  return request({
    url: '/api/operation-logs/clean',
    method: 'delete',
    params: { days }
  })
}
