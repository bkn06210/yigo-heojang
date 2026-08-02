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

// 현재 유효한 회원가입 약관 조회
export const getTerms = () => api.get('/api/terms')

// 회원가입 이메일 인증 코드 발송
export const sendSignupVerificationCode = (email) => (
  api.post('/api/auth/signup/email-verifications', { email })
)

// 회원가입 이메일 인증 코드 검증
export const verifySignupVerificationCode = (email, verificationCode) => (
  api.post('/api/auth/signup/email-verifications/verify', {
    email,
    verificationCode,
  })
)
