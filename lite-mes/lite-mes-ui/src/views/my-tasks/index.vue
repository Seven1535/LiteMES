<template>
  <!-- 我的任务：操作工视角，开始任务 + 报工弹窗 -->
  <div class="page-container">
    <el-empty description="我的任务 - 功能开发中" />
  </div>
</template>

<script setup>
// TODO: 业务开发
// 1. 列表（listMyTasks 分页查询）
// 2. 开始任务（startTask）：PENDING → PROCESSING
// 3. 报工弹窗（reportTask）：合格数 + 备注；累计达到派工数量自动完成
// 4. 订阅 WebSocket TASK_COMPLETED / TASK_CLOSED 实时刷新
</script>
