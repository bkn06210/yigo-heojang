package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CapType;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardBenefitSelectorTest {

    private static final long CAFE_CATEGORY_ID = 102L;
    private static final long STARBUCKS_MERCHANT_ID = 11L;

    private final CardBenefitSelector selector = new CardBenefitSelector();

    private BenefitRule.Builder rate(long benefitId, String percent) {
        return BenefitRule.builder()
                .benefitId(benefitId)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal(percent));
    }

    private BenefitCandidate merchantBenefit(BenefitRule rule) {
        return BenefitCandidate.builder()
                .targetType(TargetType.MERCHANT)
                .targetMerchantId(STARBUCKS_MERCHANT_ID)
                .rule(rule)
                .build();
    }

    private BenefitCandidate categoryBenefit(BenefitRule rule) {
        return BenefitCandidate.builder()
                .targetType(TargetType.CATEGORY)
                .targetCategoryId(CAFE_CATEGORY_ID)
                .rule(rule)
                .build();
    }

    private PaymentRequest starbucksPayment(long amount) {
        PaymentTarget target = PaymentTarget.builder()
                .merchantId(STARBUCKS_MERCHANT_ID)
                .merchantCode("STARBUCKS")
                .categoryId(CAFE_CATEGORY_ID)
                .categoryCode("CAFE")
                .build();
        return PaymentRequest.estimated(target, amount, "CARD");
    }

    /** 실적 충족(구간 최소 40만원), 통합한도 없음, 소진 없음 */
    private CardState cardState(BenefitUsage... usages) {
        return new CardState(metStatus(null), 0L, List.of(usages));
    }

    /** 실적 충족 구간 — min_performance_amount > 0이 곧 충족이다 */
    private static PerformanceStatus metStatus(Long sharedMonthlyLimit) {
        return new PerformanceStatus(1L, 400000L, sharedMonthlyLimit);
    }

    /** 실적 미충족 구간 — 0원 구간에 걸린 상태 */
    private static PerformanceStatus notMetStatus() {
        return new PerformanceStatus(2L, 0L, null);
    }

    @Nested
    @DisplayName("혜택 1개 선택")
    class Selection {

        @Test
        void 매칭된_혜택_중_금액이_큰_하나만_적용된다() {
            // 스타벅스 10% 할인 vs 카페 5% 할인 → 합산하지 않고 큰 쪽만
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").build()),
                    categoryBenefit(rate(2L, "5.00").build()));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isEqualTo(1000L);
            assertThat(selection.benefitKind()).isEqualTo(BenefitKind.DISCOUNT);
        }

        @Test
        void 금액이_같으면_benefitId가_작은_혜택을_고른다() {
            List<BenefitCandidate> candidates = List.of(
                    categoryBenefit(rate(7L, "5.00").build()),
                    merchantBenefit(rate(3L, "5.00").build()));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(3L);
        }

        @Test
        void 매칭되는_혜택이_없으면_혜택_없음이다() {
            List<BenefitCandidate> candidates = List.of(
                    BenefitCandidate.builder()
                            .targetType(TargetType.MERCHANT)
                            .targetMerchantId(999L)
                            .rule(rate(1L, "10.00").build())
                            .build());

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.hasBenefit()).isFalse();
            assertThat(selection.benefitAmount()).isZero();
        }

        @Test
        void 혜택이_하나도_없는_카드는_혜택_없음이다() {
            CardBenefitSelection selection =
                    selector.select(List.of(), cardState(), starbucksPayment(10000L));

            assertThat(selection.hasBenefit()).isFalse();
        }

        @Test
        void 게이트에_걸린_혜택은_후보에서_빠진다() {
            // 실적 미충족 → 실적 조건 혜택은 빠지고, 조건 없는 혜택이 선택된다
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "20.00").requirePerformance(true).build()),
                    categoryBenefit(rate(2L, "5.00").requirePerformance(false).build()));

            CardState notMet = new CardState(notMetStatus(), 0L, List.of());
            CardBenefitSelection selection = selector.select(candidates, notMet, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }

        @Test
        void 사후정산_혜택은_추천에서_빠진다() {
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "20.00").benefitKind(BenefitKind.RETROACTIVE).build()),
                    categoryBenefit(rate(2L, "5.00").build()));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
        }

        @Test
        void 한도_소진으로_0원이어도_적용된_혜택으로_남는다() {
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").monthlyLimit(5000L).build()));

            CardBenefitSelection selection = selector.select(candidates,
                    cardState(BenefitUsage.ofMonthly(1L, 5000L, 1, 0L, 0)), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isZero();
            assertThat(selection.appliedCap()).isEqualTo(CapType.MONTHLY_LIMIT);
        }

        @Test
        void 한도가_찬_혜택보다_적게_남은_다른_혜택이_이긴다() {
            // 동적 전환의 카드 내부 판본 — 10% 혜택 한도가 차면 5% 혜택이 선택된다
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").monthlyLimit(5000L).build()),
                    categoryBenefit(rate(2L, "5.00").build()));

            CardBenefitSelection selection = selector.select(candidates,
                    cardState(BenefitUsage.ofMonthly(1L, 5000L, 1, 0L, 0)), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("묶음 한도")
    class GroupLimit {

        @Test
        void 같은_그룹의_다른_혜택_소진액이_한도에서_차감된다() {
            // "카페·편의점 각 10%, 합쳐서 월 5천원" — 편의점에서 이미 4천원을 썼다
            BenefitCandidate cafe = categoryBenefit(
                    rate(1L, "10.00").monthlyLimit(5000L).build());
            BenefitCandidate convenience = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(201L)
                    .limitGroupCode("LIVING")
                    .rule(rate(2L, "10.00").monthlyLimit(5000L).build())
                    .build();
            BenefitCandidate cafeGrouped = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(CAFE_CATEGORY_ID)
                    .limitGroupCode("LIVING")
                    .rule(rate(1L, "10.00").monthlyLimit(5000L).build())
                    .build();

            CardState state = cardState(
                    BenefitUsage.ofMonthly(1L, 0L, 0, 0L, 0),
                    BenefitUsage.ofMonthly(2L, 4000L, 2, 0L, 0));

            CardBenefitSelection grouped = selector.select(
                    List.of(cafeGrouped, convenience), state, starbucksPayment(50000L));
            CardBenefitSelection ungrouped = selector.select(
                    List.of(cafe, convenience), state, starbucksPayment(50000L));

            // 묶였으면 그룹 잔여 1,000원까지만
            assertThat(grouped.benefitAmount()).isEqualTo(1000L);
            // 안 묶였으면 자기 소진(0원)만 보므로 한도 5,000원을 다 쓴다
            assertThat(ungrouped.benefitAmount()).isEqualTo(5000L);
        }

        @Test
        void 그룹의_다른_혜택이_이번_결제에_매칭되지_않아도_소진액은_반영된다() {
            // 통신 혜택은 카페 결제에 매칭되지 않지만, 같은 그룹 한도를 이미 갉아먹었다
            BenefitCandidate cafe = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(CAFE_CATEGORY_ID)
                    .limitGroupCode("LIVING")
                    .rule(rate(1L, "10.00").monthlyLimit(5000L).build())
                    .build();
            BenefitCandidate telecom = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(401L)
                    .limitGroupCode("LIVING")
                    .rule(rate(2L, "10.00").monthlyLimit(5000L).build())
                    .build();

            CardState state = cardState(BenefitUsage.ofMonthly(2L, 4500L, 1, 0L, 0));

            CardBenefitSelection selection =
                    selector.select(List.of(cafe, telecom), state, starbucksPayment(50000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }

        @Test
        void 다른_그룹의_소진액은_영향을_주지_않는다() {
            BenefitCandidate cafe = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(CAFE_CATEGORY_ID)
                    .limitGroupCode("FOOD")
                    .rule(rate(1L, "10.00").monthlyLimit(5000L).build())
                    .build();
            BenefitCandidate telecom = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(401L)
                    .limitGroupCode("LIVING")
                    .rule(rate(2L, "10.00").monthlyLimit(5000L).build())
                    .build();

            CardState state = cardState(BenefitUsage.ofMonthly(2L, 4500L, 1, 0L, 0));

            CardBenefitSelection selection =
                    selector.select(List.of(cafe, telecom), state, starbucksPayment(50000L));

            assertThat(selection.benefitAmount()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("카드 통합한도")
    class SharedLimit {

        @Test
        void 통합한도_잔여가_카드의_모든_혜택을_제한한다() {
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").useSharedLimit(true).build()),
                    categoryBenefit(rate(2L, "5.00").useSharedLimit(true).build()));

            CardState state = new CardState(metStatus(20000L), 19700L, List.of());
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(50000L));

            assertThat(selection.benefitAmount()).isEqualTo(300L);
            assertThat(selection.appliedCap()).isEqualTo(CapType.SHARED_LIMIT);
        }

        @Test
        void 통합한도를_안_쓰는_혜택은_통합한도가_차도_받는다() {
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").useSharedLimit(true).build()),
                    categoryBenefit(rate(2L, "5.00").useSharedLimit(false).build()));

            CardState state = new CardState(metStatus(20000L), 20000L, List.of());
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("실적 기간 축 (performance_period)")
    class PerformancePeriodAxis {

        private BenefitCandidate quarterBenefit(BenefitRule rule) {
            return BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(STARBUCKS_MERCHANT_ID)
                    .performancePeriod(PerformancePeriod.QUARTER)
                    .rule(rule)
                    .build();
        }

        /**
         * 한 카드가 전월 축과 전분기 축을 함께 쓴다. 축을 섞으면 전분기 100만원 조건이
         * 전월 금액으로 판정돼, 충족한 회원이 미충족으로 읽힌다.
         */
        @Test
        void 전월은_미충족이어도_전분기를_충족하면_분기_혜택이_적용된다() {
            List<BenefitCandidate> candidates = List.of(
                    quarterBenefit(rate(1L, "10.00").requirePerformance(true).build()));

            CardState state = new CardState(notMetStatus(), metStatus(null), 0L, List.of(), null);
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isEqualTo(1000L);
        }

        @Test
        void 전월을_충족해도_전분기가_미충족이면_분기_혜택은_빠진다() {
            List<BenefitCandidate> candidates = List.of(
                    quarterBenefit(rate(1L, "10.00").requirePerformance(true).build()),
                    categoryBenefit(rate(2L, "5.00").requirePerformance(true).build()));

            CardState state = new CardState(metStatus(null), notMetStatus(), 0L, List.of(), null);
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
        }

        /** 분기 구간표가 없는 카드에 분기 혜택이 달렸으면 충족했다고 볼 근거가 없다 */
        @Test
        void 분기_구간표가_없으면_분기_실적_조건은_미충족이다() {
            List<BenefitCandidate> candidates = List.of(
                    quarterBenefit(rate(1L, "10.00").requirePerformance(true).build()));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.hasBenefit()).isFalse();
        }

        @Test
        void 실적_조건이_없는_분기_혜택은_구간표와_무관하게_적용된다() {
            List<BenefitCandidate> candidates = List.of(
                    quarterBenefit(rate(1L, "10.00").requirePerformance(false).build()));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.benefitAmount()).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("횟수 묶음 (count_group_code)")
    class CountGroup {

        private BenefitCandidate merchantBenefitIn(BenefitRule rule, String limitGroup, String countGroup) {
            return BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(STARBUCKS_MERCHANT_ID)
                    .limitGroupCode(limitGroup)
                    .countGroupCode(countGroup)
                    .rule(rule)
                    .build();
        }

        private BenefitCandidate categoryBenefitIn(BenefitRule rule, String limitGroup, String countGroup) {
            return BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(CAFE_CATEGORY_ID)
                    .limitGroupCode(limitGroup)
                    .countGroupCode(countGroup)
                    .rule(rule)
                    .build();
        }

        /**
         * "영화관 3사 합쳐 연 4회"처럼 대상마다 행을 쪼갠 혜택은 횟수도 묶어야 한다.
         * 안 묶으면 각 행이 따로 4회씩 세어 연 12회가 나간다.
         */
        @Test
        void 같은_횟수_묶음의_다른_혜택이_쓴_횟수도_한도에_반영된다() {
            BenefitCandidate target = merchantBenefitIn(
                    rate(1L, "10.00").yearlyCountLimit(4).build(), null, "MOVIE");
            BenefitCandidate sibling = categoryBenefitIn(
                    rate(2L, "1.00").yearlyCountLimit(4).build(), null, "MOVIE");

            CardState state = cardState(
                    new BenefitUsage(1L, 0L, 0, 0L, 0, 0L, 0, 0L, 1),
                    new BenefitUsage(2L, 0L, 0, 0L, 0, 0L, 0, 0L, 3));

            CardBenefitSelection selection =
                    selector.select(List.of(target, sibling), state, starbucksPayment(10000L));

            // 그룹 합산 4회 = 연 한도 도달 → 두 혜택 모두 적용 불가
            assertThat(selection.hasBenefit()).isFalse();
        }

        @Test
        void 횟수_묶음이_다르면_서로의_횟수를_침범하지_않는다() {
            BenefitCandidate target = merchantBenefitIn(
                    rate(1L, "10.00").yearlyCountLimit(4).build(), null, "MOVIE");
            BenefitCandidate other = categoryBenefitIn(
                    rate(2L, "1.00").yearlyCountLimit(4).build(), null, "CAFE");

            CardState state = cardState(
                    new BenefitUsage(1L, 0L, 0, 0L, 0, 0L, 0, 0L, 1),
                    new BenefitUsage(2L, 0L, 0, 0L, 0, 0L, 0, 0L, 3));

            CardBenefitSelection selection =
                    selector.select(List.of(target, other), state, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
        }

        /**
         * 금액 묶음과 횟수 묶음의 범위가 다를 수 있다.
         * count_group_code가 없으면 limit_group_code를 따르고, 있으면 그쪽이 우선한다.
         */
        @Test
        void 횟수_묶음_코드가_없으면_금액_묶음을_따른다() {
            BenefitCandidate target = merchantBenefitIn(
                    rate(1L, "10.00").monthlyCountLimit(2).build(), "DAILY_PACK", null);
            BenefitCandidate sibling = categoryBenefitIn(
                    rate(2L, "1.00").monthlyCountLimit(2).build(), "DAILY_PACK", null);

            CardState state = cardState(
                    BenefitUsage.ofMonthly(1L, 0L, 1, 0L, 0),
                    BenefitUsage.ofMonthly(2L, 0L, 1, 0L, 0));

            CardBenefitSelection selection =
                    selector.select(List.of(target, sibling), state, starbucksPayment(10000L));

            assertThat(selection.hasBenefit()).isFalse();
        }

        @Test
        void 금액은_금액_묶음_횟수는_횟수_묶음으로_따로_합산한다() {
            // 금액은 셋이 월 5,000원을 나눠 쓰고, 횟수는 이 혜택만 따로 센다
            BenefitCandidate target = merchantBenefitIn(
                    rate(1L, "10.00").monthlyLimit(5000L).monthlyCountLimit(2).build(), "PACK", "MOVIE");
            BenefitCandidate sibling = categoryBenefitIn(
                    rate(2L, "1.00").monthlyLimit(5000L).monthlyCountLimit(2).build(), "PACK", "CAFE");

            CardState state = cardState(
                    BenefitUsage.ofMonthly(1L, 1000L, 1, 0L, 0),
                    BenefitUsage.ofMonthly(2L, 3500L, 1, 0L, 0));

            CardBenefitSelection selection =
                    selector.select(List.of(target, sibling), state, starbucksPayment(50000L));

            // 금액: 그룹 합산 4,500 소진 → 잔여 500. 횟수: 묶음이 달라 1회씩이라 한도(2회) 미달
            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("선택형 혜택 (option_group_code)")
    class OptionGroup {

        private BenefitCandidate optionBenefit(BenefitRule rule, String groupCode, String optionKey) {
            return BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(STARBUCKS_MERCHANT_ID)
                    .optionGroupCode(groupCode)
                    .optionKey(optionKey)
                    .rule(rule)
                    .build();
        }

        private CardState stateWithSelection(String groupCode, String optionKey) {
            return new CardState(metStatus(null), null, 0L, List.of(), java.util.Map.of(groupCode, optionKey));
        }

        @Test
        void 그달에_고른_선택지의_혜택만_적용된다() {
            List<BenefitCandidate> candidates = List.of(
                    optionBenefit(rate(1L, "20.00").build(), "PACK", "MEDICAL"),
                    optionBenefit(rate(2L, "5.00").build(), "PACK", "LIVING"));

            CardBenefitSelection selection = selector.select(
                    candidates, stateWithSelection("PACK", "LIVING"), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }

        /** 기본값으로 아무거나 켜면 회원이 고르지 않은 혜택을 받은 것으로 기록된다 */
        @Test
        void 아무것도_고르지_않은_달은_그_묶음이_전부_빠진다() {
            List<BenefitCandidate> candidates = List.of(
                    optionBenefit(rate(1L, "20.00").build(), "PACK", "MEDICAL"),
                    optionBenefit(rate(2L, "5.00").build(), "PACK", "LIVING"));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.hasBenefit()).isFalse();
        }

        /** 선택지 하나가 혜택 여러 개로 이뤄지므로 혜택 id가 아니라 option_key로 고른다 */
        @Test
        void 한_선택지에_묶인_혜택들이_함께_켜진다() {
            List<BenefitCandidate> candidates = List.of(
                    optionBenefit(rate(1L, "3.00").build(), "PACK", "DELIVERY"),
                    optionBenefit(rate(2L, "7.00").build(), "PACK", "DELIVERY"),
                    optionBenefit(rate(3L, "20.00").build(), "PACK", "MEDICAL"));

            CardBenefitSelection selection = selector.select(
                    candidates, stateWithSelection("PACK", "DELIVERY"), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.benefitAmount()).isEqualTo(700L);
        }

        @Test
        void 선택형이_아닌_혜택은_선택_기록과_무관하게_적용된다() {
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").build()),
                    optionBenefit(rate(2L, "20.00").build(), "PACK", "MEDICAL"));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("스탬프 진행 (COUNT_STEP)")
    class StampProgress {

        private BenefitCandidate stamp(long benefitId, int stepCount, String countGroup) {
            return BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(STARBUCKS_MERCHANT_ID)
                    .countGroupCode(countGroup)
                    .rule(BenefitRule.builder()
                            .benefitId(benefitId)
                            .benefitKind(BenefitKind.POINT)
                            .calcMethod(CalcMethod.COUNT_STEP)
                            .benefitValue(new BigDecimal("2000.00"))
                            .stepCount(stepCount)
                            .build())
                    .build();
        }

        /**
         * 스탬프는 그 결제에서 다른 혜택을 받았는지와 무관하게 찍힌다.
         * 선택된 혜택만 기록하면 진행 횟수가 안 쌓여 N회째가 영영 오지 않는다.
         */
        @Test
        void 다른_혜택이_선택돼도_스탬프는_진행으로_기록된다() {
            List<BenefitCandidate> candidates = List.of(
                    stamp(1L, 5, null),
                    merchantBenefit(rate(2L, "10.00").build()));

            CardBenefitSelection selection =
                    selector.select(candidates, cardState(), starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.stampedBenefitIds()).containsExactly(1L);
        }

        @Test
        void 스탬프가_차는_결제는_스탬프가_선택되고_진행에도_남는다() {
            List<BenefitCandidate> candidates = List.of(
                    stamp(1L, 5, null),
                    merchantBenefit(rate(2L, "10.00").build()));

            CardState state = cardState(BenefitUsage.ofMonthly(1L, 0L, 4, 0L, 0));
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isEqualTo(2000L);
            assertThat(selection.stampedBenefitIds()).containsExactly(1L);
        }

        @Test
        void 매칭되지_않는_스탬프는_진행에_남지_않는다() {
            BenefitCandidate elsewhere = BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(999L)
                    .rule(BenefitRule.builder()
                            .benefitId(1L)
                            .benefitKind(BenefitKind.POINT)
                            .calcMethod(CalcMethod.COUNT_STEP)
                            .benefitValue(new BigDecimal("2000.00"))
                            .stepCount(5)
                            .build())
                    .build();

            CardBenefitSelection selection =
                    selector.select(List.of(elsewhere), cardState(), starbucksPayment(10000L));

            assertThat(selection.stampedBenefitIds()).isEmpty();
        }

        /** 편의점 4사를 합쳐 5회 — 브랜드마다 행이 갈려도 진행은 하나로 센다 */
        @Test
        void 횟수_묶음으로_묶인_스탬프는_형제의_진행도_합산한다() {
            BenefitCandidate here = stamp(1L, 5, "STAMP_CVS");
            BenefitCandidate sibling = BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(999L)
                    .countGroupCode("STAMP_CVS")
                    .rule(BenefitRule.builder()
                            .benefitId(2L)
                            .benefitKind(BenefitKind.POINT)
                            .calcMethod(CalcMethod.COUNT_STEP)
                            .benefitValue(new BigDecimal("2000.00"))
                            .stepCount(5)
                            .build())
                    .build();

            // 형제 브랜드에서 이미 4번 찍혔다 → 이번이 5번째
            CardState state = cardState(BenefitUsage.ofMonthly(2L, 0L, 4, 0L, 0));
            CardBenefitSelection selection = selector.select(List.of(here, sibling), state,
                    starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(1L);
            assertThat(selection.benefitAmount()).isEqualTo(2000L);
        }
    }
}
