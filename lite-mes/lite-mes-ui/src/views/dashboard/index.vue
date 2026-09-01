<template>
  <!-- 生产看板：概览统计 + 在制工单进度 + 工位负载，WebSocket 实时刷新（设计规格 6.3.2） -->
  <div class="page-container dashboard">
    <!-- 标题栏 -->
    <div class="dashboard-header">
      <span class="dashboard-title">生产看板</span>
      <div class="dashboard-meta">
        <el-tag :type="online ? 'success' : 'danger'" size="small" effect="light">
          {{ online ? '实时更新中' : '连接断开，重连中…' }}
        </el-tag>
        <span class="dashboard-date">日期：{{ today }}</span>
        <el-button size="small" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- 概览统计卡片 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-card-title">工单概览</div>
        <div class="stat-card-body">
          <div class="stat-item">
            <span class="stat-value warning">{{ summary.orderOverview.inProgress }}</span>
            <span class="stat-label">生产中</span>
          </div>
          <div class="stat-item">
            <span class="stat-value info">{{ summary.orderOverview.pending }}</span>
            <span class="stat-label">待生产</span>
          </div>
          <div class="stat-item">
            <span class="stat-value success">{{ summary.orderOverview.completed }}</span>
            <span class="stat-label">已完成</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-title">工位状态</div>
        <div class="stat-card-body">
          <div class="stat-item">
            <span class="stat-value success">{{ summary.workCenterOverview.idle }}</span>
            <span class="stat-label">空闲</span>
          </div>
          <div class="stat-item">
            <span class="stat-value warning">{{ summary.workCenterOverview.busy }}</span>
            <span class="stat-label">忙碌</span>
          </div>
          <div class="stat-item">
            <span class="stat-value danger">{{ summary.workCenterOverview.offline }}</span>
            <span class="stat-label">离线</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-title">今日产出</div>
        <div class="stat-card-body">
          <div class="stat-item">
            <span class="stat-value success">{{ summary.todayOutput.completed }}</span>
            <span class="stat-label">完成</span>
          </div>
          <div class="stat-item">
            <span class="stat-value info">{{ summary.todayOutput.planned }}</span>
            <span class="stat-label">计划</span>
          </div>
          <div class="stat-item">
            <span class="stat-value primary">{{ summary.todayOutput.percentage }}%</span>
            <span class="stat-label">达成率</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 在制工单进度 -->
    <div class="panel">
      <div class="panel-title">在制工单进度</div>
      <el-empty v-if="!orders.length" description="暂无在制工单" :image-size="60" />
      <div v-for="o in orders" :key="o.id" class="progress-row">
        <span class="progress-order-no">{{ o.orderNo }}</span>
        <span class="progress-product">{{ o.productName || o.id }}</span>
        <el-progress :percentage="o.percentage" :stroke-width="12" class="progress-bar"
                     :status="o.percentage >= 100 ? 'success' : ''" />
        <span class="progress-qty">{{ o.completedQty }} / {{ o.quantity }}</span>
        <StatusTag :status="o.status" />
      </div>
    </div>

    <!-- 工位负载 -->
    <div class="panel">
      <div class="panel-title">工位负载</div>
      <el-empty v-if="!workCenters.length" description="暂无工位数据" :image-size="60" />
      <div v-for="w in workCenters" :key="w.id" class="load-row">
        <span class="load-center">{{ w.centerCode }} - {{ w.centerName }}</span>
        <StatusTag :status="w.status" />
        <template v-if="w.taskNo">
          <span class="load-task">
            {{ w.operatorName || '—' }}
            [{{ w.orderNo || '—' }} {{ w.stepName || '—' }}]
          </span>
          <span class="load-task-no">{{ w.taskNo }}</span>
        </template>
        <span v-else class="load-idle">空闲</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { getDashboardSummary, getWorkOrderProgress, getWorkCenterLoad } from '@/api/dashboard'
import {
  connectWebSocket, disconnectWebSocket, onEvent, offEvent,
  onReconnected, offReconnected, onStatusChange, offStatusChange
} from '@/utils/websocket'
import { WS_EVENT } from '@/utils/constants'

const today = new Date().toISOString().slice(0, 10)
const online = ref(false)
const summary = reactive({
  orderOverview: { inProgress: 0, pending: 0, completed: 0 },
  workCenterOverview: { idle: 0, busy: 0, offline: 0 },
  todayOutput: { completed: 0, planned: 0, percentage: 0 }
})
const orders = ref([])
const workCenters = ref([])

async function loadSummary() {
  Object.assign(summary, await getDashboardSummary())
}

async function loadOrders() {
  orders.value = await getWorkOrderProgress()
}

async function loadWorkCenters() {
  workCenters.value = await getWorkCenterLoad()
}

async function loadAll() {
  await Promise.all([loadSummary(), loadOrders(), loadWorkCenters()])
}

// ---------- WebSocket 事件 → 定向刷新 ----------
// 任务相关事件：刷新工位负载 + 工单进度 + 概览
const handleTaskEvent = () => {
  loadWorkCenters()
  loadOrders()
  loadSummary()
}
// 工位状态事件：只刷工位负载
const handleWorkCenterEvent = () => loadWorkCenters()
// 工单状态事件：刷工单进度 + 概览
const handleOrderEvent = () => {
  loadOrders()
  loadSummary()
}
// 断线重连成功：全量拉取补偿断线期间的变更（架构设计 3.6）
const handleReconnected = () => loadAll()
const handleStatus = (isOnline) => { online.value = isOnline }

onMounted(() => {
  loadAll()
  connectWebSocket()
  onEvent(WS_EVENT.TASK_STARTED, handleTaskEvent)
  onEvent(WS_EVENT.TASK_COMPLETED, handleTaskEvent)
  onEvent(WS_EVENT.TASK_CLOSED, handleTaskEvent)
  onEvent(WS_EVENT.ORDER_STATUS_CHANGED, handleOrderEvent)
  onEvent(WS_EVENT.WORKCENTER_STATUS_CHANGED, handleWorkCenterEvent)
  onReconnected(handleReconnected)
  onStatusChange(handleStatus)
})

onUnmounted(() => {
  offEvent(WS_EVENT.TASK_STARTED, handleTaskEvent)
  offEvent(WS_EVENT.TASK_COMPLETED, handleTaskEvent)
  offEvent(WS_EVENT.TASK_CLOSED, handleTaskEvent)
  offEvent(WS_EVENT.ORDER_STATUS_CHANGED, handleOrderEvent)
  offEvent(WS_EVENT.WORKCENTER_STATUS_CHANGED, handleWorkCenterEvent)
  offReconnected(handleReconnected)
  offStatusChange(handleStatus)
  disconnectWebSocket()
})
</script>

<style scoped>
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dashboard-title {
  font-size: 20px;
  font-weight: 600;
}

.dashboard-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dashboard-date {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.stat-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px 20px;
}

.stat-card-title {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.stat-card-body {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
}

.stat-value.primary { color: var(--el-color-primary); }
.stat-value.success { color: var(--el-color-success); }
.stat-value.warning { color: var(--el-color-warning); }
.stat-value.danger { color: var(--el-color-danger); }
.stat-value.info { color: var(--el-color-info); }

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.panel {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.progress-row:last-child {
  border-bottom: none;
}

.progress-order-no {
  width: 160px;
  font-weight: 600;
}

.progress-product {
  width: 160px;
  color: var(--el-text-color-secondary);
}

.progress-bar {
  flex: 1;
}

.progress-qty {
  width: 90px;
  text-align: right;
  color: var(--el-text-color-secondary);
}

.load-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.load-row:last-child {
  border-bottom: none;
}

.load-center {
  width: 200px;
  font-weight: 600;
}

.load-task {
  color: var(--el-text-color-regular);
}

.load-task-no {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.load-idle {
  color: var(--el-text-color-placeholder);
}
</style>
