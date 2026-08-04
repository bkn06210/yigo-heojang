package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitResult;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcContext;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CapType;
import com.wallet.engine.model.NotApplicableReason;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 혜택 1건 계산기 — 거래 1건에 혜택 규칙 1개를 적용해 혜택액을 구한다.
 *
 * Spring·DB·시계에 의존하지 않는 순수 계산기다. 카드 단위 비교(혜택 여러 개 중 1개 선택),
 * 실적 구간 판정, 묶음 한도 그룹 합산은 전부 이 클래스 밖에서 이뤄진다.
 *
 * 계산 순서:
 * <pre>
 * 0.  종류 게이트    RETROACTIVE·GIFT → 미적용
 * 1.  조건 게이트    실적 → 결제수단 → 건당최소금액
 * 1.5 횟수 게이트    일 횟수 → 월 횟수
 * 2.  대상금액       min(결제금액 − 포인트사용액, 대상금액상한)
 * 3.  계산           RATE: 대상금액 × 율 / FIXED: 정액
 * 3.5 결제금액 클램프 min(계산값, 대상금액)  — 할인이 결제액을 넘지 않게
 * 4~7 상한 클램프    건당 → 일 → 월 → 통합. 각 잔여는 max(0, 한도 − 소진)
 * 8.  절사           원 미만 버림
 * </pre>
 *
 * 4~7은 전부 min 연산이라 금액만 보면 순서가 무관하다(교환법칙). 순서가 실제로 영향을 주는 건
 * "어느 상한에 걸렸나"(CapType) 기록뿐이며, 동률이면 위 순서의 첫 번째를 남긴다 — 테스트 재현성.
 */
public final class BenefitCalculator {

    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

    public BenefitResult calculate(BenefitRule rule, CalcContext context) {
        if (rule == null || context == null) {
            throw new IllegalArgumentException("rule과 context는 필수다");
        }

        NotApplicableReason gateFailure = checkGates(rule, context);
        if (gateFailure != null) {
            return BenefitResult.notApplicable(gateFailure);
        }

        long eligibleAmount = eligibleAmount(rule, context);
        BigDecimal rawBenefit = rawBenefit(rule, eligibleAmount);

        ClampResult clamped = applyCaps(rule, context, rawBenefit, eligibleAmount);
        long benefitAmount = clamped.amount().setScale(0, RoundingMode.DOWN).longValue();

        return BenefitResult.of(benefitAmount, isEstimate(rule, context, clamped), clamped.capType());
    }

    /** 적용 자체를 막는 조건들. 검사 순서가 곧 사유 우선순위다. */
    private NotApplicableReason checkGates(BenefitRule rule, CalcContext context) {
        if (rule.getBenefitKind() == BenefitKind.RETROACTIVE) {
            return NotApplicableReason.RETROACTIVE_EXCLUDED;
        }
        // 증정은 결제 트랜잭션 자체가 없다(카드를 제시할 뿐). 한도 소진·횟수를 추적할 수 없고,
        // 적용된 것으로 두면 다른 혜택이 없는 카드에서 선택돼 "커피 결제에 라운지 혜택 적용"이 기록된다.
        if (rule.getBenefitKind() == BenefitKind.GIFT) {
            return NotApplicableReason.GIFT_EXCLUDED;
        }
        // 할부수수료 면제라 결제금액 기준 할인액이 없다. 금액으로 환산하면 카드 비교 순위가 왜곡된다.
        if (rule.getBenefitKind() == BenefitKind.INSTALLMENT_FREE) {
            return NotApplicableReason.INSTALLMENT_FREE_EXCLUDED;
        }
        if (rule.isRequirePerformance() && !context.isPerformanceMet()) {
            return NotApplicableReason.PERFORMANCE_NOT_MET;
        }
        // 결제수단 미입력은 "조건 충족 여부를 모름"이므로 제외한다.
        // 못 받을 수도 있는 혜택을 추천에 넣으면 실제보다 큰 금액을 보여주게 된다.
        if (rule.getRequirePaymentType() != null
                && !rule.getRequirePaymentType().equals(context.getPaymentType())) {
            return NotApplicableReason.PAYMENT_TYPE_MISMATCH;
        }
        if (rule.getMinTxnAmount() != null && context.getPaymentAmount() < rule.getMinTxnAmount()) {
            return NotApplicableReason.MIN_TXN_AMOUNT_NOT_MET;
        }
        if (rule.getDailyCountLimit() != null && context.getDailyUsedCount() >= rule.getDailyCountLimit()) {
            return NotApplicableReason.DAILY_COUNT_EXCEEDED;
        }
        if (rule.getMonthlyCountLimit() != null && context.getMonthlyUsedCount() >= rule.getMonthlyCountLimit()) {
            return NotApplicableReason.MONTHLY_COUNT_EXCEEDED;
        }
        return null;
    }

    /**
     * 혜택이 붙는 금액. 포인트로 낸 부분엔 혜택이 붙지 않는다.
     * (결제금액이 이미 포인트 차감 후 승인액이라 usedPointAmount는 0이지만,
     *  포인트 부분결제가 들어오면 바로 동작하도록 식은 이 형태로 둔다.)
     */
    private long eligibleAmount(BenefitRule rule, CalcContext context) {
        long targetAmount = context.getPaymentAmount() - context.getUsedPointAmount();
        if (rule.getMaxEligibleAmount() != null) {
            return Math.min(targetAmount, rule.getMaxEligibleAmount());
        }
        return targetAmount;
    }

    private BigDecimal rawBenefit(BenefitRule rule, long eligibleAmount) {
        if (rule.getCalcMethod() == CalcMethod.RATE) {
            return BigDecimal.valueOf(eligibleAmount)
                    .multiply(rule.getBenefitValue())
                    .divide(PERCENT_DIVISOR, 10, RoundingMode.DOWN);
        }
        return rule.getBenefitValue();
    }

    private ClampResult applyCaps(BenefitRule rule, CalcContext context,
                                  BigDecimal rawBenefit, long eligibleAmount) {
        ClampResult result = new ClampResult(rawBenefit, CapType.NONE);

        // 정액 혜택이 결제금액을 넘는 경우를 막는다 (정률은 수학적으로 넘을 수 없음)
        result = result.clampTo(eligibleAmount, CapType.PAYMENT_AMOUNT);
        result = result.clampTo(rule.getMaxBenefitPerTxn(), CapType.MAX_BENEFIT_PER_TXN);
        result = result.clampTo(remaining(rule.getDailyLimit(), context.getDailyUsedAmount()), CapType.DAILY_LIMIT);
        result = result.clampTo(remaining(rule.getMonthlyLimit(), context.getMonthlyUsedAmount()), CapType.MONTHLY_LIMIT);
        if (rule.isUseSharedLimit()) {
            result = result.clampTo(
                    remaining(context.getSharedMonthlyLimit(), context.getSharedLimitUsed()), CapType.SHARED_LIMIT);
        }
        return result;
    }

    /** 잔여 한도. 한도가 없으면(null) 제약 없음, 소진이 한도를 넘었으면(취소·보정) 0으로 막는다. */
    private Long remaining(Long limit, long used) {
        if (limit == null) {
            return null;
        }
        return Math.max(0L, limit - used);
    }

    /**
     * 예상 여부. "확정"은 실제 결제금액이 예상보다 커져도 혜택액이 안 변한다는 뜻이다.
     * 정액이거나, 어떤 상한에 걸려 값이 고정됐거나, 금액이 확정 입력이면 확정이다.
     */
    private boolean isEstimate(BenefitRule rule, CalcContext context, ClampResult clamped) {
        if (rule.getCalcMethod() == CalcMethod.FIXED) {
            return false;
        }
        if (!context.isAmountEstimated()) {
            return false;
        }
        // 대상금액 상한에 걸리면 계산 기반이 상수가 되므로 결과도 고정된다
        if (rule.getMaxEligibleAmount() != null
                && context.getPaymentAmount() - context.getUsedPointAmount() >= rule.getMaxEligibleAmount()) {
            return false;
        }
        return clamped.capType() == CapType.NONE;
    }

    /**
     * 클램프 진행 상태. 절사 전 값으로 비교해야 상한 도달 판정이 정확하다
     * (계산값 1,234.56에 상한 1,234면 금액은 같아도 "상한에 걸린 것"이 맞다).
     */
    private record ClampResult(BigDecimal amount, CapType capType) {

        ClampResult clampTo(Long cap, CapType candidate) {
            if (cap == null) {
                return this;
            }
            BigDecimal capValue = BigDecimal.valueOf(cap);
            if (amount.compareTo(capValue) < 0) {
                return this;
            }
            // 동률이면 먼저 검사한 상한을 남긴다
            CapType recorded = (capType == CapType.NONE) ? candidate : capType;
            return new ClampResult(capValue, recorded);
        }
    }
}
