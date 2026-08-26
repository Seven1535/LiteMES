// 用户状态：Token、用户信息、登录/退出
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
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
    logout() {
      this.token = ''
      this.user = null
      clearAuth()
    }
  }
})
