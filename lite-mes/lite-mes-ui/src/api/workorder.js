// 生产工单接口（litemes-production，与设计规格 7.1 生产工单模块对齐）
import request from './request'

// 工单分页查询（编号模糊 + 状态筛选）
export function listWorkOrders(params) {
  return request.get('/v1/prod/workorders', { params })
}

// 工单详情
export function getWorkOrder(id) {
  return request.get(`/v1/prod/workorders/${id}`)
}

// 创建工单（自动锁定产品生效工艺，编号由后端生成）
export function createWorkOrder(data) {
  return request.post('/v1/prod/workorders', data)
}

// 更新工单（仅已计划状态）
export function updateWorkOrder(id, data) {
  return request.put(`/v1/prod/workorders/${id}`, data)
}

// 下达工单（PLANNED → RELEASED）
export function releaseWorkOrder(id) {
  return request.put(`/v1/prod/workorders/${id}/release`)
}

// 关闭工单（终态，不可再操作）
export function closeWorkOrder(id) {
  return request.put(`/v1/prod/workorders/${id}/close`)
}

// 删除工单（仅已计划可删）
export function deleteWorkOrder(id) {
  return request.delete(`/v1/prod/workorders/${id}`)
}
