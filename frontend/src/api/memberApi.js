import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || '회원정보 요청에 실패했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

export const getMyInfo = () => api.get('/api/members/me').then(data)

export const updateMyInfo = (nickname) =>
  api.patch('/api/members/me', { nickname }).then(data)
// 07_25 연동 추가: 회원 정보 조회·수정 백엔드 API를 프론트에 연결한다.
