import api from './axios'

export const getTransactionDetail = (expenseId) => {
    return api.get(`/transactions/${expenseId}`)
}

export const getTransactionsByMemberId = (memberId) => {
    return api.get(`/transactions/members/${memberId}`)
}