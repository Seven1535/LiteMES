<template>
  <!-- 设备管理：分页列表 + 新增/编辑弹窗 + 删除（管理员功能） -->
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="工位编码">
          <el-input v-model="query.centerCode" placeholder="请输入编码" clearable style="width: 180px"
                    @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="工位名称">
          <el-input v-model="query.centerName" placeholder="请输入名称" clearable style="width: 180px"
                    @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="空闲" value="IDLE" />
            <el-option label="忙碌" value="BUSY" />
            <el-option label="离线" value="OFFLINE" />
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
        <span class="toolbar-title">设备列表</span>
        <el-button type="primary" @click="openDialog()">新增设备</el-button>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="rows" border stripe height="100%">
          <el-table-column prop="centerCode" label="工位编码" width="150" />
          <el-table-column prop="centerName" label="工位名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="centerType" label="类型" width="120" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
              <ConfirmButton title="确定删除该设备吗？" @confirm="handleDelete(row)">删除</ConfirmButton>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <Pagination v-model:page="query.pageNum" v-model:size="query.pageSize" :total="total"
                  @update:page="loadData" @update:size="loadData" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="520px"
               :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="工位编码" prop="centerCode">
          <el-input v-model="form.centerCode" placeholder="如 WC-001" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="工位名称" prop="centerName">
          <el-input v-model="form.centerName" placeholder="请输入工位名称" />
        </el-form-item>
        <el-form-item label="类型" prop="centerType">
          <el-input v-model="form.centerType" placeholder="如 车床、铣床、检验台" />
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
import { ElMessage } from 'element-plus'
import { listWorkCenters, createWorkCenter, updateWorkCenter, deleteWorkCenter } from '@/api/workcenter'
import { formatDateTime } from '@/utils/format'
import { DEFAULT_PAGE_SIZE } from '@/utils/constants'

// 查询条件
const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  centerCode: '',
  centerName: '',
  status: ''
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const data = await listWorkCenters(query)
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
  query.centerCode = ''
  query.centerName = ''
  query.status = ''
  handleSearch()
}

// 新增/编辑弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  centerCode: '',
  centerName: '',
  centerType: ''
})

const rules = {
  centerCode: [{ required: true, message: '请输入工位编码', trigger: 'blur' }],
  centerName: [{ required: true, message: '请输入工位名称', trigger: 'blur' }]
}

function openDialog(row) {
  if (row) {
    form.id = row.id
    form.centerCode = row.centerCode
    form.centerName = row.centerName
    form.centerType = row.centerType || ''
  }
  dialogVisible.value = true
}

function resetForm() {
  form.id = null
  form.centerCode = ''
  form.centerName = ''
  form.centerType = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateWorkCenter(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createWorkCenter(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await deleteWorkCenter(row.id)
  ElMessage.success('删除成功')
  // 删除后若当前页已空则回退一页
  if (rows.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.toolbar-title {
  font-weight: 600;
}
</style>
