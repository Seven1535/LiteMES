// 用户状态：Token、用户信息、登录/退出
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, setToken, setStoredUser, clearAuth, getStoredUser } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: getStoredUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.user?.username || '',
    role: (state) => state.user?.role || ''
  },
  actions: {
    // 登录：成功后保存 token 与用户信息
    async login(loginForm) {
      const data = await loginApi(loginForm)
      this.token = data.token
      this.user = data.user
      setToken(data.token)
      setStoredUser(data.user)
    },
    // 登出：先调后端拉黑 Token（网关立即强制下线），失败也清本地（如 Token 已过期）
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 忽略：拉黑失败不影响本地清理
      }
      this.token = ''
      this.user = null
      clearAuth()
    }
  }
})
