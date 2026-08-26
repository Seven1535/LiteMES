// 生产工单接口（litemes-production）
import request from './request'

// 工单分页查询
export function listWorkOrders(params) {
  return request.get('/v1/prod/work-orders', { params })
}

// 工单详情
export function getWorkOrder(id) {
  return request.get(`/v1/prod/work-orders/${id}`)
}

// 创建工单（选择产品 + 工艺路线 + 数量）
export function createWorkOrder(data) {
  return request.post('/v1/prod/work-orders', data)
}

// 下达工单（生成派工任务，状态 PLANNED → RELEASED）
export function releaseWorkOrder(id) {
  return request.post(`/v1/prod/work-orders/${id}/release`)
}

// 关闭工单（连带关闭未完成任务，RELEASED/IN_PROGRESS → CLOSED）
export function closeWorkOrder(id) {
  return request.post(`/v1/prod/work-orders/${id}/close`)
}
