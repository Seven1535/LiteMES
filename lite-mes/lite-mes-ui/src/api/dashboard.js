// 生产看板接口（litemes-production）
import request from './request'

// 看板概览统计（工单/任务/设备状态数量）
export function getOverview() {
  return request.get('/v1/prod/dashboard/overview')
}

// 设备实时状态（配合 WebSocket WORKCENTER_STATUS_CHANGED 刷新）
export function listWorkCenterStatus() {
  return request.get('/v1/prod/dashboard/workcenter-status')
}

// 今日报工趋势
export function getTodayReportTrend() {
  return request.get('/v1/prod/dashboard/today-report-trend')
}
