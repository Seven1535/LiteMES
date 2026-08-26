// 认证接口
import request from './request'

// 登录（网关白名单接口，无需 Token）
export function login(data) {
  return request.post('/v1/base/auth/login', data)
}

// 退出登录
export function logout() {
  return request.post('/v1/base/auth/logout')
}

// 修改密码
export function changePassword(data) {
  return request.post('/v1/base/auth/change-password', data)
}
