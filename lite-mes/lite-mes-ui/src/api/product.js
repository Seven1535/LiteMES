// 产品管理接口（litemes-base）
import request from './request'

// 产品分页查询
export function listProducts(params) {
  return request.get('/v1/base/products', { params })
}

// 产品详情
export function getProduct(id) {
  return request.get(`/v1/base/products/${id}`)
}

// 新增产品
export function createProduct(data) {
  return request.post('/v1/base/products', data)
}

// 修改产品
export function updateProduct(id, data) {
  return request.put(`/v1/base/products/${id}`, data)
}

// 删除产品（后端校验引用，被引用时返回 400 提示）
export function deleteProduct(id) {
  return request.delete(`/v1/base/products/${id}`)
}
