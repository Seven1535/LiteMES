<template>
  <!-- 生产工单：分页列表 + 创建弹窗，行操作按下达/关闭状态控制 -->
  <div class="page-container">
    <el-empty description="生产工单 - 功能开发中" />
  </div>
</template>

<script setup>
// TODO: 业务开发
// 1. 列表（listWorkOrders 分页查询）：状态列用 StatusTag，行内操作按状态控制（PLANNED 可下达，RELEASED/IN_PROGRESS 可关闭）
// 2. 创建工单（createWorkOrder）：选择产品 → 自动带出生效工艺路线 → 填数量
// 3. 下达（releaseWorkOrder）/ 关闭（closeWorkOrder）用 ConfirmButton 二次确认
// 4. 行点击跳转详情 /workorder/:id
</script>
