// frontend/src/api/axios.js

import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 5000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 모든 API 요청에 Access Token 자동 첨부
api.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('token')

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }

      return config
    },
    (error) => {
      return Promise.reject(error)
    }
)

let isRefreshing = false
let refreshQueue = []

const addRefreshQueue = (callback) => {
  refreshQueue.push(callback)
}

const runRefreshQueue = (newAccessToken) => {
  refreshQueue.forEach((callback) => callback(newAccessToken))
  refreshQueue = []
}

// 401 발생 시 Access Token 재발급 후 원래 요청 재시도
api.interceptors.response.use(
    (response) => response,

    async (error) => {
      const originalRequest = error.config
      const status = error.response?.status

      if (!originalRequest) {
        return Promise.reject(error)
      }

      const requestUrl = originalRequest.url || ''

      const isAuthRequest =
          requestUrl.includes('/api/auth/login') ||
          requestUrl.includes('/api/auth/token') ||
          requestUrl.includes('/api/auth/logout') ||
          requestUrl.includes('/api/auth/signup') ||
          requestUrl.includes('/api/terms')

      if (status !== 401 || originalRequest._retry || isAuthRequest) {
        return Promise.reject(error)
      }

      originalRequest._retry = true

      if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshQueue((newAccessToken) => {
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
            resolve(api(originalRequest))
          })
        })
      }

      isRefreshing = true

      try {
        const response = await api.post('/api/auth/token')

        const newAccessToken = response.data?.data?.accessToken

        if (!newAccessToken) {
          throw new Error('Access Token 재발급 응답이 비어 있습니다.')
        }

        const authStore = useAuthStore()
        authStore.setAccessToken(newAccessToken)

        runRefreshQueue(newAccessToken)

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`

        return api(originalRequest)
      } catch (refreshError) {
        const authStore = useAuthStore()
        authStore.logout()

        alert('로그인이 만료되었습니다. 다시 로그인해주세요.')
        window.location.href = '/auth/login'

        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
)

export default api
