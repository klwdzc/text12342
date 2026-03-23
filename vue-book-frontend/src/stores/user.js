import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const userRole = ref(localStorage.getItem('userRole') || '') // 'admin', 'bookadmin', 'user'
  const userId = localStorage.getItem('userId') || ''

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  // 设置 token
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  // 设置角色
  const setUserRole = (role) => {
    userRole.value = role
    localStorage.setItem('userRole', role)
  }

  // 设置用户 ID
  const setUserId = (id) => {
    localStorage.setItem('userId', id)
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    userInfo.value = {}
    userRole.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userRole')
    localStorage.removeItem('userId')
  }

  return {
    token,
    userInfo,
    userRole,
    userId,
    setUserInfo,
    setToken,
    setUserRole,
    setUserId,
    logout,
  }
})
