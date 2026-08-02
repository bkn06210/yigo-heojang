import api from './axios'

export const getCardRecommendations = (paymentData) => (
  api.post('/api/recommendations', paymentData)
)
