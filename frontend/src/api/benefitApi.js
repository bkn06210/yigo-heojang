import api from './axios'

const data = (response) => {
  const body = response.data
  if (body?.success === false) {
    const error = new Error(body.message || '혜택 정보를 불러오지 못했습니다.')
    error.code = body.code
    throw error
  }
  return body?.data ?? body
}

// 월별 혜택 리포트
// GET /api/benefits/report
//
// 총액·부문별 합계·거래 목록이 한 응답에 다 온다. 홈 리포트 카드, 바텀시트 목록,
// 부문 상세가 같은 응답을 나눠 쓴다 — 단계마다 따로 부르면 그 사이 결제가 일어났을 때
// 합계와 상세가 어긋난다.
//
// yearMonth를 생략하면 이번 달이다. 형식은 'YYYY-MM'.
// 받은 혜택이 없어도 에러가 아니라 totalBenefitAmount: 0, categories: [] 로 온다.
export const getBenefitReport = (yearMonth) => {

  const config = yearMonth
    ? { params: { yearMonth } }
    : undefined

  return api.get('/api/benefits/report', config).then(data)

}

// 가맹점·업종별 카드 혜택 조회
// GET /api/cards/applicable-benefits
//
// 혜택액을 계산하지 않고 조건만 돌려준다. 금액이 정해지지 않은 질문("여기서 어느 카드가 좋아?")에
// 쓰는 용도라, 결제 직전 추천(expectedAmount 필수)과는 쓰임이 다르다.
//
// merchantId를 주면 그 가맹점 혜택 + 소속 업종 혜택을 함께 본다.
// categoryId만 주면 업종·전체 혜택까지만 본다.
// 실적 미달로 지금 못 받는 혜택도 available: false + unavailableReason 으로 함께 온다.
export const getApplicableBenefits = ({ merchantId, categoryId } = {}) => {

  const params = {}

  if (merchantId != null) params.merchantId = merchantId
  if (categoryId != null) params.categoryId = categoryId

  return api.get('/api/cards/applicable-benefits', { params }).then(data)

}
