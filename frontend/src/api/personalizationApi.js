import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || '개인화 설정 요청에 실패했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

export const getPersonalization = () =>
  api.get('/api/personalization').then(data)

// [개인화 API 연동] category ID와 직접 입력한 브랜드 문자열을 개인화 테이블에 저장한다.
export const updatePersonalization = (payload) =>
  api.put('/api/personalization', payload).then(data)
// 07_25 연동 추가: 개인화 설정 조회·저장 백엔드 API를 프론트에 연결한다.
