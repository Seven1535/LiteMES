// 生产看板接口（litemes-production，与设计规格 7.1 看板模块对齐）
import request from './request'

// 概览数据（在制数/待下达数/完成数、工位状态统计、今日产出，后端 30s 缓存）
export function getDashboardSummary() {
  return request.get('/v1/prod/dashboard/summary')
}

// 在制工单进度列表
export function getWorkOrderProgress() {
  return request.get('/v1/prod/dashboard/workorders')
}

// 工位负载列表
export function getWorkCenterLoad() {
  return request.get('/v1/prod/dashboard/workcenters')
}
