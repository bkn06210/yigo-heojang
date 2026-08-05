import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || 'QR 요청 처리에 실패했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

export const createPaymentQr = (userCardId) =>
  api.post('/api/payments/qr', { userCardId }).then(data)

export const getPaymentQr = (qrToken) =>
  api.get(`/api/payments/qr/${qrToken}`).then(data)

export const payWithQr = (qrToken, payload) =>
  api.post(`/api/payments/qr/${qrToken}/pay`, payload).then(data)
// 07_25 연동 추가: QR 생성·조회·결제 백엔드 API를 프론트에 연결한다.
