<template>
  <!-- 我的任务：操作员视角，查看自己的派工任务 + 开工 + 报工（设计规格 6.2） -->
  <div class="page-container">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待开始" value="PENDING" />
            <el-option label="加工中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 任务列表 -->
    <div class="table-card">
      <div class="toolbar">
        <span class="toolbar-title">我的任务</span>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无数据" height="100%">
          <el-table-column prop="taskNo" label="任务编号" width="170" />
          <el-table-column prop="workOrderNo" label="工单编号" width="170" />
          <el-table-column prop="stepName" label="工序" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.stepName || row.workflowStepId }}</template>
          </el-table-column>
          <el-table-column prop="workCenterName" label="工位" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.workCenterName || row.workCenterId }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="派工数量" width="90" align="center" />
          <el-table-column label="报工进度" width="150">
            <template #default="{ row }">
              <el-progress :percentage="taskProgress(row)" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="dispatchedAt" label="派工时间" width="165" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" type="success" link size="small"
                         @click="handleStart(row)">开工</el-button>
              <el-button v-else-if="row.status === 'PROCESSING'" type="primary" link size="small"
                         @click="openReportDialog(row)">报工</el-button>
              <span v-else class="closed-text">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <Pagination v-model:page="query.pageNum" v-model:size="query.pageSize" :total="total"
                  @update:page="loadData" @update:size="loadData" />
    </div>

    <!-- 报工弹窗 -->
    <el-dialog v-model="reportVisible" title="报工" width="420px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="任务">
          <span>{{ reportTarget?.taskNo }}（{{ reportTarget?.stepName || '工序' }}）</span>
        </el-form-item>
        <el-form-item label="剩余未完成">
          <span>{{ reportRemaining }} 件</span>
        </el-form-item>
        <el-form-item label="本次报工" required>
          <el-input-number v-model="reportQty" :min="1" :max="reportRemaining" style="width: 180px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReport">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDispatchTasks, startTask, reportTask } from '@/api/dispatch'
import { getStoredUser } from '@/utils/auth'
import { connectWebSocket, onEvent, offEvent } from '@/utils/websocket'
import { WS_EVENT, DEFAULT_PAGE_SIZE } from '@/utils/constants'

const loading = ref(false)
const submitting = ref(false)
const rows = ref([])
const total = ref(0)
const operatorId = getStoredUser()?.id || ''
const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, status: '' })

const taskProgress = row => Math.min(100, Math.round(row.completedQty / row.quantity * 100))

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize, operatorId }
    if (query.status) params.status = query.status
    const data = await listDispatchTasks(params)
    rows.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.status = ''
  handleSearch()
}

async function handleStart(row) {
  await ElMessageBox.confirm(`确定开工任务 ${row.taskNo} 吗？`, '开工确认', { type: 'info' })
  await startTask(row.id)
  ElMessage.success('已开工')
  loadData()
}

// ---------- 报工 ----------
const reportVisible = ref(false)
const reportTarget = ref(null)
const reportQty = ref(1)
const reportRemaining = computed(() => reportTarget.value
  ? reportTarget.value.quantity - reportTarget.value.completedQty : 0)

function openReportDialog(row) {
  reportTarget.value = row
  reportQty.value = Math.max(1, row.quantity - row.completedQty)
  reportVisible.value = true
}

async function handleReport() {
  if (!reportQty.value || reportQty.value < 1 || reportQty.value > reportRemaining.value) {
    ElMessage.warning('报工数量需在 1 与剩余未完成数量之间')
    return
  }
  submitting.value = true
  try {
    await reportTask(reportTarget.value.id, { quantity: reportQty.value })
    ElMessage.success('报工成功')
    reportVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---------- WebSocket：任务被完成/关闭时实时刷新 ----------
const refresh = () => loadData()

onMounted(() => {
  loadData()
  connectWebSocket()
  onEvent(WS_EVENT.TASK_COMPLETED, refresh)
  onEvent(WS_EVENT.TASK_CLOSED, refresh)
})

onUnmounted(() => {
  offEvent(WS_EVENT.TASK_COMPLETED, refresh)
  offEvent(WS_EVENT.TASK_CLOSED, refresh)
})
</script>

<style scoped>
.closed-text {
  color: var(--el-text-color-placeholder);
}
</style>
