<template>
  <!-- 生产工单：分页列表 + 创建/编辑弹窗 + 下达/关闭/删除 -->
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="工单编号">
          <el-input v-model="query.orderNo" placeholder="请输入编号" clearable style="width: 200px"
                    @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="已计划" value="PLANNED" />
            <el-option label="已下达" value="RELEASED" />
            <el-option label="生产中" value="IN_PROGRESS" />
            <el-option label="已完工" value="COMPLETED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="toolbar">
        <span class="toolbar-title">工单列表</span>
        <el-button type="primary" @click="openDialog()">创建工单</el-button>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="rows" border stripe height="100%">
          <el-table-column prop="orderNo" label="工单编号" width="170" />
          <el-table-column label="产品" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.productCode ? `${row.productCode} - ${row.productName || ''}` : row.productId }}
            </template>
          </el-table-column>
          <el-table-column prop="workflowVersionName" label="工艺版本" width="100" />
          <el-table-column prop="quantity" label="计划数量" width="90" align="center" />
          <el-table-column label="完工进度" width="150">
            <template #default="{ row }">
              <el-progress :percentage="progressOf(row)" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="priorityMap[row.priority]?.type">
                {{ priorityMap[row.priority]?.text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="计划日期" width="190">
            <template #default="{ row }">
              {{ row.planStartDate || '-' }} ~ {{ row.planEndDate || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PLANNED'">
                <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
                <el-button type="success" link size="small" @click="handleRelease(row)">下达</el-button>
                <ConfirmButton title="确定删除该工单吗？" @confirm="handleDelete(row)">删除</ConfirmButton>
              </template>
              <ConfirmButton v-else-if="row.status !== 'CLOSED'" title="关闭后不可再操作，确定关闭吗？"
                             @confirm="handleClose(row)">关闭</ConfirmButton>
              <span v-else class="closed-text">已关闭</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <Pagination v-model:page="query.pageNum" v-model:size="query.pageSize" :total="total"
                  @update:page="loadData" @update:size="loadData" />
    </div>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑工单' : '创建工单'" width="560px"
               :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择产品" filterable :disabled="!!form.id"
                     style="width: 100%">
            <el-option v-for="p in activeProducts" :key="p.id"
                       :label="`${p.productCode} - ${p.productName}`" :value="p.id" />
          </el-select>
          <div v-if="!form.id" class="form-tip">创建时自动锁定该产品当前生效的工艺版本</div>
        </el-form-item>
        <el-form-item label="计划数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :max="100000" style="width: 180px" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio :value="1">紧急</el-radio>
            <el-radio :value="2">高</el-radio>
            <el-radio :value="3">中</el-radio>
            <el-radio :value="4">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="form.planStartDate" type="date" placeholder="开始日期"
                          value-format="YYYY-MM-DD" style="width: 170px" />
          <span style="margin: 0 8px">至</span>
          <el-date-picker v-model="form.planEndDate" type="date" placeholder="完成日期"
                          value-format="YYYY-MM-DD" style="width: 170px" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWorkOrders, createWorkOrder, updateWorkOrder, releaseWorkOrder, closeWorkOrder, deleteWorkOrder
} from '@/api/workorder'
import { listProducts } from '@/api/product'
import { DEFAULT_PAGE_SIZE } from '@/utils/constants'

// 优先级映射（设计规格：1=紧急, 2=高, 3=中, 4=低）
const priorityMap = {
  1: { text: '紧急', type: 'danger' },
  2: { text: '高', type: 'warning' },
  3: { text: '中', type: 'primary' },
  4: { text: '低', type: 'info' }
}

// 查询条件
const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, orderNo: '', status: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

function progressOf(row) {
  if (!row.quantity) return 0
  return Math.min(100, Math.round((row.completedQty || 0) / row.quantity * 100))
}

async function loadData() {
  loading.value = true
  try {
    const data = await listWorkOrders(query)
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
  query.orderNo = ''
  query.status = ''
  handleSearch()
}

// 创建/编辑弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const activeProducts = ref([])
const form = reactive({
  id: null,
  productId: '',
  quantity: 1,
  priority: 3,
  planStartDate: '',
  planEndDate: '',
  remark: ''
})

const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

async function loadProducts() {
  const data = await listProducts({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
  activeProducts.value = data.rows
}

function openDialog(row) {
  if (row) {
    form.id = row.id
    form.productId = row.productId
    form.quantity = row.quantity
    form.priority = row.priority
    form.planStartDate = row.planStartDate || ''
    form.planEndDate = row.planEndDate || ''
    form.remark = row.remark || ''
  }
  dialogVisible.value = true
}

function resetForm() {
  form.id = null
  form.productId = ''
  form.quantity = 1
  form.priority = 3
  form.planStartDate = ''
  form.planEndDate = ''
  form.remark = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value.validate()
  if (form.planStartDate && form.planEndDate && form.planEndDate < form.planStartDate) {
    ElMessage.warning('计划完成日期不能早于计划开始日期')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateWorkOrder(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createWorkOrder(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleRelease(row) {
  await ElMessageBox.confirm(`确定下达工单 ${row.orderNo} 吗？下达后不可再编辑。`, '下达确认', {
    type: 'warning'
  })
  await releaseWorkOrder(row.id)
  ElMessage.success('下达成功')
  loadData()
}

async function handleClose(row) {
  await closeWorkOrder(row.id)
  ElMessage.success('已关闭')
  loadData()
}

async function handleDelete(row) {
  await deleteWorkOrder(row.id)
  ElMessage.success('删除成功')
  if (rows.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  loadData()
}

onMounted(() => {
  loadData()
  loadProducts()
})
</script>

<style scoped lang="scss">
.toolbar-title {
  font-weight: 600;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}

.closed-text {
  color: #909399;
  font-size: 12px;
}
</style>
