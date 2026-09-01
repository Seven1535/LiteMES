// 用户管理接口（litemes-base）
import request from './request'

// 用户分页查询（支持 username 模糊 + role 精确筛选）
export function listUsers(params) {
  return request.get('/v1/base/users', { params })
}

// 启用状态用户简表（派工选操作员等下拉场景）
export function listEnabledUsers() {
  return request.get('/v1/base/users/list')
}

// 用户详情
export function getUser(id) {
  return request.get(`/v1/base/users/${id}`)
}

// 新增用户（password 不填后端默认 123456）
export function createUser(data) {
  return request.post('/v1/base/users', data)
}

// 修改用户（姓名/角色/状态，用户名不可改）
export function updateUser(id, data) {
  return request.put(`/v1/base/users/${id}`, data)
}

// 删除用户（逻辑删除）
export function deleteUser(id) {
  return request.delete(`/v1/base/users/${id}`)
}

// 重置密码
export function resetPassword(id, data) {
  return request.post(`/v1/base/users/${id}/reset-password`, data)
}
