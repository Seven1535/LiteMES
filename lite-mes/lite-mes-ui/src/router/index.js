// 路由表 + 导航守卫
// 说明：菜单项由本表生成（meta.hidden 不显示），角色过滤在 Sidebar 中处理
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '生产看板', icon: 'Odometer', roles: ['ADMIN', 'OPERATOR'] }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '产品管理', icon: 'Goods', roles: ['ADMIN'] }
      },
      {
        path: 'process',
        name: 'Process',
        component: () => import('@/views/process/index.vue'),
        meta: { title: '工艺路线', icon: 'Share', roles: ['ADMIN'] }
      },
      {
        path: 'process/editor/:workflowId',
        name: 'ProcessEditor',
        component: () => import('@/views/process/editor.vue'),
        meta: { title: '工艺编辑器', hidden: true, roles: ['ADMIN'] }
      },
      {
        path: 'workcenter',
        name: 'WorkCenter',
        component: () => import('@/views/workcenter/index.vue'),
        meta: { title: '设备管理', icon: 'Cpu', roles: ['ADMIN'] }
      },
      {
        path: 'workorder',
        name: 'WorkOrder',
        component: () => import('@/views/workorder/index.vue'),
        meta: { title: '生产工单', icon: 'Tickets', roles: ['ADMIN'] }
      },
      {
        path: 'workorder/:id',
        name: 'WorkOrderDetail',
        component: () => import('@/views/workorder/detail.vue'),
        meta: { title: '工单详情', hidden: true, roles: ['ADMIN'] }
      },
      {
        path: 'dispatch',
        name: 'Dispatch',
        component: () => import('@/views/dispatch/index.vue'),
        meta: { title: '派工任务', icon: 'List', roles: ['ADMIN'] }
      },
      {
        path: 'my-tasks',
        name: 'MyTasks',
        component: () => import('@/views/my-tasks/index.vue'),
        meta: { title: '我的任务', icon: 'User', roles: ['ADMIN', 'OPERATOR'] }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', icon: 'Setting', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳登录页，已登录访问登录页回看板
router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  next()
})

// 设置页面标题
router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - LiteMES` : 'LiteMES'
})

export default router
