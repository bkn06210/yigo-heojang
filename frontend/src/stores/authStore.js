import { defineStore } from 'pinia'
import { ref } from 'vue'

const readStoredUser = () => {
  const savedUser = localStorage.getItem('user')
  if (!savedUser) return null

  try {
    return JSON.parse(savedUser)
  } catch (error) {
    console.error('저장된 사용자 정보 파싱 실패:', error)
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(readStoredUser())

  const setAccessToken = (newAccessToken) => {
    token.value = newAccessToken
    localStorage.setItem('token', newAccessToken)
  }

  const updateUser = (updatedUser) => {
    user.value = { ...(user.value ?? {}), ...updatedUser }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const setLogin = (loginToken, userInfo) => {
    setAccessToken(loginToken)
    updateUser(userInfo)
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const isLogin = () => Boolean(token.value)

  return {
    token,
    user,
    setLogin,
    setAccessToken,
    updateUser,
    logout,
    isLogin,
  }
})
