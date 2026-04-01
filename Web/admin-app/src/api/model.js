import request from '../utils/request'
import axios from 'axios'

/**
 * 模型管理相关API
 */

// 分页查询模型列表
export const getModelList = (data) => {
  return request({
    url: '/api/model/list',
    method: 'post',
    data
  })
}

// 根据ID查询模型详情
export const getModelDetail = (id) => {
  return request({
    url: `/api/model/detail/${id}`,
    method: 'get'
  })
}

// 查询模型分类列表
export const getModelCategories = (params) => {
  return request({
    url: '/api/model/categories',
    method: 'get',
    params
  })
}

// 递归查询完整的分类树
export const getCategoryTreeRecursive = (params) => {
  return request({
    url: '/api/model/categories/tree',
    method: 'get',
    params
  })
}

// 根据ID查询分类详情
export const getCategoryDetail = (id) => {
  return request({
    url: `/api/model/category/${id}`,
    method: 'get'
  })
}

// 创建模型
export const createModel = (data) => {
  return request({
    url: '/api/model/create',
    method: 'post',
    data
  })
}

// 更新模型
export const updateModel = (data) => {
  return request({
    url: '/api/model/update',
    method: 'post',
    data
  })
}

// 删除模型
export const deleteModel = (id) => {
  return request({
    url: `/api/model/delete/${id}`,
    method: 'delete'
  })
}

// 创建分类
export const createCategory = (data) => {
  return request({
    url: '/api/model/category/create',
    method: 'post',
    data
  })
}

// 更新分类
export const updateCategory = (data) => {
  return request({
    url: '/api/model/category/update',
    method: 'post',
    data
  })
}

// 删除分类
export const deleteCategory = (id) => {
  return request({
    url: `/api/model/category/delete/${id}`,
    method: 'delete'
  })
}

// ==================== 文件上传 ====================

// 上传文件
export const uploadFile = (file, type = 'modelImg') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)

  return request({
    url: '/api/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 带进度条的上传函数
export const uploadFileWithProgress = (file, type, onProgress) => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)

    // 使用request实例，这样会包含认证信息和拦截器
    request.post('/api/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total > 0) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    }).then(response => {
      // 兼容不同返回结构，优先返回URL字符串
      if (typeof response === 'string') {
        resolve(response)
        return
      }
      if (response && typeof response === 'object') {
        resolve(response.data || response.url || response.message || response)
        return
      }
      resolve(response)
    }).catch(error => {
      reject(error)
    })
  })
}

// ==================== 模型图片管理 ====================

// 添加模型图片
export const addModelImage = (modelId, imageUrl, isMain = 0, imgType = 1, sortOrder = 0) => {
  return request({
    url: '/api/model/image/add',
    method: 'post',
    data: { modelId, imageUrl, isMain, imgType, sortOrder }
  })
}

// 设置主图
export const setMainImage = (modelId, imageId) => {
  return request({
    url: '/api/model/image/set-main',
    method: 'post',
    data: { modelId, imageId }
  })
}

// 删除模型图片
export const deleteModelImage = (imageId) => {
  return request({
    url: `/api/model/image/delete/${imageId}`,
    method: 'delete'
  })
}

// 更新图片排序
export const updateImageSort = (imageId, sortOrder) => {
  return request({
    url: '/api/model/image/sort',
    method: 'post',
    data: { imageId, sortOrder }
  })
}

// ==================== 材质管理 ====================

// 添加材质
export const addMaterial = (modelId, materialData) => {
  return request({
    url: `/api/model/${modelId}/material/add`,
    method: 'post',
    data: materialData
  })
}

// 更新材质
export const updateMaterial = (modelId, materialId, materialData) => {
  return request({
    url: `/api/model/${modelId}/material/${materialId}/update`,
    method: 'post',
    data: materialData
  })
}

// 删除材质
export const deleteMaterial = (modelId, materialId) => {
  return request({
    url: `/api/model/${modelId}/material/${materialId}/delete`,
    method: 'delete'
  })
}

// 获取模型材质列表
export const getModelMaterials = (modelId) => {
  return request({
    url: `/api/model/${modelId}/materials`,
    method: 'get'
  })
}

// 导出模型数据
export const exportModels = (data) => {
  return request({
    url: '/api/model/export',
    method: 'post',
    data,
    responseType: 'blob'
  })
}

// ==================== 水印管理 ====================

// 为模型生成水印
export const generateWatermark = (modelId) => {
  return request({
    url: `/api/admin/watermark/generate/${modelId}`,
    method: 'post'
  })
}

// 重新生成水印
export const regenerateWatermark = (modelId) => {
  return request({
    url: `/api/admin/watermark/regenerate/${modelId}`,
    method: 'post'
  })
}

// 查询水印状态
export const getWatermarkStatus = (modelId) => {
  return request({
    url: `/api/admin/watermark/status/${modelId}`,
    method: 'get'
  })
}

// 批量生成水印
export const batchGenerateWatermark = (modelIds) => {
  return request({
    url: '/api/admin/watermark/batch-generate',
    method: 'post',
    data: { modelIds }
  })
}

// 删除水印
export const deleteWatermark = (modelId) => {
  return request({
    url: `/api/admin/watermark/delete/${modelId}`,
    method: 'delete'
  })
}
