<template>
  <!-- 工艺路线：按产品查看版本列表 + 新建/编辑/激活/删除 + 进入画布编辑器 -->
  <div class="page-container">
    <!-- 产品选择 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="产品">
          <el-select v-model="productId" placeholder="请选择产品" filterable style="width: 280px"
                     @change="loadWorkflows">
            <el-option v-for="p in products" :key="p.id" :label="`${p.productCode} - ${p.productName}`"
                       :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!productId" @click="openCreate">新建版本</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 版本列表 -->
    <div class="table-card">
      <div class="toolbar">
        <span class="toolbar-title">工艺版本列表</span>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="rows" border stripe height="100%">
          <el-table-column prop="version" label="版本号" width="90" align="center" />
          <el-table-column prop="versionName" label="版本标识" width="120" />
          <el-table-column prop="description" label="版本说明" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="workflowStatusMap[row.status]?.type || 'info'">
                {{ workflowStatusMap[row.status]?.text || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEditor(row)">编辑画布</el-button>
              <el-button v-if="row.status === 'DRAFT'" type="success" link size="small"
                         @click="handleActivate(row)">激活</el-button>
              <el-button v-if="row.status === 'DRAFT'" type="primary" link size="small"
                         @click="openEditMeta(row)">改信息</el-button>
              <ConfirmButton v-if="row.status === 'DRAFT'" title="确定删除该工艺版本吗？"
                             @confirm="handleDelete(row)">删除</ConfirmButton>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!productId" description="请先选择产品" :image-size="80" />
    </div>

    <!-- 新建版本弹窗 -->
    <el-dialog v-model="createVisible" title="新建工艺版本" width="480px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="版本标识">
          <el-input v-model="createForm.versionName" placeholder="留空自动生成，如 V2.0" />
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="复制自版本">
          <el-select v-model="createForm.copyFromVersion" placeholder="不复制，从空白开始" clearable
                     style="width: 100%">
            <el-option v-for="w in rows" :key="w.id" :label="`V${w.version}（${w.versionName}）`"
                       :value="w.version" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改元数据弹窗（仅草稿） -->
    <el-dialog v-model="metaVisible" title="修改版本信息" width="480px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="版本标识">
          <el-input v-model="metaForm.versionName" />
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="metaForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metaVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdateMeta">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProducts } from '@/api/product'
import {
  listWorkflows, createWorkflow, updateWorkflow, activateWorkflow, deleteWorkflow
} from '@/api/process'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

// 工艺版本状态文案（与产品状态区分：ACTIVE 在此语境为"生效"）
const workflowStatusMap = {
  DRAFT: { text: '草稿', type: 'info' },
  ACTIVE: { text: '生效中', type: 'success' },
  ARCHIVED: { text: '已归档', type: 'warning' }
}

const products = ref([])
const productId = ref('')
const rows = ref([])
const loading = ref(false)
const submitting = ref(false)

async function loadProducts() {
  const data = await listProducts({ pageNum: 1, pageSize: 100 })
  products.value = data.rows
}

async function loadWorkflows() {
  if (!productId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = await listWorkflows(productId.value)
  } finally {
    loading.value = false
  }
}

// 新建版本
const createVisible = ref(false)
const createForm = reactive({ versionName: '', description: '', copyFromVersion: null })

function openCreate() {
  createForm.versionName = ''
  createForm.description = ''
  createForm.copyFromVersion = null
  createVisible.value = true
}

async function handleCreate() {
  submitting.value = true
  try {
    await createWorkflow(productId.value, createForm)
    ElMessage.success('新建版本成功')
    createVisible.value = false
    loadWorkflows()
  } finally {
    submitting.value = false
  }
}

// 修改元数据
const metaVisible = ref(false)
const metaForm = reactive({ id: null, versionName: '', description: '' })

function openEditMeta(row) {
  metaForm.id = row.id
  metaForm.versionName = row.versionName
  metaForm.description = row.description || ''
  metaVisible.value = true
}

async function handleUpdateMeta() {
  submitting.value = true
  try {
    await updateWorkflow(metaForm.id, metaForm)
    ElMessage.success('修改成功')
    metaVisible.value = false
    loadWorkflows()
  } finally {
    submitting.value = false
  }
}

async function handleActivate(row) {
  await ElMessageBox.confirm(`确定激活版本 V${row.version} 吗？当前生效版本将自动归档。`, '激活确认', {
    type: 'warning'
  })
  await activateWorkflow(row.id)
  ElMessage.success('激活成功')
  loadWorkflows()
}

async function handleDelete(row) {
  await deleteWorkflow(row.id)
  ElMessage.success('删除成功')
  loadWorkflows()
}

function openEditor(row) {
  router.push(`/process/editor/${row.id}`)
}

onMounted(loadProducts)
</script>

<style scoped lang="scss">
.toolbar-title {
  font-weight: 600;
}
</style>
