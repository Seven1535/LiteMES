<template>
  <!-- 产品管理：分页列表 + 新增/编辑弹窗 + 删除（管理员功能） -->
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入编码" clearable style="width: 180px"
                    @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="query.productName" placeholder="请输入名称" clearable style="width: 180px"
                    @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
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
        <span class="toolbar-title">产品列表</span>
        <el-button type="primary" @click="openDialog()">新增产品</el-button>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="rows" border stripe height="100%">
          <el-table-column prop="productCode" label="产品编码" width="160" />
          <el-table-column prop="productName" label="产品名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
              <ConfirmButton title="确定删除该产品吗？" @confirm="handleDelete(row)">删除</ConfirmButton>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <Pagination v-model:page="query.pageNum" v-model:size="query.pageSize" :total="total"
                  @update:page="loadData" @update:size="loadData" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑产品' : '新增产品'" width="520px"
               :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="如 P-001" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-form-item label="图纸地址" prop="drawingUrl">
          <el-input v-model="form.drawingUrl" placeholder="选填，图纸文件地址" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
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
import { listProducts, createProduct, updateProduct, deleteProduct } from '@/api/product'
import { formatDateTime } from '@/utils/format'
import { DEFAULT_PAGE_SIZE } from '@/utils/constants'

// 查询条件
const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  productCode: '',
  productName: '',
  status: ''
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const data = await listProducts(query)
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
  query.productCode = ''
  query.productName = ''
  query.status = ''
  handleSearch()
}

// 新增/编辑弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  productCode: '',
  productName: '',
  description: '',
  drawingUrl: '',
  status: 'ACTIVE'
})

const rules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function openDialog(row) {
  if (row) {
    form.id = row.id
    form.productCode = row.productCode
    form.productName = row.productName
    form.description = row.description || ''
    form.drawingUrl = row.drawingUrl || ''
    form.status = row.status
  }
  dialogVisible.value = true
}

function resetForm() {
  form.id = null
  form.productCode = ''
  form.productName = ''
  form.description = ''
  form.drawingUrl = ''
  form.status = 'ACTIVE'
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateProduct(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createProduct(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await deleteProduct(row.id)
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
