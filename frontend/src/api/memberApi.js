import api from './axios'

export const getMyInfo = () => api.get('/api/members/me')

export const updateMyInfo = (memberData) => (
  api.patch('/api/members/me', memberData)
)
