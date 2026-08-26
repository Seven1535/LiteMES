// Axios 统一封装：请求带 Token，响应统一解析 AjaxResult
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

// 请求拦截：附加 Token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：统一解析 AjaxResult{code,message,data}
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非标准 AjaxResult（如文件流）直接返回
    if (res === null || typeof res !== 'object' || res.code === undefined) return res
    if (res.code === 200) return res.data
    if (res.code === 401) {
      clearAuth()
      ElMessage.error(res.message || '登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    const status = error.response?.status
    const message = status === 404 ? '接口不存在' : error.response?.data?.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
