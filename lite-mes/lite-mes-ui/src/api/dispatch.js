// 派工任务接口（litemes-production）
import request from './request'

// 派工任务分页查询
export function listDispatchTasks(params) {
  return request.get('/v1/prod/dispatch-tasks', { params })
}

// 我的任务（当前登录人，操作工报工入口）
export function listMyTasks(params) {
  return request.get('/v1/prod/dispatch-tasks/my', { params })
}

// 任务详情
export function getDispatchTask(id) {
  return request.get(`/v1/prod/dispatch-tasks/${id}`)
}

// 开始任务（置设备运行中，PENDING → PROCESSING）
export function startTask(id) {
  return request.post(`/v1/prod/dispatch-tasks/${id}/start`)
}

// 任务报工（quantity=本次合格数；累计达到派工数量自动完成）
export function reportTask(id, data) {
  return request.post(`/v1/prod/dispatch-tasks/${id}/report`, data)
}
