import request from '@/utils/request'

export function listCategories() {
  return request({ url: '/intellect/equipdata/categories', method: 'get' })
}

export function listSubCategories(parentId) {
  return request({ url: '/intellect/equipdata/categories/' + parentId, method: 'get' })
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
