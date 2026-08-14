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

// ── 간편비밀번호 ──────────────────────────────────────────────────────────
// 설정·변경은 로그인 상태만으로는 안 되고, 아래 두 단계를 먼저 거쳐야 한다.
//   1) sendSimplePasswordCode()   로그인한 회원의 이메일로 6자리 코드 발송
//   2) verifySimplePasswordCode() 코드가 맞으면 일회용 변경 토큰 발급
//   3) updateSimplePassword()     그 토큰과 함께 6자리 간편비밀번호 저장

// 서버는 이메일 주소를 받지 않는다. 로그인한 회원의 주소로만 보내며,
// 응답에는 마스킹된 이메일(us***@example.com)과 유효시간(초)만 담긴다.
export const sendSimplePasswordCode = () =>
  api.post('/api/members/me/simple-password/email-verifications').then(data)

export const verifySimplePasswordCode = (verificationCode) =>
  api
    .post('/api/members/me/simple-password/email-verifications/verify', {
      verificationCode,
    })
    .then(data)

// 최초 설정과 변경 모두 이 API를 쓴다.
export const updateSimplePassword = ({
  simplePasswordChangeToken,
  simplePassword,
  simplePasswordConfirm,
}) =>
  api
    .put('/api/members/me/simple-password', {
      simplePasswordChangeToken,
      simplePassword,
      simplePasswordConfirm,
    })
    .then(data)

// 결제 등에서 본인 확인용으로 호출한다. { matched: true|false }를 돌려준다.
// 5회 연속 틀리면 서버가 5분간 잠그고 429로 응답한다.
export const verifySimplePassword = (simplePassword) =>
  api
    .post('/api/members/me/simple-password/verifications', { simplePassword })
    .then(data)

// ── 회원 탈퇴 ─────────────────────────────────────────────────────────────

// 탈퇴 화면에 진입할 때 호출한다. 고지 문구(content)와 termsVersionId를 받아오고,
// 사용자가 동의하면 그 termsVersionId를 아래 withdrawMember로 되돌려 보낸다.
// 서버는 그 값이 지금도 유효한 탈퇴 약관 버전인지 다시 검증한다.
export const getWithdrawalTerms = () =>
  api.get('/api/members/me/withdrawal-terms').then(data)

export const verifyMemberPassword = (password) =>
  api.post('/api/members/me/password/verifications', { password }).then(data)

// 회원 탈퇴. DELETE지만 비밀번호·사유·약관 동의를 함께 보내야 해서 body를 싣는다.
// axios는 delete에서 두 번째 인자가 config이므로 { data: ... } 형태로 넘겨야 한다.
export const withdrawMember = ({
  password,
  reasonType,
  reasonDetail,
  termVersionId,
}) =>
  api
    .delete('/api/members/me', {
      data: { password, reasonType, reasonDetail, termVersionId },
    })
    .then(data)
