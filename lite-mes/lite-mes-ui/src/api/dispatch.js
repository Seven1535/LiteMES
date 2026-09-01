// 派工任务接口（litemes-production，与设计规格 7.1 派工模块对齐）
import request from './request'

// 派工任务分页查询（工单/工位/操作员/状态筛选）
export function listDispatchTasks(params) {
  return request.get('/v1/prod/dispatch-tasks', { params })
}

// 任务详情
export function getDispatchTask(id) {
  return request.get(`/v1/prod/dispatch-tasks/${id}`)
}

// 派工（工序/工位/操作员必填，数量不超上限）
export function createDispatchTask(data) {
  return request.post('/v1/prod/dispatch-tasks', data)
}

// 开工（PENDING → PROCESSING，工位置 BUSY）
export function startTask(id) {
  return request.put(`/v1/prod/dispatch-tasks/${id}/start`)
}

// 报工（累计达到派工数量自动完成，同步更新工单已完工数量）
export function reportTask(id, data) {
  return request.put(`/v1/prod/dispatch-tasks/${id}/report`, data)
}
