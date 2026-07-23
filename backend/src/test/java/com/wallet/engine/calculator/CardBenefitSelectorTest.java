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

    private CardState cardState(BenefitUsage... usages) {
        return new CardState(true, null, 0L, List.of(usages));
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

            CardState notMet = new CardState(false, null, 0L, List.of());
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
                    cardState(new BenefitUsage(1L, 5000L, 1, 0L, 0)), starbucksPayment(10000L));

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
                    cardState(new BenefitUsage(1L, 5000L, 1, 0L, 0)), starbucksPayment(10000L));

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
                    new BenefitUsage(1L, 0L, 0, 0L, 0),
                    new BenefitUsage(2L, 4000L, 2, 0L, 0));

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

            CardState state = cardState(new BenefitUsage(2L, 4500L, 1, 0L, 0));

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

            CardState state = cardState(new BenefitUsage(2L, 4500L, 1, 0L, 0));

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

            CardState state = new CardState(true, 20000L, 19700L, List.of());
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(50000L));

            assertThat(selection.benefitAmount()).isEqualTo(300L);
            assertThat(selection.appliedCap()).isEqualTo(CapType.SHARED_LIMIT);
        }

        @Test
        void 통합한도를_안_쓰는_혜택은_통합한도가_차도_받는다() {
            List<BenefitCandidate> candidates = List.of(
                    merchantBenefit(rate(1L, "10.00").useSharedLimit(true).build()),
                    categoryBenefit(rate(2L, "5.00").useSharedLimit(false).build()));

            CardState state = new CardState(true, 20000L, 20000L, List.of());
            CardBenefitSelection selection = selector.select(candidates, state, starbucksPayment(10000L));

            assertThat(selection.benefitId()).isEqualTo(2L);
            assertThat(selection.benefitAmount()).isEqualTo(500L);
        }
    }
}
