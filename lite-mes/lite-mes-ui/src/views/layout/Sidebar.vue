<template>
  <!-- 侧边菜单：菜单项由路由表生成（meta.hidden 不显示，按 roles 过滤） -->
  <div class="sidebar">
    <div class="sidebar-logo">
      <span v-if="!appStore.sidebarCollapsed">LiteMES</span>
      <span v-else>LM</span>
    </div>
    <el-menu
      :default-active="route.path"
      :collapse="appStore.sidebarCollapsed"
      background-color="#1d2733"
      text-color="#a6adbb"
      active-text-color="#409eff"
      router
      class="sidebar-menu"
    >
      <el-menu-item v-for="item in menus" :key="item.path" :index="`/${item.path}`">
        <el-icon><component :is="item.meta.icon" /></el-icon>
        <template #title>{{ item.meta.title }}</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
// TODO: 业务开发（角色信息就绪后启用 roles 过滤）
// menus 过滤逻辑：!meta.hidden && meta.roles.includes(userStore.role)
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import router from '@/router'

const route = useRoute()
const appStore = useAppStore()

const menus = computed(() => {
  const root = router.options.routes.find((r) => r.path === '/')
  return (root?.children || []).filter((r) => !r.meta?.hidden)
})
</script>

<style scoped lang="scss">
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid #2c3a4d;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}
</style>
