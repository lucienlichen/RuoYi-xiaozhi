import request from '@/utils/request'

/** 获取分类树 */
export function getHazardSourceTree() {
  return request({ url: '/intellect/hazard-source/tree', method: 'get' })
}

/** 获取指定分类下的危险源条目 */
export function getHazardSourceItems(categoryId) {
  return request({ url: '/intellect/hazard-source/items', method: 'get', params: { categoryId } })
}

/** 获取单条危险源详情 */
export function getHazardSourceItem(id) {
  return request({ url: '/intellect/hazard-source/item/' + id, method: 'get' })
}

/** 批量获取原因文字 */
export function getHazardSourceCauses(codes) {
  return request({ url: '/intellect/hazard-source/causes', method: 'get', params: { codes } })
}

/** 批量获取事件信息 */
export function getHazardSourceEvents(codes) {
  return request({ url: '/intellect/hazard-source/events', method: 'get', params: { codes } })
}

/** 管理端：条目列表（分页） */
export function listHazardSourceItems(query) {
  return request({ url: '/intellect/hazard-source/items/list', method: 'get', params: query })
}

export function addHazardSourceItem(data) {
  return request({ url: '/intellect/hazard-source/item', method: 'post', data })
}

export function updateHazardSourceItem(data) {
  return request({ url: '/intellect/hazard-source/item', method: 'put', data })
}

export function deleteHazardSourceItem(ids) {
  return request({ url: '/intellect/hazard-source/item/' + ids, method: 'delete' })
}
