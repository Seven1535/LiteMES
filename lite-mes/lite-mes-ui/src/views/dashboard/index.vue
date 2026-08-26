<template>
  <!-- 生产看板：概览统计 + 设备实时状态 + 今日报工趋势 -->
  <div class="page-container">
    <el-empty description="生产看板 - 功能开发中" />
  </div>
</template>

<script setup>
// TODO: 业务开发
// 1. 概览统计卡片（api/dashboard.js getOverview）：工单/任务各状态数量
// 2. 设备实时状态面板（listWorkCenterStatus），订阅 WebSocket WORKCENTER_STATUS_CHANGED 实时刷新
// 3. 今日报工趋势（getTodayReportTrend）
</script>
