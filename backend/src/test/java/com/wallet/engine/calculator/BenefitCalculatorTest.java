package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitResult;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcContext;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CapType;
import com.wallet.engine.model.NotApplicableReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BenefitCalculatorTest {

    private final BenefitCalculator calculator = new BenefitCalculator();

    /** 10% 할인, 제약 없음 */
    private BenefitRule.Builder rateRule() {
        return BenefitRule.builder()
                .benefitId(1L)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal("10.00"));
    }

    /** 1,000원 정액 할인, 제약 없음 */
    private BenefitRule.Builder fixedRule() {
        return BenefitRule.builder()
                .benefitId(2L)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.FIXED)
                .benefitValue(new BigDecimal("1000.00"));
    }

    /** 1만원 결제, 실적 충족, 소진 없음, 구간 입력(예상) */
    private CalcContext.Builder context() {
        return CalcContext.builder()
                .paymentAmount(10000L)
                .performanceMet(true)
                .amountEstimated(true);
    }

    @Nested
    @DisplayName("게이트 — 적용 자체를 막는 조건")
    class Gates {

        @Test
        void 사후정산_혜택은_추천_계산에서_제외된다() {
            BenefitRule rule = rateRule().benefitKind(BenefitKind.RETROACTIVE).build();

            BenefitResult result = calculator.calculate(rule, context().build());

            assertThat(result.applied()).isFalse();
            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.RETROACTIVE_EXCLUDED);
        }

        @Test
        void 증정_혜택은_추천_계산에서_제외된다() {
            BenefitRule rule = rateRule().benefitKind(BenefitKind.GIFT).build();

            BenefitResult result = calculator.calculate(rule, context().build());

            // 적용·0원으로 두면 다른 혜택이 없는 카드에서 선택돼 결제와 무관한 혜택이 기록된다
            assertThat(result.applied()).isFalse();
            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.GIFT_EXCLUDED);
        }

        @Test
        void 실적조건이_걸린_혜택은_실적_미충족이면_미적용이다() {
            BenefitRule rule = rateRule().requirePerformance(true).build();

            BenefitResult result = calculator.calculate(rule, context().performanceMet(false).build());

            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.PERFORMANCE_NOT_MET);
        }

        @Test
        void 실적조건이_없으면_실적_미충족이어도_적용된다() {
            BenefitRule rule = rateRule().requirePerformance(false).build();

            BenefitResult result = calculator.calculate(rule, context().performanceMet(false).build());

            assertThat(result.applied()).isTrue();
            assertThat(result.benefitAmount()).isEqualTo(1000L);
        }

        @Test
        void 결제수단_조건이_걸렸는데_수단_미입력이면_미적용이다() {
            BenefitRule rule = rateRule().requirePaymentType("SIMPLE_PAY").build();

            BenefitResult result = calculator.calculate(rule, context().paymentType(null).build());

            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.PAYMENT_TYPE_MISMATCH);
        }

        @Test
        void 결제수단이_다르면_미적용이다() {
            BenefitRule rule = rateRule().requirePaymentType("SIMPLE_PAY").build();

            BenefitResult result = calculator.calculate(rule, context().paymentType("CARD").build());

            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.PAYMENT_TYPE_MISMATCH);
        }

        @Test
        void 결제수단이_일치하면_적용된다() {
            BenefitRule rule = rateRule().requirePaymentType("SIMPLE_PAY").build();

            BenefitResult result = calculator.calculate(rule, context().paymentType("SIMPLE_PAY").build());

            assertThat(result.applied()).isTrue();
        }

        @Test
        void 건당_최소금액은_같으면_적용_미달이면_미적용이다() {
            BenefitRule rule = rateRule().minTxnAmount(10000L).build();

            assertThat(calculator.calculate(rule, context().paymentAmount(10000L).build()).applied()).isTrue();
            assertThat(calculator.calculate(rule, context().paymentAmount(9999L).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.MIN_TXN_AMOUNT_NOT_MET);
        }

        @Test
        void 일_횟수_한도에_도달하면_미적용이다() {
            BenefitRule rule = rateRule().dailyCountLimit(1).build();

            assertThat(calculator.calculate(rule, context().dailyUsedCount(0).build()).applied()).isTrue();
            assertThat(calculator.calculate(rule, context().dailyUsedCount(1).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.DAILY_COUNT_EXCEEDED);
        }

        @Test
        void 월_횟수_한도에_도달하면_미적용이다() {
            BenefitRule rule = rateRule().monthlyCountLimit(3).build();

            assertThat(calculator.calculate(rule, context().monthlyUsedCount(2).build()).applied()).isTrue();
            assertThat(calculator.calculate(rule, context().monthlyUsedCount(3).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.MONTHLY_COUNT_EXCEEDED);
        }

        @Test
        void 분기_횟수_한도에_도달하면_미적용이다() {
            BenefitRule rule = rateRule().quarterlyCountLimit(2).build();

            assertThat(calculator.calculate(rule, context().quarterlyUsedCount(1).build()).applied()).isTrue();
            assertThat(calculator.calculate(rule, context().quarterlyUsedCount(2).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.QUARTERLY_COUNT_EXCEEDED);
        }

        @Test
        void 연_횟수_한도에_도달하면_미적용이다() {
            BenefitRule rule = rateRule().yearlyCountLimit(4).build();

            assertThat(calculator.calculate(rule, context().yearlyUsedCount(3).build()).applied()).isTrue();
            assertThat(calculator.calculate(rule, context().yearlyUsedCount(4).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.YEARLY_COUNT_EXCEEDED);
        }

        /**
         * "영화관 3사 합쳐 연 4회"를 월 4회로 옮겨 적으면 매달 리셋되어 연 48회가 된다.
         * 축이 실제로 분리돼 있는지를 고정한다 — 연 소진이 차 있어도 월 소진이 비면 통과하면 안 된다.
         */
        @Test
        void 월_횟수가_남아도_연_횟수가_차면_미적용이다() {
            BenefitRule rule = rateRule().monthlyCountLimit(2).yearlyCountLimit(4).build();

            BenefitResult result = calculator.calculate(
                    rule, context().monthlyUsedCount(0).yearlyUsedCount(4).build());

            assertThat(result.applied()).isFalse();
            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.YEARLY_COUNT_EXCEEDED);
        }

        @Test
        void 횟수_한도_NULL은_무제한이고_0은_항상_미적용이다() {
            BenefitRule unlimited = rateRule().monthlyCountLimit(null).build();
            BenefitRule zero = rateRule().monthlyCountLimit(0).build();

            assertThat(calculator.calculate(unlimited, context().monthlyUsedCount(999).build()).applied()).isTrue();
            assertThat(calculator.calculate(zero, context().monthlyUsedCount(0).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.MONTHLY_COUNT_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("대상금액과 혜택 계산")
    class Calculation {

        @Test
        void 대상금액_상한이_있으면_그_금액까지만_혜택_대상이다() {
            BenefitRule rule = rateRule().maxEligibleAmount(30000L).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(50000L).build());

            assertThat(result.benefitAmount()).isEqualTo(3000L);
        }

        @Test
        void 대상금액_상한이_없으면_결제금액_전체가_대상이다() {
            BenefitRule rule = rateRule().maxEligibleAmount(null).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(50000L).build());

            assertThat(result.benefitAmount()).isEqualTo(5000L);
        }

        @Test
        void 대상금액_상한이_결제금액보다_크면_결제금액_기준이다() {
            BenefitRule rule = rateRule().maxEligibleAmount(100000L).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(50000L).build());

            assertThat(result.benefitAmount()).isEqualTo(5000L);
        }

        @Test
        void 정률_계산은_원_미만을_버린다() {
            BenefitRule rule = rateRule().benefitValue(new BigDecimal("10.00")).build();

            // 12,345 × 10% = 1,234.5 → 1,234
            BenefitResult result = calculator.calculate(rule, context().paymentAmount(12345L).build());

            assertThat(result.benefitAmount()).isEqualTo(1234L);
        }

        @Test
        void 소수_둘째자리_율도_정확히_계산한다() {
            BenefitRule rule = rateRule().benefitValue(new BigDecimal("1.25")).build();

            // 33,333 × 1.25% = 416.6625 → 416
            BenefitResult result = calculator.calculate(rule, context().paymentAmount(33333L).build());

            assertThat(result.benefitAmount()).isEqualTo(416L);
        }

        @Test
        void 포인트로_낸_부분에는_혜택이_붙지_않는다() {
            BenefitRule rule = rateRule().build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).usedPointAmount(20000L).build());

            assertThat(result.benefitAmount()).isEqualTo(3000L);
        }

        @Test
        void 정액_혜택이_결제금액보다_크면_결제금액까지만_할인된다() {
            BenefitRule rule = fixedRule().benefitValue(new BigDecimal("5000.00")).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(3000L).build());

            assertThat(result.benefitAmount()).isEqualTo(3000L);
            assertThat(result.appliedCap()).isEqualTo(CapType.PAYMENT_AMOUNT);
        }
    }

    @Nested
    @DisplayName("상한 클램프")
    class Caps {

        @Test
        void 건당_최대혜택액으로_깎인다() {
            BenefitRule rule = rateRule().maxBenefitPerTxn(2000L).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(50000L).build());

            assertThat(result.benefitAmount()).isEqualTo(2000L);
            assertThat(result.appliedCap()).isEqualTo(CapType.MAX_BENEFIT_PER_TXN);
        }

        @Test
        void 월_한도가_일부_소진됐으면_잔여만큼만_받는다() {
            BenefitRule rule = rateRule().monthlyLimit(10000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).monthlyUsedAmount(8000L).build());

            assertThat(result.benefitAmount()).isEqualTo(2000L);
            assertThat(result.appliedCap()).isEqualTo(CapType.MONTHLY_LIMIT);
        }

        @Test
        void 월_한도가_전액_소진됐으면_적용되지만_0원이다() {
            BenefitRule rule = rateRule().monthlyLimit(10000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().monthlyUsedAmount(10000L).build());

            assertThat(result.applied()).isTrue();
            assertThat(result.benefitAmount()).isZero();
            assertThat(result.notApplicableReason()).isNull();
            assertThat(result.appliedCap()).isEqualTo(CapType.MONTHLY_LIMIT);
        }

        @Test
        void 소진이_한도를_넘어도_음수가_아니라_0원이다() {
            BenefitRule rule = rateRule().monthlyLimit(10000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().monthlyUsedAmount(15000L).build());

            assertThat(result.benefitAmount()).isZero();
        }

        @Test
        void 월_한도_NULL은_제약없음이고_0은_혜택없음이다() {
            BenefitRule unlimited = rateRule().monthlyLimit(null).build();
            BenefitRule zero = rateRule().monthlyLimit(0L).build();

            assertThat(calculator.calculate(unlimited, context().paymentAmount(1000000L).build()).benefitAmount())
                    .isEqualTo(100000L);
            assertThat(calculator.calculate(zero, context().build()).benefitAmount()).isZero();
        }

        @Test
        void 일_한도_NULL은_제약없음이고_0은_혜택없음이다() {
            BenefitRule unlimited = rateRule().dailyLimit(null).build();
            BenefitRule zero = rateRule().dailyLimit(0L).build();

            assertThat(calculator.calculate(unlimited, context().build()).benefitAmount()).isEqualTo(1000L);
            assertThat(calculator.calculate(zero, context().build()).benefitAmount()).isZero();
        }

        @Test
        void 분기_한도가_일부_소진됐으면_잔여만큼만_받는다() {
            BenefitRule rule = rateRule().quarterlyLimit(30000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).quarterlyUsedAmount(29000L).build());

            assertThat(result.benefitAmount()).isEqualTo(1000L);
            assertThat(result.appliedCap()).isEqualTo(CapType.QUARTERLY_LIMIT);
        }

        @Test
        void 연_한도가_일부_소진됐으면_잔여만큼만_받는다() {
            BenefitRule rule = rateRule().yearlyLimit(100000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).yearlyUsedAmount(98000L).build());

            assertThat(result.benefitAmount()).isEqualTo(2000L);
            assertThat(result.appliedCap()).isEqualTo(CapType.YEARLY_LIMIT);
        }

        /**
         * 분기 한도를 월 필드에 옮겨 적으면 매달 리셋되어 실제의 세 배가 나간다.
         * 월 소진이 비어 있어도 분기 소진이 차 있으면 막혀야 축이 분리된 것이다.
         */
        @Test
        void 월_잔여가_남아도_분기_잔여가_없으면_0원이다() {
            BenefitRule rule = rateRule().monthlyLimit(30000L).quarterlyLimit(60000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).monthlyUsedAmount(0L).quarterlyUsedAmount(60000L).build());

            assertThat(result.benefitAmount()).isZero();
            assertThat(result.appliedCap()).isEqualTo(CapType.QUARTERLY_LIMIT);
        }

        @Test
        void 분기_연_한도_NULL은_제약없음이고_0은_혜택없음이다() {
            BenefitRule unlimited = rateRule().quarterlyLimit(null).yearlyLimit(null).build();
            BenefitRule quarterlyZero = rateRule().quarterlyLimit(0L).build();
            BenefitRule yearlyZero = rateRule().yearlyLimit(0L).build();

            assertThat(calculator.calculate(unlimited, context().build()).benefitAmount()).isEqualTo(1000L);
            assertThat(calculator.calculate(quarterlyZero, context().build()).benefitAmount()).isZero();
            assertThat(calculator.calculate(yearlyZero, context().build()).benefitAmount()).isZero();
        }

        @Test
        void 통합_한도_NULL은_통합한도_없는_카드다() {
            BenefitRule rule = rateRule().useSharedLimit(true).build();

            BenefitResult result = calculator.calculate(rule,
                    context().sharedMonthlyLimit(null).sharedLimitUsed(999999L).build());

            assertThat(result.benefitAmount()).isEqualTo(1000L);
        }

        @Test
        void 통합_한도를_안_쓰는_혜택은_통합_소진과_무관하다() {
            BenefitRule rule = rateRule().useSharedLimit(false).build();

            BenefitResult result = calculator.calculate(rule,
                    context().sharedMonthlyLimit(20000L).sharedLimitUsed(20000L).build());

            assertThat(result.benefitAmount()).isEqualTo(1000L);
        }

        @Test
        void 통합_한도를_쓰는_혜택은_통합_잔여로_깎인다() {
            BenefitRule rule = rateRule().useSharedLimit(true).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).sharedMonthlyLimit(20000L).sharedLimitUsed(19500L).build());

            assertThat(result.benefitAmount()).isEqualTo(500L);
            assertThat(result.appliedCap()).isEqualTo(CapType.SHARED_LIMIT);
        }

        @Test
        void 여러_상한_중_가장_작은_잔여가_적용된다() {
            BenefitRule dailyTighter = rateRule().dailyLimit(1000L).monthlyLimit(30000L).build();
            BenefitRule monthlyTighter = rateRule().dailyLimit(30000L).monthlyLimit(1000L).build();

            BenefitResult daily = calculator.calculate(dailyTighter, context().paymentAmount(50000L).build());
            BenefitResult monthly = calculator.calculate(monthlyTighter, context().paymentAmount(50000L).build());

            assertThat(daily.benefitAmount()).isEqualTo(1000L);
            assertThat(daily.appliedCap()).isEqualTo(CapType.DAILY_LIMIT);
            assertThat(monthly.benefitAmount()).isEqualTo(1000L);
            assertThat(monthly.appliedCap()).isEqualTo(CapType.MONTHLY_LIMIT);
        }

        @Test
        void 일_잔여와_월_잔여가_같으면_먼저_검사한_일_한도를_기록한다() {
            BenefitRule rule = rateRule().dailyLimit(2000L).monthlyLimit(2000L).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(50000L).build());

            assertThat(result.benefitAmount()).isEqualTo(2000L);
            assertThat(result.appliedCap()).isEqualTo(CapType.DAILY_LIMIT);
        }

        @Test
        void 묶음_한도는_호출자가_넣어준_그룹_합산_소진액으로_판정된다() {
            // 그룹 한도 5,000원 중 다른 혜택이 이미 4,500원을 썼다고 가정
            BenefitRule rule = rateRule().monthlyLimit(5000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).monthlyUsedAmount(4500L).build());

            assertThat(result.benefitAmount()).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("COUNT_STEP — N회마다 정액 지급(스탬프형)")
    class CountStep {

        /** 5회마다 2,000원. 시드의 편의점 Stamp와 같은 형태 */
        private BenefitRule.Builder stampRule() {
            return BenefitRule.builder()
                    .benefitId(3L)
                    .benefitKind(BenefitKind.POINT)
                    .calcMethod(CalcMethod.COUNT_STEP)
                    .benefitValue(new BigDecimal("2000.00"))
                    .stepCount(5);
        }

        @Test
        void 스탬프가_안_찼으면_적용되지만_0원이다() {
            BenefitRule rule = stampRule().build();

            BenefitResult result = calculator.calculate(rule, context().monthlyUsedCount(2).build());

            assertThat(result.applied()).isTrue();
            assertThat(result.benefitAmount()).isZero();
        }

        @Test
        void 이번_결제로_스탬프가_차면_정액을_지급한다() {
            BenefitRule rule = stampRule().build();

            // 이미 4번 찍혔고 이번이 5번째
            BenefitResult result = calculator.calculate(rule, context().monthlyUsedCount(4).build());

            assertThat(result.benefitAmount()).isEqualTo(2000L);
        }

        /** FIXED로 넣으면 결제마다 지급되어 실제의 step_count배가 나간다 — 그 차이를 고정한다 */
        @Test
        void 다섯_번의_결제_중_한_번만_지급된다() {
            BenefitRule rule = stampRule().build();

            long total = 0;
            for (int stamped = 0; stamped < 5; stamped++) {
                total += calculator.calculate(rule, context().monthlyUsedCount(stamped).build()).benefitAmount();
            }

            assertThat(total).isEqualTo(2000L);
        }

        @Test
        void 두_바퀴째도_같은_주기로_지급된다() {
            BenefitRule rule = stampRule().build();

            assertThat(calculator.calculate(rule, context().monthlyUsedCount(8).build()).benefitAmount()).isZero();
            assertThat(calculator.calculate(rule, context().monthlyUsedCount(9).build()).benefitAmount())
                    .isEqualTo(2000L);
        }

        /**
         * 횟수 한도의 단위는 "받은 횟수"다. 스탬프 소진 횟수를 그대로 비교하면
         * 두 번째 결제에서 막혀 스탬프를 채울 기회 자체가 사라진다.
         */
        @Test
        void 월_지급횟수_한도는_스탬프_진행_횟수가_아니라_지급_횟수로_판정한다() {
            BenefitRule rule = stampRule().monthlyCountLimit(1).build();

            // 4번 찍힌 상태 — 아직 한 번도 지급 안 됨. 막히면 안 된다
            assertThat(calculator.calculate(rule, context().monthlyUsedCount(4).build()).benefitAmount())
                    .isEqualTo(2000L);
            // 5번 찍혀 이미 1회 지급 — 월 1회 한도에 걸린다
            assertThat(calculator.calculate(rule, context().monthlyUsedCount(5).build()).notApplicableReason())
                    .isEqualTo(NotApplicableReason.MONTHLY_COUNT_EXCEEDED);
        }

        @Test
        void 건당_최소금액에_미달하면_스탬프가_찍히지_않는다() {
            BenefitRule rule = stampRule().minTxnAmount(5000L).build();

            BenefitResult result = calculator.calculate(
                    rule, context().paymentAmount(4900L).monthlyUsedCount(4).build());

            assertThat(result.applied()).isFalse();
            assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.MIN_TXN_AMOUNT_NOT_MET);
        }

        @Test
        void 지급액은_결제금액을_넘지_않는다() {
            BenefitRule rule = stampRule().build();

            BenefitResult result = calculator.calculate(
                    rule, context().paymentAmount(1500L).monthlyUsedCount(4).build());

            assertThat(result.benefitAmount()).isEqualTo(1500L);
            assertThat(result.appliedCap()).isEqualTo(CapType.PAYMENT_AMOUNT);
        }

        @Test
        void 지급액은_결제금액에_비례하지_않으므로_항상_확정이다() {
            BenefitRule rule = stampRule().build();

            BenefitResult result = calculator.calculate(
                    rule, context().monthlyUsedCount(4).amountEstimated(true).build());

            assertThat(result.estimate()).isFalse();
        }

        @Test
        void stepCount가_없거나_0이면_규칙_생성에서_막힌다() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> stampRule().stepCount(null).build());
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> stampRule().stepCount(0).build());
        }
    }

    @Nested
    @DisplayName("예상 / 확정 판정")
    class EstimateFlag {

        @Test
        void 정액_혜택은_항상_확정이다() {
            BenefitResult result = calculator.calculate(fixedRule().build(), context().build());

            assertThat(result.estimate()).isFalse();
        }

        @Test
        void 정률_혜택은_구간_입력이고_상한에_안_걸리면_예상이다() {
            BenefitResult result = calculator.calculate(rateRule().build(), context().amountEstimated(true).build());

            assertThat(result.estimate()).isTrue();
        }

        @Test
        void 금액이_확정_입력이면_정률이어도_확정이다() {
            BenefitResult result = calculator.calculate(rateRule().build(), context().amountEstimated(false).build());

            assertThat(result.estimate()).isFalse();
        }

        @Test
        void 정률이어도_상한에_걸리면_확정이다() {
            BenefitRule rule = rateRule().maxBenefitPerTxn(500L).build();

            BenefitResult result = calculator.calculate(rule, context().amountEstimated(true).build());

            assertThat(result.estimate()).isFalse();
        }

        @Test
        void 계산값이_상한과_정확히_같아도_상한_도달로_본다() {
            // 10,000 × 10% = 1,000, 건당 상한도 1,000
            BenefitRule rule = rateRule().maxBenefitPerTxn(1000L).build();

            BenefitResult result = calculator.calculate(rule, context().build());

            assertThat(result.benefitAmount()).isEqualTo(1000L);
            assertThat(result.estimate()).isFalse();
            assertThat(result.appliedCap()).isEqualTo(CapType.MAX_BENEFIT_PER_TXN);
        }

        @Test
        void 절사되기_전_값으로_상한_도달을_판정한다() {
            // 12,345 × 10% = 1,234.5 → 상한 1,234에 걸린다 (절사값 1,234와 금액은 같지만 확정)
            BenefitRule rule = rateRule().maxBenefitPerTxn(1234L).build();

            BenefitResult result = calculator.calculate(rule, context().paymentAmount(12345L).build());

            assertThat(result.benefitAmount()).isEqualTo(1234L);
            assertThat(result.estimate()).isFalse();
        }

        @Test
        void 대상금액_상한을_넘긴_결제는_금액이_커져도_혜택이_고정이라_확정이다() {
            BenefitRule rule = rateRule().maxEligibleAmount(30000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(50000L).amountEstimated(true).build());

            assertThat(result.benefitAmount()).isEqualTo(3000L);
            assertThat(result.estimate()).isFalse();
        }

        @Test
        void 대상금액_상한에_못_미치면_여전히_예상이다() {
            BenefitRule rule = rateRule().maxEligibleAmount(30000L).build();

            BenefitResult result = calculator.calculate(rule,
                    context().paymentAmount(20000L).amountEstimated(true).build());

            assertThat(result.benefitAmount()).isEqualTo(2000L);
            assertThat(result.estimate()).isTrue();
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class InputValidation {

        @Test
        void rule이나_context가_null이면_예외다() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> calculator.calculate(null, context().build()));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> calculator.calculate(rateRule().build(), null));
        }
    }
}
