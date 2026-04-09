import request from '../utils/request'

/**
 * 管理端活动列表
 */
export const getEventAdminList = (data) => {
  return request({
    url: '/api/event/admin/list',
    method: 'POST',
    data
  })
}

/**
 * 活动详情
 */
export const getEventDetail = (id) => {
  return request({
    url: `/api/event/admin/detail/${id}`,
    method: 'GET'
  })
}

/**
 * 创建活动
 */
export const createEvent = (data) => {
  return request({
    url: '/api/event/admin/create',
    method: 'POST',
    data
  })
}

/**
 * 更新活动
 */
export const updateEvent = (data) => {
  return request({
    url: '/api/event/admin/update',
    method: 'PUT',
    data
  })
}

/**
 * 更新活动状态
 */
export const updateEventStatus = (data) => {
  return request({
    url: '/api/event/admin/status',
    method: 'PUT',
    data
  })
}

/**
 * 删除活动
 */
export const deleteEvent = (id) => {
  return request({
    url: `/api/event/admin/delete/${id}`,
    method: 'DELETE'
  })
}

/**
 * 管理端获取活动作品列表
 */
export const getAdminSubmissions = (eventId, data) => {
  return request({
    url: `/api/event/admin/submissions/${eventId}`,
    method: 'POST',
    data
  })
}

/**
 * 管理端获取所有作品列表
 */
export const getAllAdminSubmissions = (data) => {
  return request({
    url: '/api/event/admin/submissions/all',
    method: 'POST',
    data
  })
}

/**
 * 审核作品
 */
export const reviewSubmission = (data) => {
  return request({
    url: '/api/event/admin/submission/review',
    method: 'POST',
    data
  })
}

/**
 * 管理端获取作品评论列表
 */
export const getAdminSubmissionComments = (submissionId, data) => {
  return request({
    url: `/api/event/admin/submission/comments/${submissionId}`,
    method: 'POST',
    data
  })
}

/**
 * 管理端删除评论
 */
export const adminDeleteComment = (commentId) => {
  return request({
    url: `/api/event/admin/submission/comment/${commentId}`,
    method: 'DELETE'
  })
}

/**
 * 管理端获取活动报名列表
 */
export const getAdminParticipations = (eventId, data) => {
  return request({
    url: `/api/event/admin/participations/${eventId}`,
    method: 'POST',
    data
  })
}

/**
 * 管理端获取所有报名列表
 */
export const getAllAdminParticipations = (data) => {
  return request({
    url: '/api/event/admin/participations/all',
    method: 'POST',
    data
  })
}

/**
 * 管理端更新参与状态
 */
export const updateParticipationStatus = (data) => {
  return request({
    url: '/api/event/admin/participation/status',
    method: 'PUT',
    data
  })
}

/**
 * 为指定获奖者颁发积分
 */
export const awardPoints = (data) => {
  return request({
    url: '/api/event/admin/award-points',
    method: 'POST',
    data
  })
}

/**
 * 批量发放活动所有获奖者积分
 */
export const awardAllWinners = (eventId) => {
  return request({
    url: `/api/event/admin/award-all/${eventId}`,
    method: 'POST'
  })
}
