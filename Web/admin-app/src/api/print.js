import request from '../utils/request'

export const getPrintJobPage = (data) => {
  return request({
    url: '/api/print/admin/jobs',
    method: 'post',
    data
  })
}

export const getPrinterList = (params) => {
  return request({
    url: '/api/print/admin/printers',
    method: 'get',
    params
  })
}

export const createPrinter = (data) => {
  return request({
    url: '/api/print/admin/printers',
    method: 'post',
    data
  })
}

export const updatePrinter = (data) => {
  return request({
    url: '/api/print/admin/printers',
    method: 'put',
    data
  })
}

export const deletePrinter = (id) => {
  return request({
    url: `/api/print/admin/printers/${id}`,
    method: 'delete'
  })
}

export const dispatchPrintJob = (data) => {
  return request({
    url: '/api/print/admin/dispatch',
    method: 'post',
    data
  })
}

export const adjustPrintJob = (data) => {
  return request({
    url: '/api/print/admin/adjust',
    method: 'put',
    data
  })
}

export const stopPrintJob = (jobId) => {
  return request({
    url: `/api/print/admin/stop/${jobId}`,
    method: 'post'
  })
}

export const retryPrintJob = (jobId) => {
  return request({
    url: `/api/print/admin/retry/${jobId}`,
    method: 'post'
  })
}

export const getPrintJobEvents = (jobId, limit = 100) => {
  return request({
    url: `/api/print/admin/jobs/${jobId}/events`,
    method: 'get',
    params: { limit }
  })
}

export const deletePrintJob = (jobId) => {
  return request({
    url: `/api/print/admin/jobs/${jobId}`,
    method: 'delete'
  })
}
