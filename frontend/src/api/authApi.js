// frontend/src/api/authApi.js

import api from './axios'

// 회원가입 요청
export const signup = (userData) => {
  return api.post(
      '/api/auth/signup',
      userData
  )
}

// 로그인 요청
export const login = (loginData) => {
  return api.post(
      '/api/auth/login',
      loginData
  )
}

// Access Token 재발급 요청
export const reissueAccessToken = () => {
  return api.post('/api/auth/token')
}

// 로그아웃 요청
export const logout = () => {
  return api.post('/api/auth/logout')
}