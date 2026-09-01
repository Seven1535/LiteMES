<template>
  <!-- 派工工作台：选工单 → 查看任务列表 → 派工 / 开工 / 报工 -->
  <div class="page-container">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="工单">
          <el-select v-model="query.workOrderId" placeholder="请选择工单" filterable clearable
                     style="width: 260px" @change="handleSearch">
            <el-option v-for="o in dispatchableOrders" :key="o.id"
                       :label="`${o.orderNo} - ${o.productName || ''}`" :value="o.id" />
          </el-select>
        </el-form-item>
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

    <!-- 选中工单的概要信息 -->
    <div v-if="currentOrder" class="order-summary">
      <span class="summary-title">{{ currentOrder.orderNo }}</span>
      <span>产品：{{ currentOrder.productName || currentOrder.productId }}</span>
      <span>工艺：{{ currentOrder.workflowVersionName || '-' }}</span>
      <span>计划：{{ currentOrder.quantity }}</span>
      <span>已完工：{{ currentOrder.completedQty }}</span>
      <el-progress :percentage="orderProgress" :stroke-width="10" style="width: 180px" />
      <StatusTag :status="currentOrder.status" />
    </div>

    <!-- 任务列表 -->
    <div class="table-card">
      <div class="toolbar">
        <span class="toolbar-title">派工任务列表</span>
        <el-button type="primary" :disabled="!currentOrder || !canDispatch" @click="openDispatchDialog">
          派工
        </el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="taskNo" label="任务编号" width="170" />
        <el-table-column prop="stepName" label="工序" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.stepName || row.workflowStepId }}</template>
        </el-table-column>
        <el-table-column prop="workCenterName" label="工位" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.workCenterName || row.workCenterId }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作员" width="110">
          <template #default="{ row }">{{ row.operatorName || row.operatorId }}</template>
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
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" link size="small"
                       @click="handleStart(row)">开工</el-button>
            <el-button v-else-if="row.status === 'PROCESSING'" type="primary" link size="small"
                       @click="openReportDialog(row)">报工</el-button>
            <span v-else class="closed-text">-</span>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="query.pageNum" v-model:size="query.pageSize" :total="total"
                  @update:page="loadTasks" @update:size="loadTasks" />
    </div>

    <!-- 派工弹窗 -->
    <el-dialog v-model="dispatchVisible" title="派工" width="560px" :close-on-click-modal="false"
               @closed="resetDispatchForm">
      <el-form ref="dispatchFormRef" :model="dispatchForm" :rules="dispatchRules" label-width="100px">
        <el-form-item label="工序" prop="workflowStepId">
          <el-select v-model="dispatchForm.workflowStepId" placeholder="请选择工序" style="width: 100%">
            <el-option v-for="s in workflowSteps" :key="s.id"
                       :label="`${s.stepCode ? s.stepCode + ' - ' : ''}${s.stepName}`" :value="s.id" />
          </el-select>
          <div class="form-tip">同一工序可拆分派到不同工位，累计派工量不超过工单计划数量</div>
        </el-form-item>
        <el-form-item label="工位" prop="workCenterId">
          <el-select v-model="dispatchForm.workCenterId" placeholder="请选择工位" filterable style="width: 100%">
            <el-option v-for="w in workCenters" :key="w.id" :disabled="w.status === 'OFFLINE'"
                       :label="`${w.centerCode} - ${w.centerName}${w.status === 'BUSY' ? '（忙碌）' : ''}`"
                       :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作员" prop="operatorId">
          <el-select v-model="dispatchForm.operatorId" placeholder="请选择操作员" filterable style="width: 100%">
            <el-option v-for="u in operators" :key="u.id"
                       :label="`${u.realName || u.username}（${u.username}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="派工数量" prop="quantity">
          <el-input-number v-model="dispatchForm.quantity" :min="1" :max="currentOrder?.quantity || 100000"
                           style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dispatchForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleDispatch">确定</el-button>
      </template>
    </el-dialog>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDispatchTasks, createDispatchTask, startTask, reportTask } from '@/api/dispatch'
import { listWorkOrders } from '@/api/workorder'
import { getWorkflowDetail } from '@/api/process'
import { listWorkCenters } from '@/api/workcenter'
import { listEnabledUsers } from '@/api/user'

// ---------- 查询与列表 ----------
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, workOrderId: '', status: '' })

const dispatchableOrders = ref([])
const currentOrder = computed(() =>
  dispatchableOrders.value.find(o => o.id === query.workOrderId) || null)
const canDispatch = computed(() =>
  currentOrder.value && ['RELEASED', 'IN_PROGRESS'].includes(currentOrder.value.status))
const orderProgress = computed(() => currentOrder.value
  ? Math.min(100, Math.round(currentOrder.value.completedQty / currentOrder.value.quantity * 100))
  : 0)
const taskProgress = row => Math.min(100, Math.round(row.completedQty / row.quantity * 100))

async function loadOrders() {
  // 可派工工单 = 已下达 + 生产中（合并查询）
  const [released, inProgress] = await Promise.all([
    listWorkOrders({ pageNum: 1, pageSize: 100, status: 'RELEASED' }),
    listWorkOrders({ pageNum: 1, pageSize: 100, status: 'IN_PROGRESS' })
  ])
  dispatchableOrders.value = [...released.rows, ...inProgress.rows]
}

async function loadTasks() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.workOrderId) params.workOrderId = query.workOrderId
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
  loadTasks()
}

function handleReset() {
  query.workOrderId = ''
  query.status = ''
  handleSearch()
}

// ---------- 派工 ----------
const dispatchVisible = ref(false)
const submitting = ref(false)
const dispatchFormRef = ref(null)
const dispatchForm = reactive({ workflowStepId: '', workCenterId: '', operatorId: '', quantity: 1, remark: '' })
const dispatchRules = {
  workflowStepId: [{ required: true, message: '请选择工序', trigger: 'change' }],
  workCenterId: [{ required: true, message: '请选择工位', trigger: 'change' }],
  operatorId: [{ required: true, message: '请选择操作员', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入派工数量', trigger: 'blur' }]
}
const workflowSteps = ref([])
const workCenters = ref([])
const operators = ref([])

async function openDispatchDialog() {
  dispatchVisible.value = true
  // 工序来自工单锁定的工艺版本，工位/操作员下拉并行加载
  const [detail, centers, users] = await Promise.all([
    getWorkflowDetail(currentOrder.value.workflowId),
    listWorkCenters({ pageNum: 1, pageSize: 100 }),
    listEnabledUsers()
  ])
  workflowSteps.value = detail.steps || []
  workCenters.value = centers.rows
  operators.value = users
}

function resetDispatchForm() {
  dispatchForm.workflowStepId = ''
  dispatchForm.workCenterId = ''
  dispatchForm.operatorId = ''
  dispatchForm.quantity = 1
  dispatchForm.remark = ''
  dispatchFormRef.value?.clearValidate()
}

async function handleDispatch() {
  await dispatchFormRef.value.validate()
  submitting.value = true
  try {
    await createDispatchTask({ ...dispatchForm, workOrderId: query.workOrderId })
    ElMessage.success('派工成功')
    dispatchVisible.value = false
    await Promise.all([loadOrders(), loadTasks()])
  } finally {
    submitting.value = false
  }
}

// ---------- 开工 / 报工 ----------
async function handleStart(row) {
  await ElMessageBox.confirm(`确定开工任务 ${row.taskNo} 吗？`, '开工确认', { type: 'info' })
  await startTask(row.id)
  ElMessage.success('已开工')
  loadTasks()
}

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
    await Promise.all([loadOrders(), loadTasks()])
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadOrders()
  loadTasks()
})
</script>

<style scoped>
.order-summary {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: var(--el-color-primary-light-9, #ecf5ff);
  border-radius: 6px;
  font-size: 14px;
}

.summary-title {
  font-weight: 600;
}

.closed-text {
  color: var(--el-text-color-placeholder);
}
</style>
