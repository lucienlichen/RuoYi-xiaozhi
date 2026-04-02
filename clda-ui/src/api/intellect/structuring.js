import request from '@/utils/request'

export function listTemplates(query) {
  return request({ url: '/intellect/structuring/list', method: 'get', params: query })
}

export function getTemplate(id) {
  return request({ url: '/intellect/structuring/' + id, method: 'get' })
}

export function addTemplate(data) {
  return request({ url: '/intellect/structuring', method: 'post', data })
}

export function updateTemplate(data) {
  return request({ url: '/intellect/structuring', method: 'put', data })
}

export function delTemplate(id) {
  return request({ url: '/intellect/structuring/' + id, method: 'delete' })
}
