import request from '@/utils/request'

/** 获取全部检查项（业务端预览用） */
export function getInspectionItems() {
  return request({ url: '/intellect/inspection/items', method: 'get' })
}

/** 获取设备历史排查记录 */
export function getInspectionRecords(equipmentId) {
  return request({ url: '/intellect/inspection/records', method: 'get', params: { equipmentId } })
}

/** 获取单条记录详情（含结果明细） */
export function getInspectionRecordDetail(id) {
  return request({ url: '/intellect/inspection/record/' + id, method: 'get' })
}

/** 管理端：检查项列表（分页） */
export function listInspectionItems(query) {
  return request({ url: '/intellect/inspection/items/list', method: 'get', params: query })
}

export function addInspectionItem(data) {
  return request({ url: '/intellect/inspection/item', method: 'post', data })
}

export function updateInspectionItem(data) {
  return request({ url: '/intellect/inspection/item', method: 'put', data })
}

export function deleteInspectionItem(ids) {
  return request({ url: '/intellect/inspection/item/' + ids, method: 'delete' })
}
