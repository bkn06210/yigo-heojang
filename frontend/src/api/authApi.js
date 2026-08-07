import api from './axios'

export const getTerms = () => api.get('/api/terms')


// 회원가입 요청
// POST /api/auth/signup
// 전달 데이터: name, email, password
// passwordConfirm은 프론트 검증용이라 전달하지 않음
export const signup = (userData) => {

  return api.post(
    '/api/auth/signup',
    userData
  )

}


// 로그인 요청
// POST /api/auth/login
export const login = (loginData) => {

  return api.post(
    '/api/auth/login',
    loginData
  )

}

// 회원가입 이메일 인증 코드 요청
// POST /api/auth/signup/email-verifications
// 전달 데이터: email
export const sendSignupEmailVerification = (email) => {

  return api.post(
    '/api/auth/signup/email-verifications',
    {
      email
    }
  )

}


// 회원가입 이메일 인증 코드 검증
// POST /api/auth/signup/email-verifications/verify
// 전달 데이터: email, verificationCode
export const verifySignupEmailVerification = (
  email,
  verificationCode
) => {

  return api.post(
    '/api/auth/signup/email-verifications/verify',
    {
      email,
      verificationCode
    }
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

export const requestPasswordResetCode = (email) => (
  api.post('/api/auth/password/reset-link', { email })
)

export const verifyPasswordResetCode = (email, verificationCode) => (
  api.post('/api/auth/password/verify-code', { email, verificationCode })
)

export const resetPassword = (passwordResetToken, newPassword) => (
  api.post('/api/auth/password/resets', { passwordResetToken, newPassword })
)
