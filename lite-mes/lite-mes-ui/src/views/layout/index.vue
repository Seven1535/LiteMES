<template>
  <!-- 主布局：左侧菜单 + 顶部导航 + 内容区 -->
  <el-container class="layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '210px'" class="layout-aside">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="layout-header" height="50px">
        <Navbar />
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.layout {
  height: 100%;
}

.layout-aside {
  background: #1d2733;
  transition: width 0.2s;
  overflow: hidden;
}

.layout-header {
  background: $card-bg;
  border-bottom: 1px solid $border-color;
  padding: 0 16px;
}

.layout-main {
  padding: 0;
  background: $page-bg;
  overflow: auto;
}
</style>
