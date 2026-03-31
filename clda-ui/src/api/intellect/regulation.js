import request from '@/utils/request'

/** 查询法规列表（支持 category / title 过滤，分页） */
export function listRegulations(query) {
  return request({ url: '/intellect/regulation/list', method: 'get', params: query })
}

/** 查询法规详情（含 content_html） */
export function getRegulation(id) {
  return request({ url: '/intellect/regulation/' + id, method: 'get' })
}

export function addRegulation(data) {
  return request({ url: '/intellect/regulation', method: 'post', data })
}

export function updateRegulation(data) {
  return request({ url: '/intellect/regulation', method: 'put', data })
}

export function deleteRegulation(ids) {
  return request({ url: '/intellect/regulation/' + ids, method: 'delete' })
}
