import request from '@/utils/request'

export function listCategories() {
  return request({ url: '/intellect/equipdata/categories', method: 'get' })
}

export function listEquipData(query) {
  return request({ url: '/intellect/equipdata/list', method: 'get', params: query })
}

export function getDataFiles(dataId) {
  return request({ url: '/intellect/equipdata/files/' + dataId, method: 'get' })
}

export function getDataDates(query) {
  return request({ url: '/intellect/equipdata/dataDates', method: 'get', params: query })
}

export function uploadFiles(data) {
  return request({
    url: '/intellect/equipdata/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function delEquipData(id) {
  return request({ url: '/intellect/equipdata/' + id, method: 'delete' })
}

export function delDataFile(fileId) {
  return request({ url: '/intellect/equipdata/file/' + fileId, method: 'delete' })
}

/** 批量查询文件处理状态(轮询用) */
export function getFilesStatus(dataId) {
  return request({ url: '/intellect/equipdata/files/status', method: 'get', params: { dataId } })
}

/** 获取单个文件的结构化数据 */
export function getStructuredData(fileId) {
  return request({ url: '/intellect/equipdata/file/structured/' + fileId, method: 'get' })
}

/** 手动重新处理文件 */
export function reprocessFile(fileId) {
  return request({ url: '/intellect/equipdata/file/reprocess/' + fileId, method: 'post' })
}
