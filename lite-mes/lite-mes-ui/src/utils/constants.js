// 全局常量：状态枚举、颜色映射、分页默认值、WebSocket 事件
// 说明：状态中文名与颜色统一在此维护，页面一律通过 StatusTag 组件渲染，禁止散落硬编码

// 工单状态
export const ORDER_STATUS = {
  PLANNED: 'PLANNED',
  RELEASED: 'RELEASED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  CLOSED: 'CLOSED'
}

// 任务状态
export const TASK_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  CLOSED: 'CLOSED'
}

// 设备状态
export const WORKCENTER_STATUS = {
  IDLE: 'IDLE',
  RUNNING: 'RUNNING',
  FAULT: 'FAULT',
  MAINTENANCE: 'MAINTENANCE',
  OFFLINE: 'OFFLINE'
}

// 状态 → 文案 + el-tag 类型 统一映射
export const STATUS_MAP = {
  PLANNED: { text: '已计划', type: 'info' },
  RELEASED: { text: '已下达', type: 'primary' },
  IN_PROGRESS: { text: '生产中', type: 'warning' },
  COMPLETED: { text: '已完成', type: 'success' },
  CLOSED: { text: '已关闭', type: 'danger' },
  PENDING: { text: '待开始', type: 'info' },
  PROCESSING: { text: '加工中', type: 'warning' },
  IDLE: { text: '空闲', type: 'success' },
  RUNNING: { text: '运行中', type: 'warning' },
  FAULT: { text: '故障', type: 'danger' },
  MAINTENANCE: { text: '维护中', type: 'info' },
  OFFLINE: { text: '离线', type: 'info' },
  // 用户状态
  ENABLED: { text: '启用', type: 'success' },
  DISABLED: { text: '停用', type: 'danger' },
  // 产品状态
  ACTIVE: { text: '启用', type: 'success' },
  INACTIVE: { text: '停用', type: 'info' }
}

// 分页
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]
export const DEFAULT_PAGE_SIZE = 10

// WebSocket 事件类型（与后端 ProductionWebSocketHandler 推送一致）
export const WS_EVENT = {
  TASK_STARTED: 'TASK_STARTED',
  TASK_COMPLETED: 'TASK_COMPLETED',
  TASK_CLOSED: 'TASK_CLOSED',
  ORDER_STATUS_CHANGED: 'ORDER_STATUS_CHANGED',
  WORKCENTER_STATUS_CHANGED: 'WORKCENTER_STATUS_CHANGED'
}
