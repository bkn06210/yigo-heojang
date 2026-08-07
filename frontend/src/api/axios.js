// frontend/src/api/axios.js

import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // 07_25 연동 수정: 배포 환경별 API 주소를 환경변수에서 읽는다.
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

const addRefreshQueue = (resolve, reject, originalRequest) => {
  refreshQueue.push({ resolve, reject, originalRequest }) // 07_25 연동 수정: 재발급 실패 시 대기 요청도 reject할 수 있게 보관한다.
}

const runRefreshQueue = (newAccessToken) => {
  refreshQueue.forEach(({ resolve, originalRequest }) => {
    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
    resolve(api(originalRequest))
  })
  refreshQueue = []
}

const rejectRefreshQueue = (error) => {
  refreshQueue.forEach(({ reject }) => reject(error)) // 07_25 연동 수정: 토큰 갱신 실패 시 pending 요청을 모두 종료한다.
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
          requestUrl.includes('/api/auth/logout')

      if (status !== 401 || originalRequest._retry || isAuthRequest) {
        return Promise.reject(error)
      }

      originalRequest._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          addRefreshQueue(resolve, reject, originalRequest) // 07_25 연동 수정: 성공·실패 콜백을 함께 큐에 등록한다.
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
        rejectRefreshQueue(refreshError) // 07_25 연동 수정: 재발급 실패로 멈춰 있던 API 요청을 모두 해제한다.
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
// 07_25 연동 변경: develop 프론트 대비 공통 API 주소·토큰·오류 처리를 보완한 파일이다.
