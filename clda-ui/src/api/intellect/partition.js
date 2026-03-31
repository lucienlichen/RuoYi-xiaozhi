import request from '@/utils/request'

export function listPartition(query) {
  return request({ url: '/intellect/partition/list', method: 'get', params: query })
}

export function getPartition(id) {
  return request({ url: '/intellect/partition/' + id, method: 'get' })
}

export function addPartition(data) {
  return request({ url: '/intellect/partition', method: 'post', data })
}

export function updatePartition(data) {
  return request({ url: '/intellect/partition', method: 'put', data })
}

export function delPartition(id) {
  return request({ url: '/intellect/partition/' + id, method: 'delete' })
}
