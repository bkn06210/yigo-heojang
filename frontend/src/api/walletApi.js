import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || '요청 처리에 실패했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

export const getPoints = () => api.get('/api/points').then(data)
export const getPointHistory = (params = {}) => api.get('/api/points/history', { params }).then(data)
export const getPointUsagePlaces = (providerId, params = {}) =>
  api.get(`/api/points/${providerId}/usage-places`, { params }).then(data)

export const getMemberships = () => api.get('/api/memberships').then(data)
export const getMembershipProviders = () => api.get('/api/memberships/providers').then(data)
export const getMembership = (id) => api.get(`/api/memberships/${id}`).then(data)
export const registerMembership = (pointProviderId) =>
  api.post('/api/memberships', { pointProviderId }).then(data)
export const deleteMembership = (id) => api.delete(`/api/memberships/${id}`).then(data)

export const getTransactions = (params = {}) => api.get('/api/transactions', { params }).then(data)
export const getTransaction = (id) => api.get(`/api/transactions/${id}`).then(data)
export const getTransactionsSummary = (params = {}) => api.get('/api/transactions/summary', { params }).then(data)
export const syncTransactions = () => api.post('/api/transactions/sync').then(data)
export const getExpenseCategories = () => api.get('/api/expense-categories').then(data)

export const getCardRecommendations = (payload) =>
  api.post('/api/recommendations', payload).then(data)

// PR #25 연동: 로그인 회원의 전체 보유카드 월별 실적/혜택 요약을 조회한다.
export const getCardMonthlyStatuses = (params = {}) =>
  api.get('/api/cards/monthly-status', { params }).then(data)

// PR #25 연동: 선택한 보유카드 한 장의 월별 실적과 혜택별 사용 현황을 조회한다.
export const getCardMonthlyStatus = (userCardId, params = {}) =>
  api.get(`/api/cards/${userCardId}/monthly-status`, { params }).then(data)

// 카드번호만 보내면 서버가 등록 가능한 번호인지 확인하고 카드 상품을 결정한다.
// 클라이언트는 카드 상품(cardId)을 고르지 않는다 — 카드번호와 무관한 상품이
// 등록되지 않도록 매칭 권한을 서버가 갖는다.
//
// 응답: { userCardId, cardId, cardName, issuerName, cardType, maskedCardNumber, imageUrl, representative }
// 실패: CARD_NUMBER_INVALID(형식·룬 검증), CARD_NOT_SUPPORTED(등록 미지원 번호),
//       USER_CARD_ALREADY_EXISTS(이미 등록한 카드)
export const registerUserCard = (cardNumber) =>
  api.post('/api/user-cards', { cardNumber }).then(data)

// PR #32 연동: 로그인 회원의 활성 보유카드 기본 목록을 조회한다.
export const getUserCards = () =>
  api.get('/api/user-cards').then(data)

// 카드 고정 설정 (representative 업데이트)
export const updateCardRepresentative = (userCardId, representative) =>
  api.patch(`/api/user-cards/${userCardId}/representative`, { representative }).then(data)

// PR #34 연동: 204 응답 본문을 파싱하지 않고 HTTP 성공 여부로 보유카드 삭제를 확정한다.
export const deleteUserCard = (userCardId) =>
  api.delete(`/api/user-cards/${userCardId}`).then(() => undefined)

export const createPayment = (payload) => api.post('/api/payments', payload).then(data)
export const getPayment = (id) => api.get(`/api/payments/${id}`).then(data)
// 07_25 연동 추가: 카드·포인트·멤버십·소비내역 API를 화면에서 공통으로 사용한다.
