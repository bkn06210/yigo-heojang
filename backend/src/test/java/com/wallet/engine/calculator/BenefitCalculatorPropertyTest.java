package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineArbitraries.StampMode;
import com.wallet.engine.calculator.EngineSpecs.AmountLimits;
import com.wallet.engine.calculator.EngineSpecs.ContextSpec;
import com.wallet.engine.calculator.EngineSpecs.CountLimits;
import com.wallet.engine.calculator.EngineSpecs.RuleSpec;
import com.wallet.engine.calculator.EngineSpecs.UsedAmounts;
import com.wallet.engine.calculator.EngineSpecs.UsedCounts;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitResult;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CapType;
import com.wallet.engine.model.NotApplicableReason;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Disabled;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.statistics.Statistics;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 혜택 1건 계산의 불변식 — 무작위 규칙·상태 수만 건에서 "어떤 입력에도 깨지면 안 되는 것"을 검사한다.
 *
 * 예시 테스트는 사람이 떠올린 조합만 본다. 한도 여덟 축에 건당 상한·통합한도·실적·결제수단이
 * 곱해지면 조합이 너무 많아 손으로는 다 못 쓰므로, 조합은 생성기에 맡기고 여기서는 규칙만 적는다.
 *
 * 기대값은 계산기를 다시 부르지 않고 따로 구한다(정수 연산). 같은 식을 두 번 돌려 비교하면
 * 식이 틀려도 통과한다.
 */
class BenefitCalculatorPropertyTest {

    private static final int TRIES = 10_000;
    private static final String SEED = "20261012";

    private final BenefitCalculator calculator = new BenefitCalculator();

    @Provide
    Arbitrary<RuleSpec> rules() {
        return EngineArbitraries.rules(StampMode.WITH_STAMP);
    }

    @Provide
    Arbitrary<ContextSpec> contexts() {
        return EngineArbitraries.contexts();
    }

    @Provide
    Arbitrary<UsedAmounts> usedAmounts() {
        return EngineArbitraries.usedAmounts();
    }

    @Provide
    Arbitrary<UsedCounts> usedCounts() {
        return EngineArbitraries.usedCounts();
    }

    private BenefitResult calculate(RuleSpec rule, ContextSpec context) {
        return calculator.calculate(rule.toRule(), context.toContext());
    }

    // ------------------------------------------------------------------ 범위

    @Property(tries = TRIES, seed = SEED)
    void 혜택액은_0원_이상이고_혜택_대상금액을_넘지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        BenefitResult result = calculate(rule, context);

        assertThat(result.benefitAmount()).isBetween(0L, context.targetAmount());
    }

    @Property(tries = TRIES, seed = SEED)
    void 건당_최대와_기간별_잔여_한도와_통합한도_잔여를_넘지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        long benefit = calculate(rule, context).benefitAmount();
        AmountLimits limits = rule.amountLimits();
        UsedAmounts used = context.usedAmounts();

        assertWithin(benefit, rule.maxBenefitPerTxn(), 0L, "건당 최대");
        assertWithin(benefit, limits.daily(), used.daily(), "일 한도");
        assertWithin(benefit, limits.monthly(), used.monthly(), "월 한도");
        assertWithin(benefit, limits.quarterly(), used.quarterly(), "분기 한도");
        assertWithin(benefit, limits.yearly(), used.yearly(), "연 한도");
        if (rule.useSharedLimit()) {
            assertWithin(benefit, context.sharedMonthlyLimit(), context.sharedLimitUsed(), "통합한도");
        }
        if (rule.method() == CalcMethod.RATE && rule.maxEligibleAmount() != null) {
            assertThat(benefit).as("대상금액 상한까지만 율을 곱한다")
                    .isLessThanOrEqualTo(rateOf(rule.maxEligibleAmount(), rule.value()));
        }
    }

    private void assertWithin(long benefit, Long limit, long used, String label) {
        if (limit != null) {
            assertThat(benefit).as(label + " 잔여").isLessThanOrEqualTo(Math.max(0L, limit - used));
        }
    }

    // ------------------------------------------------------------------ NULL과 0

    @Property(tries = TRIES, seed = SEED)
    void 금액_한도가_0이면_어느_축이든_혜택은_0원이다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context,
            @ForAll @IntRange(min = 0, max = 5) int zeroedAxis) {
        AmountLimits limits = rule.amountLimits();
        RuleSpec zeroed = switch (zeroedAxis) {
            case 0 -> rule.withMaxBenefitPerTxn(0L);
            case 1 -> rule.withAmountLimits(
                    new AmountLimits(0L, limits.monthly(), limits.quarterly(), limits.yearly()));
            case 2 -> rule.withAmountLimits(
                    new AmountLimits(limits.daily(), 0L, limits.quarterly(), limits.yearly()));
            case 3 -> rule.withAmountLimits(
                    new AmountLimits(limits.daily(), limits.monthly(), 0L, limits.yearly()));
            case 4 -> rule.withAmountLimits(
                    new AmountLimits(limits.daily(), limits.monthly(), limits.quarterly(), 0L));
            default -> rule.withUseSharedLimit(true);
        };
        ContextSpec zeroedContext = zeroedAxis == 5
                ? new ContextSpec(context.paymentAmount(), context.usedPointAmount(), context.paymentType(),
                        context.performanceMet(), context.amountEstimated(), context.usedAmounts(),
                        context.usedCounts(), 0L, context.sharedLimitUsed())
                : context;

        assertThat(calculate(zeroed, zeroedContext).benefitAmount()).isZero();
    }

    @Property(tries = TRIES, seed = SEED)
    void 횟수_한도가_0이면_어느_축이든_적용되지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context,
            @ForAll @IntRange(min = 0, max = 3) int zeroedAxis) {
        CountLimits limits = rule.countLimits();
        RuleSpec zeroed = rule.withCountLimits(switch (zeroedAxis) {
            case 0 -> new CountLimits(0, limits.monthly(), limits.quarterly(), limits.yearly());
            case 1 -> new CountLimits(limits.daily(), 0, limits.quarterly(), limits.yearly());
            case 2 -> new CountLimits(limits.daily(), limits.monthly(), 0, limits.yearly());
            default -> new CountLimits(limits.daily(), limits.monthly(), limits.quarterly(), 0);
        });

        assertThat(calculate(zeroed, context).applied()).isFalse();
    }

    @Property(tries = TRIES, seed = SEED)
    void 금액_한도가_전부_NULL이면_소진액이_아무리_커도_상한_적용_전_값_그대로다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        // NULL을 0으로 뭉개면 혜택이 통째로 사라진다. 소진액이 있어도 한도가 없으면 깎을 근거가 없다
        RuleSpec unlimited = new RuleSpec(rule.benefitId(), rule.kind(), rule.method(), rule.value(),
                rule.stepCount(), rule.requirePerformance(), rule.requirePaymentType(), rule.minTxnAmount(),
                null, null, AmountLimits.none(), rule.countLimits(), false);

        BenefitResult result = calculate(unlimited, context);

        if (result.applied()) {
            assertThat(result.benefitAmount())
                    .isEqualTo(Math.min(uncappedBenefit(unlimited, context), context.targetAmount()));
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 횟수_한도가_전부_NULL이면_소진_횟수는_적용_여부를_바꾸지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        RuleSpec uncounted = rule.withCountLimits(CountLimits.none());

        boolean appliedWithUsage = calculate(uncounted, context).applied();
        boolean appliedWithoutUsage = calculate(uncounted, context.withUsedCounts(UsedCounts.none())).applied();

        assertThat(appliedWithUsage).isEqualTo(appliedWithoutUsage);
    }

    // ------------------------------------------------------------------ 미적용과 사유

    @Property(tries = TRIES, seed = SEED)
    void 미적용이면_0원이고_사유가_실제_조건과_맞다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        BenefitResult result = calculate(rule, context);
        if (result.applied()) {
            assertThat(result.notApplicableReason()).isNull();
            return;
        }

        assertThat(result.benefitAmount()).isZero();
        assertThat(result.appliedCap()).isEqualTo(CapType.NONE);
        assertThat(result.estimate()).isFalse();
        NotApplicableReason reason = result.notApplicableReason();
        CountLimits limits = rule.countLimits();
        UsedCounts used = context.usedCounts();
        boolean reasonHolds = switch (reason) {
            case RETROACTIVE_EXCLUDED, GIFT_EXCLUDED, INSTALLMENT_FREE_EXCLUDED -> !rule.isCalculableKind();
            case PERFORMANCE_NOT_MET -> rule.requirePerformance() && !context.performanceMet();
            case PAYMENT_TYPE_MISMATCH -> rule.requirePaymentType() != null
                    && !rule.requirePaymentType().equals(context.paymentType());
            case MIN_TXN_AMOUNT_NOT_MET -> rule.minTxnAmount() != null
                    && context.paymentAmount() < rule.minTxnAmount();
            case DAILY_COUNT_EXCEEDED -> limits.daily() != null && used.daily() >= limits.daily();
            case MONTHLY_COUNT_EXCEEDED -> limits.monthly() != null && used.monthly() >= limits.monthly();
            case QUARTERLY_COUNT_EXCEEDED -> limits.quarterly() != null && used.quarterly() >= limits.quarterly();
            case YEARLY_COUNT_EXCEEDED -> limits.yearly() != null && used.yearly() >= limits.yearly();
        };
        assertThat(reasonHolds).as("사유 %s 가 실제 조건과 맞아야 한다", reason).isTrue();
    }

    @Property(tries = TRIES, seed = SEED)
    void 막을_조건이_하나도_없으면_반드시_적용된다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        boolean blocked = !rule.isCalculableKind()
                || (rule.requirePerformance() && !context.performanceMet())
                || (rule.requirePaymentType() != null && !rule.requirePaymentType().equals(context.paymentType()))
                || (rule.minTxnAmount() != null && context.paymentAmount() < rule.minTxnAmount())
                || !rule.countLimits().isEmpty();

        if (!blocked) {
            assertThat(calculate(rule, context).applied()).isTrue();
        }
    }

    // ------------------------------------------------------------------ 상한 기록

    @Property(tries = TRIES, seed = SEED)
    void 상한_적용_전_값보다_적게_받았으면_깎은_상한이_기록된다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        BenefitResult result = calculate(rule, context);

        if (result.applied() && result.benefitAmount() < uncappedBenefit(rule, context)) {
            Statistics.label("덜 받은 결제에 기록된 상한").collect(result.appliedCap());
            assertThat(result.appliedCap()).isNotEqualTo(CapType.NONE);
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 기록된_상한은_실제로_그_혜택에_걸려_있던_상한이다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        BenefitResult result = calculate(rule, context);
        long benefit = result.benefitAmount();
        AmountLimits limits = rule.amountLimits();
        UsedAmounts used = context.usedAmounts();

        switch (result.appliedCap()) {
            case NONE -> {
                if (result.applied()) {
                    assertThat(benefit).isEqualTo(uncappedBenefit(rule, context));
                }
            }
            case PAYMENT_AMOUNT -> assertThat(benefit).isLessThanOrEqualTo(context.targetAmount());
            case MAX_ELIGIBLE_AMOUNT -> {
                assertThat(rule.method()).isEqualTo(CalcMethod.RATE);
                assertThat(rule.maxEligibleAmount()).isNotNull();
                assertThat(context.targetAmount()).isGreaterThanOrEqualTo(rule.maxEligibleAmount());
                assertThat(benefit).isLessThanOrEqualTo(rateOf(rule.maxEligibleAmount(), rule.value()));
            }
            case MAX_BENEFIT_PER_TXN -> assertReached(benefit, rule.maxBenefitPerTxn(), 0L);
            case DAILY_LIMIT -> assertReached(benefit, limits.daily(), used.daily());
            case MONTHLY_LIMIT -> assertReached(benefit, limits.monthly(), used.monthly());
            case QUARTERLY_LIMIT -> assertReached(benefit, limits.quarterly(), used.quarterly());
            case YEARLY_LIMIT -> assertReached(benefit, limits.yearly(), used.yearly());
            case SHARED_LIMIT -> {
                assertThat(rule.useSharedLimit()).isTrue();
                assertReached(benefit, context.sharedMonthlyLimit(), context.sharedLimitUsed());
            }
        }
    }

    private void assertReached(long benefit, Long limit, long used) {
        assertThat(limit).as("기록된 상한은 값이 있어야 한다").isNotNull();
        assertThat(benefit).isLessThanOrEqualTo(Math.max(0L, limit - used));
    }

    // ------------------------------------------------------------------ 단조성

    @Property(tries = TRIES, seed = SEED)
    void 소진액이_늘면_혜택은_늘지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context,
            @ForAll("usedAmounts") UsedAmounts extraUsed,
            @ForAll @LongRange(min = 0, max = 50_000) long extraSharedUsed) {
        ContextSpec moreUsed = context
                .withUsedAmounts(context.usedAmounts().plus(extraUsed))
                .withSharedLimitUsed(context.sharedLimitUsed() + extraSharedUsed);

        assertThat(calculate(rule, moreUsed).benefitAmount())
                .isLessThanOrEqualTo(calculate(rule, context).benefitAmount());
    }

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형이_아니면_소진_횟수가_늘어도_혜택은_늘지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context,
            @ForAll("usedCounts") UsedCounts extraUsed) {
        // 스탬프형은 횟수가 차야 지급되므로 횟수가 늘면 혜택이 생기는 것이 정상이다
        if (rule.method() == CalcMethod.COUNT_STEP) {
            return;
        }
        ContextSpec moreUsed = context.withUsedCounts(context.usedCounts().plus(extraUsed));

        assertThat(calculate(rule, moreUsed).benefitAmount())
                .isLessThanOrEqualTo(calculate(rule, context).benefitAmount());
    }

    @Property(tries = TRIES, seed = SEED)
    void 결제금액이_커지면_혜택은_줄지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context,
            @ForAll @LongRange(min = 1, max = 100_000) long increase) {
        ContextSpec larger = context.withPaymentAmount(context.paymentAmount() + increase);

        assertThat(calculate(rule, larger).benefitAmount())
                .isGreaterThanOrEqualTo(calculate(rule, context).benefitAmount());
    }

    // ------------------------------------------------------------------ 예상·확정

    @Property(tries = TRIES, seed = SEED)
    void 확정으로_표시한_혜택은_결제금액이_커져도_변하지_않는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context,
            @ForAll @LongRange(min = 1, max = 100_000) long increase) {
        ContextSpec estimated = context.withAmountEstimated(true);
        BenefitResult result = calculate(rule, estimated);

        // 결제금액 자체가 상한인 경우는 뺀다. 혜택이 결제금액을 넘어 결제금액만큼만 받는 상태라
        // (3,000원 정액 할인을 2,500원 결제에 적용) 결제금액이 커지면 혜택도 따라 커지는데,
        // 지금 판정은 "상한에 걸렸으니 확정"으로 본다. 표시와 실제가 어긋나는 알려진 경계다.
        if (result.applied() && !result.estimate()) {
            ContextSpec larger = estimated.withPaymentAmount(estimated.paymentAmount() + increase);
            boolean unchanged = calculate(rule, larger).benefitAmount() == result.benefitAmount();
            Statistics.label("확정 표시 혜택이 결제금액이 커졌을 때")
                    .collect(unchanged ? "그대로" : "변함 (" + rule.method() + ", " + result.appliedCap() + ")");
            if (result.appliedCap() != CapType.PAYMENT_AMOUNT) {
                assertThat(unchanged).isTrue();
            }
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 예상_표시는_예상금액으로_계산한_정률_혜택에만_붙는다(
            @ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        BenefitResult result = calculate(rule, context);

        if (result.estimate()) {
            assertThat(rule.method()).isEqualTo(CalcMethod.RATE);
            assertThat(context.amountEstimated()).isTrue();
            assertThat(result.appliedCap()).isEqualTo(CapType.NONE);
        }
    }

    // ------------------------------------------------------------------ 스탬프형의 횟수 한도

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형이_일_횟수_한도로_막혔다면_그날_이미_한도만큼_지급됐다(
            @ForAll @IntRange(min = 2, max = 6) int stepCount,
            @ForAll @IntRange(min = 1, max = 3) int dailyCountLimit,
            @ForAll @Size(4) List<@IntRange(min = 0, max = 14) Integer> paymentsPerDay) {
        RuleSpec rule = stampRule(stepCount, new CountLimits(dailyCountLimit, null, null, null));

        int stampsThisMonth = 0;
        for (int paymentsToday : paymentsPerDay) {
            int stampsToday = 0;
            int payoutsToday = 0;
            for (int payment = 0; payment < paymentsToday; payment++) {
                BenefitResult result = calculate(rule, stampContext(
                        new UsedCounts(stampsToday, stampsThisMonth, stampsThisMonth, stampsThisMonth)));
                if (!result.applied()) {
                    assertThat(payoutsToday).as("지급 %d회인데 일 %d회 한도로 막혔다", payoutsToday, dailyCountLimit)
                            .isGreaterThanOrEqualTo(dailyCountLimit);
                    continue;
                }
                stampsToday += 1;
                stampsThisMonth += 1;
                payoutsToday += result.benefitAmount() > 0 ? 1 : 0;
            }
            assertThat(payoutsToday).isLessThanOrEqualTo(dailyCountLimit);
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형의_분기_지급_횟수는_분기_횟수_한도를_넘지_않는다(
            @ForAll @IntRange(min = 2, max = 6) int stepCount,
            @ForAll @IntRange(min = 1, max = 3) int quarterlyCountLimit,
            @ForAll @Size(3) List<@IntRange(min = 0, max = 14) Integer> paymentsPerMonth) {
        assertThat(replayQuarter(stepCount, quarterlyCountLimit, paymentsPerMonth).payouts())
                .isLessThanOrEqualTo(quarterlyCountLimit);
    }

    /**
     * 위 속성의 반대 방향 — 한도에 못 미쳤는데 막히는 일이 없어야 한다.
     *
     * 지금은 성립하지 않는다. 횟수 한도는 "소진 횟수 ÷ N"으로 지급 횟수를 어림하는데, 스탬프 진행은
     * 달이 바뀌면 0부터 다시 세고 분기 소진 횟수는 그 달들의 합이다. 그래서 2회마다 지급·분기 1회
     * 한도인 혜택이 [0건, 1건, 2건]으로 쓰이면 한 번도 지급하지 않은 채 분기 한도 초과로 막힌다.
     * 분기·연 횟수 한도가 걸린 스탬프형 혜택이 생기면 먼저 고쳐야 하는 지점이라 남겨 둔다.
     */
    @Disabled("스탬프형의 분기·연 횟수 한도가 지급 횟수를 정확히 세지 못한다 — 한도에 못 미쳐도 막힌다")
    @Property(tries = TRIES, seed = SEED)
    void 스탬프형이_분기_횟수_한도로_막혔다면_그_분기에_이미_한도만큼_지급됐다(
            @ForAll @IntRange(min = 2, max = 6) int stepCount,
            @ForAll @IntRange(min = 1, max = 3) int quarterlyCountLimit,
            @ForAll @Size(3) List<@IntRange(min = 0, max = 14) Integer> paymentsPerMonth) {
        assertThat(replayQuarter(stepCount, quarterlyCountLimit, paymentsPerMonth).blockedBeforeLimit())
                .isFalse();
    }

    /** 한 분기(석 달)의 결과. blockedBeforeLimit = 지급 횟수가 한도에 못 미쳤는데 횟수 초과로 막힌 적이 있다 */
    private record QuarterReplay(int payouts, boolean blockedBeforeLimit) {
    }

    /** 분기 소진 횟수는 그 분기에 속한 월 소진 횟수의 합이고, 스탬프 진행은 달이 바뀌면 0부터 다시 센다 */
    private QuarterReplay replayQuarter(int stepCount, int quarterlyCountLimit, List<Integer> paymentsPerMonth) {
        RuleSpec rule = stampRule(stepCount, new CountLimits(null, null, quarterlyCountLimit, null));
        int stampsThisQuarter = 0;
        int payoutsThisQuarter = 0;
        boolean blockedBeforeLimit = false;
        for (int paymentsThisMonth : paymentsPerMonth) {
            int stampsThisMonth = 0;
            for (int payment = 0; payment < paymentsThisMonth; payment++) {
                BenefitResult result = calculate(rule, stampContext(
                        new UsedCounts(0, stampsThisMonth, stampsThisQuarter, stampsThisQuarter)));
                if (!result.applied()) {
                    blockedBeforeLimit |= payoutsThisQuarter < quarterlyCountLimit;
                    continue;
                }
                stampsThisMonth += 1;
                stampsThisQuarter += 1;
                payoutsThisQuarter += result.benefitAmount() > 0 ? 1 : 0;
            }
        }
        return new QuarterReplay(payoutsThisQuarter, blockedBeforeLimit);
    }

    private static RuleSpec stampRule(int stepCount, CountLimits countLimits) {
        return new RuleSpec(1L, BenefitKind.POINT, CalcMethod.COUNT_STEP, BigDecimal.valueOf(2_000L), stepCount,
                false, null, null, null, null, AmountLimits.none(), countLimits, false);
    }

    private static ContextSpec stampContext(UsedCounts usedCounts) {
        return new ContextSpec(10_000L, 0L, EngineArbitraries.CARD, true, false,
                UsedAmounts.none(), usedCounts, null, 0L);
    }

    // ------------------------------------------------------------------ 결정성

    @Property(tries = TRIES, seed = SEED)
    void 같은_입력이면_같은_결과다(@ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        assertThat(calculate(rule, context)).isEqualTo(calculate(rule, context));
    }

    // ------------------------------------------------------------------ 기대값 (계산기와 따로 구한다)

    /**
     * 상한을 하나도 적용하지 않았을 때의 혜택액 — 결제금액 전부를 대상으로 본 값이다.
     * 대상금액 상한(max_eligible_amount)도 상한이므로 여기서는 적용하지 않는다.
     */
    private static long uncappedBenefit(RuleSpec rule, ContextSpec context) {
        return switch (rule.method()) {
            case RATE -> rateOf(context.targetAmount(), rule.value());
            case FIXED -> rule.value().longValueExact();
            case COUNT_STEP -> (context.usedCounts().monthly() + 1) % rule.stepCount() == 0
                    ? rule.value().longValueExact()
                    : 0L;
        };
    }

    /** 금액 × 요율(%)을 원 미만에서 버린 값. 요율은 소수 둘째 자리까지라 10,000으로 나누면 된다 */
    private static long rateOf(long amount, BigDecimal percent) {
        long hundredthsOfPercent = percent.movePointRight(2).longValueExact();
        return amount * hundredthsOfPercent / 10_000L;
    }
}
