import api from './axios'

export const createPaymentQr = (userCardId) => {
    return api.post('/payments/qr', {
        userCardId,
    })
}

export const getPaymentQr = (qrToken) => {
    return api.get(`/payments/qr/${qrToken}`)
}

export const payWithQr = (qrToken, payload) => {
    return api.post(`/payments/qr/${qrToken}/pay`, payload)
}