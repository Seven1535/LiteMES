// 工艺路线接口（litemes-base，与设计规格 7.1 工艺路线模块对齐）
import request from './request'

// 产品的工艺路线版本列表（版本号倒序）
export function listWorkflows(productId) {
  return request.get(`/v1/base/products/${productId}/workflows`)
}

// 新建工艺路线版本（版本号自增，可选 copyFromVersion 复制）
export function createWorkflow(productId, data) {
  return request.post(`/v1/base/products/${productId}/workflows`, data)
}

// 工艺路线详情（含工序步骤和连线，编辑器渲染用）
export function getWorkflowDetail(id) {
  return request.get(`/v1/base/workflows/${id}`)
}

// 产品当前生效工艺版本（含工序与连线，工单创建时选择用）
export function getActiveWorkflow(productId) {
  return request.get(`/v1/base/workflows/products/${productId}/active`)
}

// 修改工艺路线元数据（仅草稿）
export function updateWorkflow(id, data) {
  return request.put(`/v1/base/workflows/${id}`, data)
}

// 批量保存工序（画布保存，全量覆盖）
export function saveSteps(id, steps) {
  return request.put(`/v1/base/workflows/${id}/steps`, { steps })
}

// 批量保存连线关系（画布保存，全量覆盖）
export function saveLinks(id, links) {
  return request.put(`/v1/base/workflows/${id}/links`, { links })
}

// 发布/激活版本（旧生效版本自动归档）
export function activateWorkflow(id) {
  return request.put(`/v1/base/workflows/${id}/activate`)
}

// 删除工艺版本（仅草稿）
export function deleteWorkflow(id) {
  return request.delete(`/v1/base/workflows/${id}`)
}
