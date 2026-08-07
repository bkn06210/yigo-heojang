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
