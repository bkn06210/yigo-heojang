package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PortfolioSimulation;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;
import com.wallet.engine.model.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardPortfolioSimulatorTest {

    private static final long CAFE_CATEGORY_ID = 102L;
    private static final long MART_CATEGORY_ID = 301L;

    private final CardPortfolioSimulator simulator = new CardPortfolioSimulator();

    private static PerformanceStatus metStatus() {
        return new PerformanceStatus(1L, 400000L, null);
    }

    private BenefitRule rate(long benefitId, String percent) {
        return BenefitRule.builder()
                .benefitId(benefitId)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal(percent))
                .build();
    }

    private BenefitCandidate categoryBenefit(long categoryId, BenefitRule rule) {
        return BenefitCandidate.builder()
                .targetType(TargetType.CATEGORY)
                .targetCategoryId(categoryId)
                .rule(rule)
                .build();
    }

    private SimulatedPayment payment(LocalDate date, long categoryId, String categoryCode, long amount) {
        PaymentTarget target = PaymentTarget.builder()
                .categoryId(categoryId)
                .categoryCode(categoryCode)
                .build();
        return new SimulatedPayment(date, PaymentRequest.estimated(target, amount, "CARD"));
    }

    /** 카페 10건 + 마트 10건, 하루 한 건씩 */
    private List<SimulatedPayment> month() {
        List<SimulatedPayment> payments = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 7, 1);
        for (int i = 0; i < 10; i++) {
            payments.add(payment(start.plusDays(i), CAFE_CATEGORY_ID, "CAFE", 10_000L));
            payments.add(payment(start.plusDays(i), MART_CATEGORY_ID, "MART", 20_000L));
        }
        return payments;
    }

    @Nested
    @DisplayName("결제마다 최적 카드를 고른다")
    class PerPaymentChoice {

        @Test
        void 카드를_추가하면_유리한_결제만_새_카드로_넘어간다() {
            SimulatedCard existing = new SimulatedCard(1L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(11L, "1.00")),
                    categoryBenefit(MART_CATEGORY_ID, rate(12L, "5.00"))), metStatus());
            SimulatedCard candidate = new SimulatedCard(2L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(21L, "10.00"))), metStatus());

            PortfolioSimulation before = simulator.simulate(List.of(existing), month());
            PortfolioSimulation after = simulator.simulate(List.of(existing, candidate), month());

            // 기존: 카페 1%(1,000) + 마트 5%(10,000) = 11,000
            assertThat(before.totalBenefitAmount()).isEqualTo(11_000L);
            // 추가 후: 카페는 새 카드 10%(10,000), 마트는 기존 카드 그대로(10,000) = 20,000
            assertThat(after.totalBenefitAmount()).isEqualTo(20_000L);
            assertThat(after.totalBenefitAmount() - before.totalBenefitAmount()).isEqualTo(9_000L);
        }

        @Test
        void 마트는_기존_카드가_계속_가져간다() {
            SimulatedCard existing = new SimulatedCard(1L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(11L, "1.00")),
                    categoryBenefit(MART_CATEGORY_ID, rate(12L, "5.00"))), metStatus());
            SimulatedCard candidate = new SimulatedCard(2L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(21L, "10.00"))), metStatus());

            PortfolioSimulation after = simulator.simulate(List.of(existing, candidate), month());

            // 카페를 뺏겨도 마트 10,000원은 기존 카드에 남는다
            assertThat(after.shares()).hasSize(2);
            assertThat(after.shares()).anySatisfy(share -> {
                assertThat(share.cardId()).isEqualTo(1L);
                assertThat(share.benefitAmount()).isEqualTo(10_000L);
            });
        }

        @Test
        void 카드를_추가해도_총합은_줄지_않는다() {
            // 새 카드가 어디서도 못 이기면 순증 0. 음수가 될 수 없다
            SimulatedCard existing = new SimulatedCard(1L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(11L, "10.00")),
                    categoryBenefit(MART_CATEGORY_ID, rate(12L, "10.00"))), metStatus());
            SimulatedCard weak = new SimulatedCard(2L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(21L, "1.00"))), metStatus());

            long before = simulator.simulate(List.of(existing), month()).totalBenefitAmount();
            long after = simulator.simulate(List.of(existing, weak), month()).totalBenefitAmount();

            assertThat(after).isEqualTo(before);
        }
    }

    @Nested
    @DisplayName("소진은 고른 카드만 올린다")
    class UsageOnWinnerOnly {

        @Test
        void 안_쓴_카드의_한도는_닳지_않는다() {
            // 카페 10건. 새 카드가 월 한도 3,000원까지만 이기고 그 뒤엔 기존 카드가 가져가야 한다
            SimulatedCard existing = new SimulatedCard(1L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(11L, "1.00"))), metStatus());
            BenefitRule capped = BenefitRule.builder()
                    .benefitId(21L)
                    .benefitKind(BenefitKind.DISCOUNT)
                    .calcMethod(CalcMethod.RATE)
                    .benefitValue(new BigDecimal("10.00"))
                    .monthlyLimit(3_000L)
                    .build();
            SimulatedCard candidate = new SimulatedCard(2L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, capped)), metStatus());

            List<SimulatedPayment> cafeOnly = new ArrayList<>();
            LocalDate start = LocalDate.of(2026, 7, 1);
            for (int i = 0; i < 10; i++) {
                cafeOnly.add(payment(start.plusDays(i), CAFE_CATEGORY_ID, "CAFE", 10_000L));
            }

            PortfolioSimulation result = simulator.simulate(List.of(existing, candidate), cafeOnly);

            // 새 카드 3,000원(한도) + 남은 7건은 기존 카드 1% × 7 = 700원
            assertThat(result.totalBenefitAmount()).isEqualTo(3_700L);
            assertThat(result.shares()).anySatisfy(share -> {
                assertThat(share.cardId()).isEqualTo(2L);
                assertThat(share.benefitAmount()).isEqualTo(3_000L);
            });
        }
    }

    @Nested
    @DisplayName("경계")
    class Edges {

        @Test
        void 카드가_없으면_빈_결과다() {
            assertThat(simulator.simulate(List.of(), month()).totalBenefitAmount()).isZero();
        }

        @Test
        void 소비가_없으면_빈_결과다() {
            SimulatedCard card = new SimulatedCard(1L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(11L, "1.00"))), metStatus());

            assertThat(simulator.simulate(List.of(card), List.of()).totalBenefitAmount()).isZero();
        }

        @Test
        void 동점이면_cardId가_작은_카드가_가져간다() {
            SimulatedCard first = new SimulatedCard(1L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(11L, "5.00"))), metStatus());
            SimulatedCard second = new SimulatedCard(2L, List.of(
                    categoryBenefit(CAFE_CATEGORY_ID, rate(21L, "5.00"))), metStatus());

            PortfolioSimulation result = simulator.simulate(List.of(first, second),
                    List.of(payment(LocalDate.of(2026, 7, 1), CAFE_CATEGORY_ID, "CAFE", 10_000L)));

            assertThat(result.shares().get(0).cardId()).isEqualTo(1L);
            assertThat(result.shares().get(0).benefitAmount()).isEqualTo(500L);
        }
    }
}
