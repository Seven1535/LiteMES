// 工艺路线接口（litemes-base）
import request from './request'

// 工艺路线分页查询
export function listWorkflows(params) {
  return request.get('/v1/base/workflows', { params })
}

// 产品当前生效工艺（含步骤，用于工单创建时选择）
export function getActiveWorkflow(productId) {
  return request.get(`/v1/base/workflows/${productId}/active`)
}

// 保存工艺路线（含步骤，全量保存）
export function saveWorkflow(data) {
  return request.post('/v1/base/workflows', data)
}

// 修改工艺路线（仅草稿可改）
export function updateWorkflow(id, data) {
  return request.put(`/v1/base/workflows/${id}`, data)
}

// 发布工艺路线（生效）
export function releaseWorkflow(id) {
  return request.post(`/v1/base/workflows/${id}/release`)
}

// 停用工艺路线
export function disableWorkflow(id) {
  return request.post(`/v1/base/workflows/${id}/disable`)
}
