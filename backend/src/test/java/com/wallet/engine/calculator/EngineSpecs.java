package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcContext;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;
import com.wallet.engine.model.TargetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 속성 테스트의 입력 명세 — 무작위로 만든 값을 담아 두었다가 엔진 객체로 바꾼다.
 *
 * 엔진 객체(BenefitRule 등)를 직접 생성하지 않고 한 단계를 두는 이유는 둘이다.
 * 실패했을 때 반례가 사람이 읽을 수 있는 글로 찍혀야 하는데 엔진 객체에는 toString이 없고,
 * 불변식을 검사하는 쪽이 한도·묶음 값을 엔진을 거치지 않고 읽을 수 있어야 검사가 엔진과 독립이 된다.
 */
final class EngineSpecs {

    private EngineSpecs() {
    }

    /** 기간별 금액 한도. null = 제약 없음, 0 = 혜택 없음 */
    record AmountLimits(Long daily, Long monthly, Long quarterly, Long yearly) {

        static AmountLimits none() {
            return new AmountLimits(null, null, null, null);
        }

        boolean isEmpty() {
            return daily == null && monthly == null && quarterly == null && yearly == null;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", "금액한도[", "]");
            append(joiner, "일", daily);
            append(joiner, "월", monthly);
            append(joiner, "분기", quarterly);
            append(joiner, "연", yearly);
            return joiner.toString();
        }
    }

    /** 기간별 횟수 한도. null = 제약 없음, 0 = 혜택 없음 */
    record CountLimits(Integer daily, Integer monthly, Integer quarterly, Integer yearly) {

        static CountLimits none() {
            return new CountLimits(null, null, null, null);
        }

        boolean isEmpty() {
            return daily == null && monthly == null && quarterly == null && yearly == null;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", "횟수한도[", "]");
            append(joiner, "일", daily);
            append(joiner, "월", monthly);
            append(joiner, "분기", quarterly);
            append(joiner, "연", yearly);
            return joiner.toString();
        }
    }

    /** 혜택 규칙 한 건 (benefit 한 행의 계산 부분) */
    record RuleSpec(
            long benefitId,
            BenefitKind kind,
            CalcMethod method,
            BigDecimal value,
            Integer stepCount,
            boolean requirePerformance,
            String requirePaymentType,
            Long minTxnAmount,
            Long maxEligibleAmount,
            Long maxBenefitPerTxn,
            AmountLimits amountLimits,
            CountLimits countLimits,
            boolean useSharedLimit) {

        BenefitRule toRule() {
            return BenefitRule.builder()
                    .benefitId(benefitId)
                    .benefitKind(kind)
                    .calcMethod(method)
                    .benefitValue(value)
                    .stepCount(stepCount)
                    .requirePerformance(requirePerformance)
                    .requirePaymentType(requirePaymentType)
                    .minTxnAmount(minTxnAmount)
                    .maxEligibleAmount(maxEligibleAmount)
                    .maxBenefitPerTxn(maxBenefitPerTxn)
                    .dailyLimit(amountLimits.daily())
                    .monthlyLimit(amountLimits.monthly())
                    .quarterlyLimit(amountLimits.quarterly())
                    .yearlyLimit(amountLimits.yearly())
                    .dailyCountLimit(countLimits.daily())
                    .monthlyCountLimit(countLimits.monthly())
                    .quarterlyCountLimit(countLimits.quarterly())
                    .yearlyCountLimit(countLimits.yearly())
                    .useSharedLimit(useSharedLimit)
                    .build();
        }

        RuleSpec withBenefitId(long newBenefitId) {
            return new RuleSpec(newBenefitId, kind, method, value, stepCount, requirePerformance,
                    requirePaymentType, minTxnAmount, maxEligibleAmount, maxBenefitPerTxn,
                    amountLimits, countLimits, useSharedLimit);
        }

        RuleSpec withAmountLimits(AmountLimits newAmountLimits) {
            return new RuleSpec(benefitId, kind, method, value, stepCount, requirePerformance,
                    requirePaymentType, minTxnAmount, maxEligibleAmount, maxBenefitPerTxn,
                    newAmountLimits, countLimits, useSharedLimit);
        }

        RuleSpec withCountLimits(CountLimits newCountLimits) {
            return new RuleSpec(benefitId, kind, method, value, stepCount, requirePerformance,
                    requirePaymentType, minTxnAmount, maxEligibleAmount, maxBenefitPerTxn,
                    amountLimits, newCountLimits, useSharedLimit);
        }

        RuleSpec withMaxBenefitPerTxn(Long newMaxBenefitPerTxn) {
            return new RuleSpec(benefitId, kind, method, value, stepCount, requirePerformance,
                    requirePaymentType, minTxnAmount, maxEligibleAmount, newMaxBenefitPerTxn,
                    amountLimits, countLimits, useSharedLimit);
        }

        RuleSpec withUseSharedLimit(boolean newUseSharedLimit) {
            return new RuleSpec(benefitId, kind, method, value, stepCount, requirePerformance,
                    requirePaymentType, minTxnAmount, maxEligibleAmount, maxBenefitPerTxn,
                    amountLimits, countLimits, newUseSharedLimit);
        }

        /** 계산 대상 종류인가. 증정·사후정산·무이자할부는 금액을 내지 않는다 */
        boolean isCalculableKind() {
            return kind == BenefitKind.DISCOUNT || kind == BenefitKind.POINT || kind == BenefitKind.SPECIAL_PRICE;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", "혜택#" + benefitId + "{", "}");
            joiner.add(switch (method) {
                case RATE -> "정률 " + value + "%";
                case FIXED -> "정액 " + value + "원";
                case COUNT_STEP -> stepCount + "회마다 " + value + "원";
            });
            if (kind != BenefitKind.DISCOUNT) {
                joiner.add("종류=" + kind);
            }
            if (requirePerformance) {
                joiner.add("실적필요");
            }
            append(joiner, "결제수단", requirePaymentType);
            append(joiner, "건당최소", minTxnAmount);
            append(joiner, "대상금액상한", maxEligibleAmount);
            append(joiner, "건당최대", maxBenefitPerTxn);
            if (!amountLimits.isEmpty()) {
                joiner.add(amountLimits.toString());
            }
            if (!countLimits.isEmpty()) {
                joiner.add(countLimits.toString());
            }
            if (useSharedLimit) {
                joiner.add("통합한도사용");
            }
            return joiner.toString();
        }
    }

    /** 기간별 소진액. 일 ≤ 월 ≤ 분기 ≤ 연 — 넓은 기간이 좁은 기간을 포함하므로 실제 상태도 이 순서다 */
    record UsedAmounts(long daily, long monthly, long quarterly, long yearly) {

        static UsedAmounts none() {
            return new UsedAmounts(0L, 0L, 0L, 0L);
        }

        UsedAmounts plus(UsedAmounts extra) {
            return new UsedAmounts(daily + extra.daily, monthly + extra.monthly,
                    quarterly + extra.quarterly, yearly + extra.yearly);
        }
    }

    /** 기간별 소진 횟수. 순서 관계는 금액과 같다 */
    record UsedCounts(int daily, int monthly, int quarterly, int yearly) {

        static UsedCounts none() {
            return new UsedCounts(0, 0, 0, 0);
        }

        UsedCounts plus(UsedCounts extra) {
            return new UsedCounts(daily + extra.daily, monthly + extra.monthly,
                    quarterly + extra.quarterly, yearly + extra.yearly);
        }
    }

    /** 거래 1건과 그 시점의 소진 상태 (CalcContext) */
    record ContextSpec(
            long paymentAmount,
            long usedPointAmount,
            String paymentType,
            boolean performanceMet,
            boolean amountEstimated,
            UsedAmounts usedAmounts,
            UsedCounts usedCounts,
            Long sharedMonthlyLimit,
            long sharedLimitUsed) {

        CalcContext toContext() {
            return CalcContext.builder()
                    .paymentAmount(paymentAmount)
                    .usedPointAmount(usedPointAmount)
                    .paymentType(paymentType)
                    .performanceMet(performanceMet)
                    .amountEstimated(amountEstimated)
                    .dailyUsedAmount(usedAmounts.daily())
                    .monthlyUsedAmount(usedAmounts.monthly())
                    .quarterlyUsedAmount(usedAmounts.quarterly())
                    .yearlyUsedAmount(usedAmounts.yearly())
                    .dailyUsedCount(usedCounts.daily())
                    .monthlyUsedCount(usedCounts.monthly())
                    .quarterlyUsedCount(usedCounts.quarterly())
                    .yearlyUsedCount(usedCounts.yearly())
                    .sharedMonthlyLimit(sharedMonthlyLimit)
                    .sharedLimitUsed(sharedLimitUsed)
                    .build();
        }

        /** 혜택이 붙는 금액 — 포인트로 낸 부분을 뺀 승인액 */
        long targetAmount() {
            return paymentAmount - usedPointAmount;
        }

        ContextSpec withPaymentAmount(long newPaymentAmount) {
            return new ContextSpec(newPaymentAmount, usedPointAmount, paymentType, performanceMet,
                    amountEstimated, usedAmounts, usedCounts, sharedMonthlyLimit, sharedLimitUsed);
        }

        ContextSpec withUsedAmounts(UsedAmounts newUsedAmounts) {
            return new ContextSpec(paymentAmount, usedPointAmount, paymentType, performanceMet,
                    amountEstimated, newUsedAmounts, usedCounts, sharedMonthlyLimit, sharedLimitUsed);
        }

        ContextSpec withUsedCounts(UsedCounts newUsedCounts) {
            return new ContextSpec(paymentAmount, usedPointAmount, paymentType, performanceMet,
                    amountEstimated, usedAmounts, newUsedCounts, sharedMonthlyLimit, sharedLimitUsed);
        }

        ContextSpec withSharedLimitUsed(long newSharedLimitUsed) {
            return new ContextSpec(paymentAmount, usedPointAmount, paymentType, performanceMet,
                    amountEstimated, usedAmounts, usedCounts, sharedMonthlyLimit, newSharedLimitUsed);
        }

        ContextSpec withAmountEstimated(boolean newAmountEstimated) {
            return new ContextSpec(paymentAmount, usedPointAmount, paymentType, performanceMet,
                    newAmountEstimated, usedAmounts, usedCounts, sharedMonthlyLimit, sharedLimitUsed);
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", "결제{", "}");
            joiner.add(paymentAmount + "원");
            if (usedPointAmount != 0) {
                joiner.add("포인트사용=" + usedPointAmount);
            }
            append(joiner, "결제수단", paymentType);
            joiner.add(performanceMet ? "실적충족" : "실적미달");
            joiner.add(amountEstimated ? "예상금액" : "확정금액");
            if (!usedAmounts.equals(UsedAmounts.none())) {
                joiner.add("소진액[일=" + usedAmounts.daily() + ", 월=" + usedAmounts.monthly()
                        + ", 분기=" + usedAmounts.quarterly() + ", 연=" + usedAmounts.yearly() + "]");
            }
            if (!usedCounts.equals(UsedCounts.none())) {
                joiner.add("소진횟수[일=" + usedCounts.daily() + ", 월=" + usedCounts.monthly()
                        + ", 분기=" + usedCounts.quarterly() + ", 연=" + usedCounts.yearly() + "]");
            }
            append(joiner, "통합한도", sharedMonthlyLimit);
            if (sharedLimitUsed != 0) {
                joiner.add("통합소진=" + sharedLimitUsed);
            }
            return joiner.toString();
        }
    }

    /**
     * 결제가 일어나는 곳. 시연 데이터의 구조(대분류 → 중분류 → 가맹점)를 줄여 옮긴 것이다.
     *
     * 수를 적게 둔 것은 일부러다 — 혜택 대상과 결제 장소가 자주 겹쳐야 한도 경쟁이 생긴다.
     */
    enum Place {
        STARBUCKS(10L, "STARBUCKS", 102L, "CAFE", 1L, "DINING"),
        EDIYA(11L, "EDIYA", 102L, "CAFE", 1L, "DINING"),
        RESTAURANT(null, null, 101L, "RESTAURANT", 1L, "DINING"),
        GS25(20L, "GS25", 201L, "CONVENIENCE_STORE", 2L, "SHOPPING"),
        ONLINE_MALL(null, null, 204L, "ONLINE_SHOPPING", 2L, "SHOPPING"),
        /** 장소 미정 — 전 가맹점 혜택만 매칭된다 */
        UNSPECIFIED(null, null, null, null, null, null);

        private final Long merchantId;
        private final String merchantCode;
        private final Long categoryId;
        private final String categoryCode;
        private final Long parentCategoryId;
        private final String parentCategoryCode;

        Place(Long merchantId, String merchantCode, Long categoryId, String categoryCode,
              Long parentCategoryId, String parentCategoryCode) {
            this.merchantId = merchantId;
            this.merchantCode = merchantCode;
            this.categoryId = categoryId;
            this.categoryCode = categoryCode;
            this.parentCategoryId = parentCategoryId;
            this.parentCategoryCode = parentCategoryCode;
        }

        PaymentTarget toTarget() {
            return PaymentTarget.builder()
                    .merchantId(merchantId)
                    .merchantCode(merchantCode)
                    .categoryId(categoryId)
                    .categoryCode(categoryCode)
                    .parentCategoryId(parentCategoryId)
                    .parentCategoryCode(parentCategoryCode)
                    .build();
        }

        /** 이 장소의 결제가 그 대상에 해당하는가 — 매칭기와 따로 적은 판정이다 */
        boolean isCoveredBy(TargetSpec target) {
            return switch (target.type()) {
                case ALL -> true;
                case MERCHANT -> target.merchantId().equals(merchantId);
                case CATEGORY -> target.categoryId().equals(categoryId)
                        || target.categoryId().equals(parentCategoryId);
            };
        }
    }

    /** 혜택이 겨냥하는 대상 */
    record TargetSpec(TargetType type, Long categoryId, Long merchantId) {

        static TargetSpec all() {
            return new TargetSpec(TargetType.ALL, null, null);
        }

        static TargetSpec category(long categoryId) {
            return new TargetSpec(TargetType.CATEGORY, categoryId, null);
        }

        static TargetSpec merchant(long merchantId) {
            return new TargetSpec(TargetType.MERCHANT, null, merchantId);
        }

        /** 두 대상에 모두 해당하는 결제 장소가 있는가 */
        boolean overlaps(TargetSpec other) {
            for (Place place : Place.values()) {
                if (place.isCoveredBy(this) && place.isCoveredBy(other)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return switch (type) {
                case ALL -> "전가맹점";
                case CATEGORY -> "카테고리 " + categoryId;
                case MERCHANT -> "가맹점 " + merchantId;
            };
        }
    }

    /** 카드가 가진 혜택 하나 — 규칙에 대상과 묶음 정보가 붙는다 (BenefitCandidate) */
    record BenefitSpec(
            RuleSpec rule,
            TargetSpec target,
            String limitGroupCode,
            String countGroupCode,
            String optionGroupCode,
            String optionKey,
            PerformancePeriod performancePeriod) {

        BenefitCandidate toCandidate() {
            return BenefitCandidate.builder()
                    .targetType(target.type())
                    .targetCategoryId(target.categoryId())
                    .targetMerchantId(target.merchantId())
                    .limitGroupCode(limitGroupCode)
                    .countGroupCode(countGroupCode)
                    .optionGroupCode(optionGroupCode)
                    .optionKey(optionKey)
                    .performancePeriod(performancePeriod)
                    .rule(rule.toRule())
                    .build();
        }

        long benefitId() {
            return rule.benefitId();
        }

        /** 횟수를 함께 세는 묶음의 코드. 따로 지정하지 않으면 금액 묶음을 따른다 */
        String effectiveCountGroupCode() {
            return countGroupCode == null ? limitGroupCode : countGroupCode;
        }

        BenefitSpec withRule(RuleSpec newRule) {
            return new BenefitSpec(newRule, target, limitGroupCode, countGroupCode,
                    optionGroupCode, optionKey, performancePeriod);
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            joiner.add(target.toString());
            joiner.add(rule.toString());
            append(joiner, "금액묶음", limitGroupCode);
            append(joiner, "횟수묶음", countGroupCode);
            if (optionGroupCode != null) {
                joiner.add("선택형=" + optionGroupCode + "/" + optionKey);
            }
            if (performancePeriod == PerformancePeriod.QUARTER) {
                joiner.add("분기실적축");
            }
            return joiner.toString();
        }
    }

    /** 실적 판정 결과. 최소실적이 0이면 실적 미충족 구간이다 */
    record StatusSpec(long minPerformanceAmount, Long sharedMonthlyLimit) {

        PerformanceStatus toStatus(long tierId) {
            return new PerformanceStatus(tierId, minPerformanceAmount, sharedMonthlyLimit);
        }

        boolean performanceMet() {
            return minPerformanceAmount > 0;
        }

        @Override
        public String toString() {
            return (performanceMet() ? "실적충족" : "실적미달")
                    + (sharedMonthlyLimit == null ? "" : ", 통합한도=" + sharedMonthlyLimit);
        }
    }

    /** 카드 한 장 — 혜택 목록과 실적 판정 결과 */
    record CardSpec(
            long cardId,
            List<BenefitSpec> benefits,
            StatusSpec monthStatus,
            StatusSpec quarterStatus,
            Map<String, String> selectedOptionKeys) {

        List<BenefitCandidate> toCandidates() {
            List<BenefitCandidate> candidates = new ArrayList<>(benefits.size());
            for (BenefitSpec benefit : benefits) {
                candidates.add(benefit.toCandidate());
            }
            return candidates;
        }

        PerformanceStatus toMonthStatus() {
            return monthStatus.toStatus(1L);
        }

        PerformanceStatus toQuarterStatus() {
            return quarterStatus == null ? null : quarterStatus.toStatus(2L);
        }

        SimulatedCard toSimulatedCard() {
            return new SimulatedCard(cardId, toCandidates(), toMonthStatus(), toQuarterStatus(), selectedOptionKeys);
        }

        BenefitSpec benefitOf(long benefitId) {
            for (BenefitSpec benefit : benefits) {
                if (benefit.benefitId() == benefitId) {
                    return benefit;
                }
            }
            throw new IllegalArgumentException("이 카드에 없는 혜택이다: " + benefitId);
        }

        boolean hasStampBenefit() {
            return benefits.stream().anyMatch(benefit -> benefit.rule().method() == CalcMethod.COUNT_STEP);
        }

        /** 이 혜택의 실적 조건이 걸린 기간 축에서 실적을 채웠는가 */
        boolean performanceMetFor(BenefitSpec benefit) {
            StatusSpec status = benefit.performancePeriod() == PerformancePeriod.QUARTER ? quarterStatus : monthStatus;
            return status != null && status.performanceMet();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("카드#" + cardId + "(" + monthStatus);
            if (quarterStatus != null) {
                builder.append(", 분기: ").append(quarterStatus);
            }
            if (!selectedOptionKeys.isEmpty()) {
                builder.append(", 선택=").append(selectedOptionKeys);
            }
            builder.append(")");
            for (BenefitSpec benefit : benefits) {
                builder.append("\n      ").append(benefit);
            }
            return builder.toString();
        }
    }

    /** 흘려보낼 결제 한 건. 날짜는 한 달 안의 일(day)로만 든다 — 일 한도 리셋을 일으키는 값이다 */
    record PaymentSpec(int dayOfMonth, Place place, long amount, String paymentType) {

        private static final LocalDate MONTH_START = LocalDate.of(2026, 7, 1);

        LocalDate paymentDate() {
            return MONTH_START.plusDays(dayOfMonth - 1L);
        }

        PaymentRequest toRequest() {
            return PaymentRequest.estimated(place.toTarget(), amount, paymentType);
        }

        SimulatedPayment toPayment() {
            return new SimulatedPayment(paymentDate(), toRequest());
        }

        @Override
        public String toString() {
            return dayOfMonth + "일 " + place + " " + amount + "원"
                    + (paymentType == null ? "" : " (" + paymentType + ")");
        }
    }

    static List<SimulatedPayment> toPayments(List<PaymentSpec> payments) {
        List<SimulatedPayment> converted = new ArrayList<>(payments.size());
        for (PaymentSpec payment : payments) {
            converted.add(payment.toPayment());
        }
        return converted;
    }

    static List<SimulatedCard> toSimulatedCards(List<CardSpec> cards) {
        List<SimulatedCard> converted = new ArrayList<>(cards.size());
        for (CardSpec card : cards) {
            converted.add(card.toSimulatedCard());
        }
        return converted;
    }

    private static void append(StringJoiner joiner, String label, Object value) {
        if (value != null) {
            joiner.add(label + "=" + value);
        }
    }
}
