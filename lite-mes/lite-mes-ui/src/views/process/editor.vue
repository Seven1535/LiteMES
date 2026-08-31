<template>
  <!-- 工艺编辑器：左侧工序面板 + 中间 Vue Flow 画布 + 右侧属性面板 -->
  <div class="editor-container">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button text @click="router.push('/process')">← 返回</el-button>
        <span class="workflow-title">
          {{ workflow?.productName }} · {{ workflow?.versionName }}
          <el-tag size="small" :type="statusTag.type" style="margin-left: 8px">{{ statusTag.text }}</el-tag>
        </span>
      </div>
      <div class="toolbar-right" v-if="readonly === false">
        <el-button type="primary" :loading="saving" @click="handleSaveSteps">保存工序</el-button>
        <el-button type="primary" plain :loading="saving" @click="handleSaveLinks">保存连线</el-button>
        <el-button type="success" :disabled="nodes.length === 0" @click="handleActivate">激活版本</el-button>
      </div>
      <div class="toolbar-right" v-else>
        <el-tag type="info">已发布版本，只读（如需调整请新建版本）</el-tag>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左侧：工序面板 -->
      <div class="step-panel">
        <div class="panel-header">
          <span>工序列表（{{ nodes.length }}）</span>
          <el-button v-if="!readonly" type="primary" size="small" @click="addStep">+ 新增</el-button>
        </div>
        <div class="step-list">
          <div v-for="node in orderedNodes" :key="node.id" class="step-item"
               :class="{ active: node.selected }" @click="selectStep(node)">
            <div class="step-item-main">
              <span class="step-order">{{ node.data.step.stepOrder }}</span>
              <span class="step-name">{{ node.data.step.stepName }}</span>
            </div>
            <span class="step-code">{{ node.data.step.stepCode }}</span>
            <el-button v-if="!readonly" type="danger" link size="small" class="step-del"
                       @click.stop="removeStep(node)">删除</el-button>
          </div>
          <el-empty v-if="nodes.length === 0" description="暂无工序" :image-size="60" />
        </div>
      </div>

      <!-- 中间：Vue Flow 画布 -->
      <div class="flow-canvas">
        <VueFlow v-model:nodes="nodes" v-model:edges="edges" :nodes-draggable="!readonly"
                 :nodes-connectable="!readonly" :elements-selectable="true" fit-view-on-init>
          <Background />
        </VueFlow>
      </div>

      <!-- 右侧：属性面板 -->
      <div class="property-panel">
        <div class="panel-header"><span>工序属性</span></div>
        <template v-if="selectedNode">
          <el-form label-width="80px" size="small" class="property-form">
            <el-form-item label="编码">
              <el-input v-model="selectedNode.data.step.stepCode" :disabled="readonly" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="selectedNode.data.step.stepName" :disabled="readonly"
                        @change="syncNodeLabel(selectedNode)" />
            </el-form-item>
            <el-form-item label="顺序号">
              <el-input-number v-model="selectedNode.data.step.stepOrder" :min="1" :disabled="readonly" />
            </el-form-item>
            <el-form-item label="工位类型">
              <el-input v-model="selectedNode.data.step.requiredWorkCenterType" :disabled="readonly"
                        placeholder="如 车床" />
            </el-form-item>
            <el-form-item label="预估工时">
              <el-input-number v-model="selectedNode.data.step.estimatedMinutes" :min="0" :disabled="readonly" />
              <span class="unit">分钟</span>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="selectedNode.data.step.description" type="textarea" :rows="3"
                        :disabled="readonly" />
            </el-form-item>
          </el-form>
        </template>
        <el-empty v-else description="点击节点查看属性" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import { getWorkflowDetail, saveSteps, saveLinks, activateWorkflow } from '@/api/process'

const route = useRoute()
const router = useRouter()
const workflowId = route.params.workflowId

const workflow = ref(null)
const nodes = ref([])
const edges = ref([])
const saving = ref(false)
let stepSeq = 0 // 新增工序的顺序号基数

// 只读：非草稿版本禁止编辑
const readonly = computed(() => workflow.value?.status !== 'DRAFT')

const workflowStatusMap = {
  DRAFT: { text: '草稿', type: 'info' },
  ACTIVE: { text: '生效中', type: 'success' },
  ARCHIVED: { text: '已归档', type: 'warning' }
}
const statusTag = computed(() => workflowStatusMap[workflow.value?.status] || { text: '', type: 'info' })

const selectedNode = computed(() => nodes.value.find(n => n.selected))
const orderedNodes = computed(() =>
  [...nodes.value].sort((a, b) => (a.data.step.stepOrder || 0) - (b.data.step.stepOrder || 0)))

// 加载详情并渲染画布
async function loadDetail() {
  const detail = await getWorkflowDetail(workflowId)
  workflow.value = detail.workflow
  nodes.value = detail.steps.map(step => ({
    id: step.id,
    position: { x: step.posX || 0, y: step.posY || 0 },
    data: { label: `${step.stepName}（${step.stepCode}）`, step },
    style: { padding: '8px 14px', borderRadius: '6px' }
  }))
  edges.value = detail.links.map(link => ({
    id: link.id,
    source: link.sourceStepId,
    target: link.targetStepId,
    animated: true
  }))
  stepSeq = detail.steps.length
}

function syncNodeLabel(node) {
  node.data.label = `${node.data.step.stepName}（${node.data.step.stepCode}）`
}

function selectStep(node) {
  nodes.value.forEach(n => { n.selected = n.id === node.id })
}

// 新增工序：横向自动排布，先保存工序落库后才能对其连线
function addStep() {
  stepSeq += 1
  const step = {
    id: null,
    stepCode: `OP${String(stepSeq).padStart(2, '0')}`,
    stepName: '新工序',
    stepOrder: stepSeq,
    description: '',
    requiredWorkCenterType: '',
    estimatedMinutes: null,
    posX: 80 + ((stepSeq - 1) % 4) * 220,
    posY: 80 + Math.floor((stepSeq - 1) / 4) * 140
  }
  nodes.value.push({
    id: `temp-${stepSeq}-${Date.now()}`,
    position: { x: step.posX, y: step.posY },
    data: { label: `${step.stepName}（${step.stepCode}）`, step },
    selected: false,
    style: { padding: '8px 14px', borderRadius: '6px' }
  })
}

function removeStep(node) {
  nodes.value = nodes.value.filter(n => n.id !== node.id)
  edges.value = edges.value.filter(e => e.source !== node.id && e.target !== node.id)
}

// 保存工序（画布坐标一并提交；含未落库工序时连线暂存会丢失，提示先保存）
async function handleSaveSteps() {
  if (nodes.value.length === 0) {
    ElMessage.warning('请先新增工序')
    return
  }
  const codes = nodes.value.map(n => n.data.step.stepCode)
  if (new Set(codes).size !== codes.length) {
    ElMessage.warning('工序编码不能重复')
    return
  }
  saving.value = true
  try {
    const steps = nodes.value.map((n, index) => ({
      id: n.id.startsWith('temp-') ? null : n.id,
      stepCode: n.data.step.stepCode,
      stepName: n.data.step.stepName,
      stepOrder: n.data.step.stepOrder || index + 1,
      description: n.data.step.description,
      requiredWorkCenterType: n.data.step.requiredWorkCenterType,
      estimatedMinutes: n.data.step.estimatedMinutes,
      posX: n.position.x,
      posY: n.position.y
    }))
    await saveSteps(workflowId, steps)
    ElMessage.success('工序已保存')
    await loadDetail()
  } finally {
    saving.value = false
  }
}

// 保存连线（全量覆盖；存在未落库工序时禁止，避免端点 ID 无效）
async function handleSaveLinks() {
  const tempIds = new Set(nodes.value.filter(n => n.id.startsWith('temp-')).map(n => n.id))
  if (edges.value.some(e => tempIds.has(e.source) || tempIds.has(e.target))) {
    ElMessage.warning('存在未保存的新工序，请先点击"保存工序"后再保存连线')
    return
  }
  saving.value = true
  try {
    await saveLinks(workflowId, edges.value.map(e => ({ sourceStepId: e.source, targetStepId: e.target })))
    ElMessage.success('连线已保存')
    await loadDetail()
  } finally {
    saving.value = false
  }
}

async function handleActivate() {
  await ElMessageBox.confirm('确定激活该版本吗？当前生效版本将自动归档。', '激活确认', { type: 'warning' })
  await activateWorkflow(workflowId)
  ElMessage.success('激活成功')
  await loadDetail()
}

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.editor-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #ebeef5;

  .workflow-title {
    font-weight: 600;
  }
}

.editor-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.step-panel,
.property-panel {
  width: 240px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.property-panel {
  border-right: none;
  border-left: 1px solid #ebeef5;
  width: 280px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
}

.step-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.step-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  background: #f5f7fa;
  position: relative;

  &.active {
    background: #ecf5ff;
    outline: 1px solid var(--el-color-primary);
  }

  .step-item-main {
    display: flex;
    align-items: center;
    gap: 6px;

    .step-order {
      background: var(--el-color-primary);
      color: #fff;
      border-radius: 50%;
      width: 20px;
      height: 20px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
    }

    .step-name {
      font-weight: 500;
    }
  }

  .step-code {
    font-size: 12px;
    color: #909399;
    margin-left: 26px;
  }

  .step-del {
    position: absolute;
    right: 4px;
    top: 4px;
  }
}

.flow-canvas {
  flex: 1;
  min-width: 0;
}

.property-form {
  padding: 12px;

  .unit {
    margin-left: 6px;
    color: #909399;
  }
}
</style>
