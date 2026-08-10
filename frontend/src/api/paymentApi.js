import api from './axios'

// 결제 추천 API 호출
export const getRecommendations = async (params) => {
  try {
    const response = await api.post('/api/recommendations', params)
    return response.data
  } catch (error) {
    console.error('추천 API 호출 실패:', error)
    throw error
  }
}
