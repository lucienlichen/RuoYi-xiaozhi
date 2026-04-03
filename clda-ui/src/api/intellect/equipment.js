import request from '@/utils/request'

export function listEquipment(query) {
  return request({ url: '/intellect/equipment/list', method: 'get', params: query })
}

export function getEquipment(id) {
  return request({ url: '/intellect/equipment/' + id, method: 'get' })
}

export function addEquipment(data) {
  return request({ url: '/intellect/equipment', method: 'post', data })
}

export function updateEquipment(data) {
  return request({ url: '/intellect/equipment', method: 'put', data })
}

export function delEquipment(id) {
  return request({ url: '/intellect/equipment/' + id, method: 'delete' })
}
