import request from '@/utils/request'

// 查询人脸注册列表
export function listFace(query) {
  return request({
    url: '/intellect/face/list',
    method: 'get',
    params: query
  })
}

// 查询人脸注册详细
export function getFace(id) {
  return request({
    url: '/intellect/face/' + id,
    method: 'get'
  })
}

// 新增人脸注册
export function addFace(data) {
  return request({
    url: '/intellect/face',
    method: 'post',
    data: data
  })
}

// 修改人脸注册
export function updateFace(data) {
  return request({
    url: '/intellect/face',
    method: 'put',
    data: data
  })
}

// 删除人脸注册
export function delFace(id) {
  return request({
    url: '/intellect/face/' + id,
    method: 'delete'
  })
}
