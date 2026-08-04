package com.wallet.engine.model;

/**
 * 계산 대상 결제 한 건 — 어디서, 얼마를, 어떤 수단으로.
 *
 * @param target          결제 지점(가맹점·카테고리). 장소 미정이면 {@link PaymentTarget#unspecified()}
 * @param paymentAmount   결제금액(원). 추천 흐름에서는 5천원 단위 구간 대표값
 * @param usedPointAmount 포인트 사용분. amount가 차감 후 값이라 항상 0이지만 계산식의 자리는 유지한다
 * @param paymentType     결제수단(CARD, SIMPLE_PAY 등). null이면 결제수단 조건이 걸린 혜택은 제외된다
 * @param amountEstimated 금액이 구간 대표값이면 true(추천), 확정 금액이면 false(정산 재계산)
 */
public record PaymentRequest(
        PaymentTarget target,
        long paymentAmount,
        long usedPointAmount,
        String paymentType,
        boolean amountEstimated
) {

    public PaymentRequest {
        if (target == null) {
            throw new IllegalArgumentException("target은 필수다 (장소 미정이면 PaymentTarget.unspecified())");
        }
        if (paymentAmount <= 0) {
            throw new IllegalArgumentException("결제금액은 양수여야 한다: " + paymentAmount);
        }
    }

    /** 추천 흐름용 — 금액이 구간 대표값(예상)인 결제 */
    public static PaymentRequest estimated(PaymentTarget target, long paymentAmount, String paymentType) {
        return new PaymentRequest(target, paymentAmount, 0L, paymentType, true);
    }

    /** 정산 흐름용 — 금액이 확정된 결제 */
    public static PaymentRequest confirmed(PaymentTarget target, long paymentAmount, String paymentType) {
        return new PaymentRequest(target, paymentAmount, 0L, paymentType, false);
    }
}
