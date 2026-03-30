import request from '../utils/request'

/**
 * 活动列表(移动端)
 */
export const getEventListApi = (data) => {
  return request({
    url: '/api/event/list',
    method: 'POST',
    data
  })
}

/**
 * 活动详情
 */
export const getEventDetailApi = (id) => {
  return request({
    url: `/api/event/detail/${id}`,
    method: 'GET'
  })
}

/**
 * 活动报名
 */
export const signupEventApi = (id) => {
  return request({
    url: `/api/event/signup/${id}`,
    method: 'POST'
  })
}

/**
 * 取消报名
 */
export const cancelSignupApi = (id) => {
  return request({
    url: `/api/event/cancel/${id}`,
    method: 'POST'
  })
}

/**
 * 我参加的活动
 */
export const getMyParticipationsApi = (data) => {
  return request({
    url: '/api/event/my/participations',
    method: 'POST',
    data
  })
}

/**
 * 上传作品
 */
export const createSubmissionApi = (data) => {
  return request({
    url: '/api/event/submission/create',
    method: 'POST',
    data
  })
}

/**
 * 签到
 */
export const checkinEventApi = (id) => {
  return request({
    url: `/api/event/checkin/${id}`,
    method: 'POST'
  })
}

/**
 * 获取活动作品列表
 */
export const getEventSubmissionsApi = (eventId, data) => {
  return request({
    url: `/api/event/submissions/${eventId}`,
    method: 'POST',
    data
  })
}

/**
 * 获取我在某活动的作品
 */
export const getMySubmissionApi = (eventId) => {
  return request({
    url: `/api/event/my-submission/${eventId}`,
    method: 'GET'
  })
}

/**
 * 更新作品
 */
export const updateSubmissionApi = (submissionId, data) => {
  return request({
    url: `/api/event/submission/update/${submissionId}`,
    method: 'PUT',
    data
  })
}

/**
 * 获取作品详情
 */
export const getSubmissionDetailApi = (submissionId) => {
  return request({
    url: `/api/event/submission/${submissionId}`,
    method: 'GET'
  })
}

/**
 * 点赞作品
 */
export const likeSubmissionApi = (submissionId) => {
  return request({
    url: `/api/event/submission/like/${submissionId}`,
    method: 'POST'
  })
}

/**
 * 取消点赞作品
 */
export const unlikeSubmissionApi = (submissionId) => {
  return request({
    url: `/api/event/submission/like/${submissionId}`,
    method: 'DELETE'
  })
}

/**
 * 获取作品评论列表
 */
export const getSubmissionCommentsApi = (submissionId, data) => {
  return request({
    url: `/api/event/submission/comments/${submissionId}`,
    method: 'POST',
    data
  })
}

/**
 * 评论作品
 */
export const commentSubmissionApi = (data) => {
  return request({
    url: '/api/event/submission/comment',
    method: 'POST',
    data
  })
}

/**
 * 删除评论
 */
export const deleteCommentApi = (commentId) => {
  return request({
    url: `/api/event/submission/comment/${commentId}`,
    method: 'DELETE'
  })
}

/**
 * 点赞评论
 */
export const likeCommentApi = (commentId) => {
  return request({
    url: `/api/event/submission/comment/like/${commentId}`,
    method: 'POST'
  })
}

/**
 * 取消点赞评论
 */
export const unlikeCommentApi = (commentId) => {
  return request({
    url: `/api/event/submission/comment/like/${commentId}`,
    method: 'DELETE'
  })
}
