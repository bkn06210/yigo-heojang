import api from './axios'

// 카드 전체 월별 현황과 홈 브리핑
export const getCardMonthlyStatus = (yearMonth) => {
  const params = {}
  if (yearMonth) params.yearMonth = yearMonth
  return api.get('/api/cards/monthly-status', { params })
}

// 개별 카드 월별 현황
export const getCardStatus = (userCardId, yearMonth) => {
  const params = {}
  if (yearMonth) params.yearMonth = yearMonth
  return api.get(`/api/cards/${userCardId}/monthly-status`, { params })
}

// 카드 상세 정보 조회 (마스킹된 카드 번호 포함)
export const getUserCardDetail = (userCardId) => {
  return api.get(`/api/user-cards/${userCardId}`).then(res => res.data.data)
}
