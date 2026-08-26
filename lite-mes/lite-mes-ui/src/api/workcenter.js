// 设备管理接口（litemes-base）
import request from './request'

// 设备分页查询
export function listWorkCenters(params) {
  return request.get('/v1/base/workcenters', { params })
}

// 设备详情
export function getWorkCenter(id) {
  return request.get(`/v1/base/workcenters/${id}`)
}

// 新增设备
export function createWorkCenter(data) {
  return request.post('/v1/base/workcenters', data)
}

// 修改设备
export function updateWorkCenter(id, data) {
  return request.put(`/v1/base/workcenters/${id}`, data)
}

// 删除设备（后端校验是否有任务引用）
export function deleteWorkCenter(id) {
  return request.delete(`/v1/base/workcenters/${id}`)
}
