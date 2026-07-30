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