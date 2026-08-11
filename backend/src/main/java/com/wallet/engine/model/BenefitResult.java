package com.wallet.engine.model;

/**
 * 혜택 1건 계산 결과.
 *
 * 두 상태만 존재한다:
 * - 미적용: applied=false, benefitAmount=0, notApplicableReason 필수, appliedCap=NONE
 * - 적용:   applied=true, benefitAmount≥0 (한도 전액 소진이면 0원), notApplicableReason=null
 *
 * 불법 조합(미적용인데 금액>0 등)은 정적 팩토리에서 차단한다 — 생성자 직접 호출 금지.
 *
 * @param applied             적용 여부 (게이트 통과 여부)
 * @param benefitAmount       혜택액(원, 절사 후). 미적용이면 0
 * @param estimate            예상 여부. true=예상("약 1,500원"), false=확정. 미적용·FIXED·GIFT는 false
 * @param notApplicableReason 미적용 사유. 적용이면 null
 * @param appliedCap          혜택액을 깎은 상한. 안 깎였으면 NONE
 */
public record BenefitResult(
        boolean applied,
        long benefitAmount,
        boolean estimate,
        NotApplicableReason notApplicableReason,
        CapType appliedCap
) {

    public BenefitResult {
        if (appliedCap == null) {
            throw new IllegalArgumentException("appliedCap은 null 대신 NONE을 사용한다");
        }
        if (benefitAmount < 0) {
            throw new IllegalArgumentException("혜택액은 음수일 수 없다: " + benefitAmount);
        }
        if (applied && notApplicableReason != null) {
            throw new IllegalArgumentException("적용 결과에 미적용 사유가 있을 수 없다");
        }
        if (!applied && notApplicableReason == null) {
            throw new IllegalArgumentException("미적용 결과에는 사유가 필수다");
        }
        if (!applied && (benefitAmount != 0 || estimate || appliedCap != CapType.NONE)) {
            throw new IllegalArgumentException("미적용 결과는 0원·확정·NONE이어야 한다");
        }
    }

    public static BenefitResult notApplicable(NotApplicableReason reason) {
        return new BenefitResult(false, 0L, false, reason, CapType.NONE);
    }

    public static BenefitResult of(long benefitAmount, boolean estimate, CapType appliedCap) {
        return new BenefitResult(true, benefitAmount, estimate, null, appliedCap);
    }
}
