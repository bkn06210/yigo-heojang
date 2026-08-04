import api from './axios'


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