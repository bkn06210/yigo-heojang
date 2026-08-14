import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || '챗봇 요청에 실패했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

// 챗봇 질의
// POST /api/chat
//
// 회원 id는 보내지 않는다. 서버가 토큰에서 꺼내 쓴다.
//
// pendingContext는 직전 답변이 되물으면서 내려준 맥락이다. 그대로 다시 실어 보내야
// 대화가 이어진다 — 내용을 해석하거나 고치지 않는다. 주제가 바뀌면 서버가 알아서 버린다.
//
// 답변 생성에 LLM 호출이 두 번 들어가 수 초 걸린다. 호출하는 쪽에서 로딩 상태를 잡아야 한다.
// axios 기본 timeout(30초)이 백엔드 RestTemplate readTimeout과 같으므로 따로 늘리지 않는다.
export const askChat = (question, pendingContext = null) => {

  const payload = { question }

  // null을 실어 보내면 서버가 "맥락 있음"으로 오해할 여지가 있어 있을 때만 넣는다.
  if (pendingContext) {
    payload.pendingContext = pendingContext
  }

  return api.post('/api/chat', payload).then(data)

}
