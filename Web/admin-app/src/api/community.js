import request from '../utils/request'

export const getAdminPostPage = (data) => {
  return request({
    url: '/api/community/admin/post/page',
    method: 'post',
    data
  })
}

export const getAdminPostDetail = (postId) => {
  return request({
    url: `/api/community/admin/post/detail/${postId}`,
    method: 'get'
  })
}

export const updatePostStatus = (data) => {
  return request({
    url: '/api/community/admin/post/status',
    method: 'post',
    data
  })
}

export const updatePostTop = (data) => {
  return request({
    url: '/api/community/admin/post/top',
    method: 'post',
    data
  })
}

export const updateAdminPostCategory = (data) => {
  return request({
    url: '/api/community/admin/post/category',
    method: 'post',
    data
  })
}

export const deleteAdminPost = (postId) => {
  return request({
    url: `/api/community/admin/post/delete/${postId}`,
    method: 'post'
  })
}

export const getAdminReplyPage = (data) => {
  return request({
    url: '/api/community/admin/reply/page',
    method: 'post',
    data
  })
}

export const deleteAdminReply = (replyId) => {
  return request({
    url: `/api/community/admin/reply/delete/${replyId}`,
    method: 'post'
  })
}

export const updateReplyStatus = (data) => {
  return request({
    url: '/api/community/admin/reply/status',
    method: 'post',
    data
  })
}

export const updateReplyExcellent = (data) => {
  return request({
    url: '/api/community/admin/reply/excellent',
    method: 'post',
    data
  })
}

export const getAdminCategoryList = () => {
  return request({
    url: '/api/community/admin/category/list',
    method: 'get'
  })
}

export const getAdminCategoryPage = (params) => {
  return request({
    url: '/api/community/admin/category/page',
    method: 'get',
    params
  })
}

export const createPostCategory = (data) => {
  return request({
    url: '/api/community/admin/category/create',
    method: 'post',
    data
  })
}

export const updatePostCategory = (data) => {
  return request({
    url: '/api/community/admin/category/update',
    method: 'post',
    data
  })
}

export const deletePostCategory = (categoryId) => {
  return request({
    url: `/api/community/admin/category/delete/${categoryId}`,
    method: 'post'
  })
}

export const uploadCommunityMedia = (formData) => {
  return request({
    url: '/api/community/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
