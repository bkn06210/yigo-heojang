package com.wallet.engine.dto;

/**
 * 결제 직전 추천 요청 — 어디서 얼마를 쓸 예정인가.
 *
 * 입력이 구체적일수록 계산 범위가 넓어진다:
 * <pre>
 * merchantId 있음   가맹점 직접 혜택 + 그 카테고리 혜택 + 전체(ALL) 혜택
 * categoryId만 있음 카테고리 혜택 + 전체(ALL) 혜택
 * 둘 다 없음        전체(ALL) 혜택만 (장소 미정)
 * </pre>
 *
 * paymentType이 없으면 결제수단 조건이 걸린 혜택은 계산에서 빠진다 —
 * 받을지 확실하지 않은 혜택을 추천에 넣으면 실제보다 큰 금액을 보여주게 되기 때문이다.
 * 회원 id는 요청 body가 아니라 토큰에서 꺼낸다.
 */
public class RecommendationRequest {

    private Long merchantId;
    private Long categoryId;
    private Long expectedAmount;
    private String paymentType;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(Long expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }
}
