<template>
  <!-- 用户管理：分页列表 + 新增/编辑弹窗 + 重置密码（管理员功能） -->
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width: 200px"
                    @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 140px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="操作工" value="OPERATOR" />
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
        <span>用户列表</span>
        <el-button type="primary" @click="openDialog()">新增用户</el-button>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="rows" stripe height="100%">
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'primary' : 'info'" size="small">
                {{ row.role === 'ADMIN' ? '管理员' : '操作工' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openDialog(row)">编辑</el-button>
              <el-button type="warning" size="small" link @click="openResetDialog(row)">重置密码</el-button>
              <ConfirmButton link title="确定删除该用户吗？" @confirm="handleDelete(row)">删除</ConfirmButton>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <Pagination v-model:page="query.pageNum" v-model:size="query.pageSize"
                  :total="total" @update:page="loadData" @update:size="loadData" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="4-32 位" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" placeholder="不填默认 123456" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="操作工" value="OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" placeholder="6-32 位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DEFAULT_PAGE_SIZE } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'
import { listUsers, createUser, updateUser, deleteUser, resetPassword } from '@/api/user'

// 查询条件与列表数据
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, username: '', role: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await listUsers(query)
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
  query.username = ''
  query.role = ''
  handleSearch()
}

// 新增/编辑弹窗
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const emptyForm = { id: '', username: '', password: '', realName: '', role: 'OPERATOR', status: 'ENABLED' }
const form = reactive({ ...emptyForm })
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度为 4-32 位', trigger: 'blur' }
  ],
  password: [{ min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function openDialog(row) {
  Object.assign(form, emptyForm)
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, realName: row.realName, role: row.role, status: row.status })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form.id, { realName: form.realName, role: form.role, status: form.status })
      ElMessage.success('修改成功')
    } else {
      await createUser({ username: form.username, password: form.password || undefined, realName: form.realName, role: form.role })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

// 删除（ConfirmButton 已二次确认）
async function handleDelete(row) {
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  // 当前页删空后回退一页
  if (rows.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
  loadData()
}

// 重置密码弹窗
const resetVisible = ref(false)
const resetFormRef = ref()
const resetForm = reactive({ id: '', newPassword: '' })
const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ]
}

function openResetDialog(row) {
  resetForm.id = row.id
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function handleResetPassword() {
  await resetFormRef.value.validate()
  saving.value = true
  try {
    await resetPassword(resetForm.id, { newPassword: resetForm.newPassword })
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
