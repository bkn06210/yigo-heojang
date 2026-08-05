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

// PR #29 연동: 카드번호 BIN으로 등록 가능한 카드 상품 후보를 조회한다.
export const getUserCardCandidates = (cardNumber) =>
  api.post('/api/user-cards/candidates', { cardNumber }).then(data)

// PR #29 연동: 선택한 카드 상품과 카드번호로 로그인 회원의 보유카드를 등록한다.
export const registerUserCard = (cardId, cardNumber) =>
  api.post('/api/user-cards', { cardId, cardNumber }).then(data)

// PR #32 연동: 로그인 회원의 활성 보유카드 기본 목록을 조회한다.
export const getUserCards = () =>
  api.get('/api/user-cards').then(data)

export const createPayment = (payload) => api.post('/api/payments', payload).then(data)
export const getPayment = (id) => api.get(`/api/payments/${id}`).then(data)
// 07_25 연동 추가: 카드·포인트·멤버십·소비내역 API를 화면에서 공통으로 사용한다.
