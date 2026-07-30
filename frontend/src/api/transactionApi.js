import api from './axios'

export const getTransactionsByMemberId = (memberId) => {
  return api.get(`/transactions/members/${memberId}`)
}

export const getMyTransactions = () => {
  return getTransactionsByMemberId(1)
}