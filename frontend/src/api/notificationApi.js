import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || '알림 설정 요청에 실패했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

// 알림 목록. page는 0부터, size는 서버가 최대 10까지만 허용한다(그보다 크면 400).
export const getNotifications = ({ page = 0, size = 10 } = {}) =>
  api.get('/api/notifications', { params: { page, size } }).then(data)

// 안 읽은 알림 수.
export const getUnreadNotificationCount = () =>
  api.get('/api/notifications/unread-count').then(data)

// 읽음 처리. 204 No Content라 응답 본문을 파싱하지 않는다.
// 이미 읽은 알림에 다시 호출해도 성공한다(서버가 멱등 처리).
export const markNotificationAsRead = (notificationId) =>
  api.patch(`/api/notifications/${notificationId}/read`).then(() => undefined)

// 삭제(소프트 삭제). 204 No Content.
export const deleteNotification = (notificationId) =>
  api.delete(`/api/notifications/${notificationId}`).then(() => undefined)

// PR #53 연동: 로그인 회원의 알림 수신 설정을 조회한다.
// 설정을 한 번도 바꾼 적 없는 회원도 오류가 아니라 기본값(둘 다 true)이 온다.
export const getNotificationSettings = () =>
  api.get('/api/notifications/settings').then(data)

// PR #53 연동: 알림 수신 설정을 저장한다.
//
// 토글이 아니라 "원하는 최종 상태"를 그대로 보내는 방식이라 두 값을 항상 함께 보내야 한다.
// 하나만 보내면 서버가 400으로 막는다 — boolean은 "안 보냄"과 "false"를 구분할 수 없어서다.
export const updateNotificationSettings = ({
  performanceShortageEnabled,
  benefitLimitEnabled,
}) =>
  api
    .patch('/api/notifications/settings', {
      performanceShortageEnabled,
      benefitLimitEnabled,
    })
    .then(data)
