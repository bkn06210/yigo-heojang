package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.SimulatedPayment;
import com.wallet.engine.model.SpendingSimulation;
import com.wallet.engine.model.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CardSpendingSimulatorTest {

    private static final long CAFE_CATEGORY_ID = 102L;

    private final CardSpendingSimulator simulator = new CardSpendingSimulator();

    /** 실적 충족 구간 — min_performance_amount > 0이 곧 충족이다 */
    private static PerformanceStatus metStatus(Long sharedMonthlyLimit) {
        return new PerformanceStatus(1L, 400000L, sharedMonthlyLimit);
    }

    private BenefitRule.Builder cafeRate(long benefitId, String percent) {
        return BenefitRule.builder()
                .benefitId(benefitId)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal(percent));
    }

    private BenefitCandidate cafeBenefit(BenefitRule rule) {
        return BenefitCandidate.builder()
                .targetType(TargetType.CATEGORY)
                .targetCategoryId(CAFE_CATEGORY_ID)
                .rule(rule)
                .build();
    }

    private SimulatedPayment cafePayment(LocalDate date, long amount) {
        PaymentTarget target = PaymentTarget.builder()
                .categoryId(CAFE_CATEGORY_ID)
                .categoryCode("CAFE")
                .build();
        return new SimulatedPayment(date, PaymentRequest.estimated(target, amount, "CARD"));
    }

    /** 카페에 amount원씩 count번 — 하루 한 번씩 흩어 쓴다 */
    private List<SimulatedPayment> cafeMonth(long amount, int count) {
        List<SimulatedPayment> payments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            payments.add(cafePayment(LocalDate.of(2026, 7, 1).plusDays(i), amount));
        }
        return payments;
    }

    private SpendingSimulation simulate(List<BenefitCandidate> candidates, List<SimulatedPayment> payments) {
        return simulator.simulate(candidates, metStatus(null), null, Map.of(), payments);
    }

    @Nested
    @DisplayName("총액에 요율을 곱한 값과 다르다")
    class NotJustRateTimesTotal {

        @Test
        void 건당_최대가_걸리면_그만큼_깎인다() {
            // 카페 10% (건당 최대 300원). 5천원씩 40번 = 요율만 보면 20,000원
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").maxBenefitPerTxn(300L).build()));

            SpendingSimulation result = simulate(candidates, cafeMonth(5_000L, 40));

            // 건당 500원이 나올 것을 300원으로 깎아 40번
            assertThat(result.totalBenefitAmount()).isEqualTo(12_000L);
        }

        @Test
        void 월_한도가_걸리면_한도까지만_받는다() {
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").monthlyLimit(5_000L).build()));

            SpendingSimulation result = simulate(candidates, cafeMonth(5_000L, 40));

            assertThat(result.totalBenefitAmount()).isEqualTo(5_000L);
        }

        @Test
        void 월_횟수가_걸리면_앞의_몇_건만_받는다() {
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").monthlyCountLimit(5).build()));

            SpendingSimulation result = simulate(candidates, cafeMonth(5_000L, 40));

            assertThat(result.totalBenefitAmount()).isEqualTo(2_500L);
        }

        @Test
        void 건당_최소금액에_미달하면_한_건도_못_받는다() {
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").minTxnAmount(10_000L).build()));

            SpendingSimulation result = simulate(candidates, cafeMonth(5_000L, 40));

            assertThat(result.totalBenefitAmount()).isZero();
            assertThat(result.breakdowns()).isEmpty();
        }

        @Test
        void 같은_총액이라도_결제_건수가_다르면_혜택이_다르다() {
            // 이 시뮬레이터가 필요한 이유 — 총액만으로는 답이 정해지지 않는다
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").monthlyCountLimit(5).build()));

            long manySmall = simulate(candidates, cafeMonth(5_000L, 40)).totalBenefitAmount();
            long fewLarge = simulate(candidates, cafeMonth(50_000L, 4)).totalBenefitAmount();

            assertThat(manySmall).isEqualTo(2_500L);   // 5회 x 500원
            assertThat(fewLarge).isEqualTo(20_000L);   // 4회 x 5,000원
            assertThat(fewLarge).isGreaterThan(manySmall);
        }
    }

    @Nested
    @DisplayName("소진 누적")
    class Accumulation {

        @Test
        void 일_한도는_날짜가_바뀌면_리셋된다() {
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").dailyLimit(500L).build()));
            LocalDate day1 = LocalDate.of(2026, 7, 1);
            LocalDate day2 = LocalDate.of(2026, 7, 2);
            List<SimulatedPayment> payments = List.of(
                    cafePayment(day1, 10_000L), cafePayment(day1, 10_000L),
                    cafePayment(day2, 10_000L), cafePayment(day2, 10_000L));

            SpendingSimulation result = simulator.simulate(
                    candidates, metStatus(null), null, Map.of(), payments);

            // 하루 500원씩 이틀. 리셋이 없으면 500원에 머문다
            assertThat(result.totalBenefitAmount()).isEqualTo(1_000L);
        }

        @Test
        void 통합한도는_카드_전체에_걸린다() {
            BenefitCandidate cafe = cafeBenefit(
                    cafeRate(1L, "10.00").useSharedLimit(true).build());

            SpendingSimulation result = simulator.simulate(
                    List.of(cafe), metStatus(3_000L), null, Map.of(), cafeMonth(10_000L, 10));

            assertThat(result.totalBenefitAmount()).isEqualTo(3_000L);
        }

        @Test
        void 혜택별_내역을_금액_큰_순서로_돌려준다() {
            BenefitCandidate small = cafeBenefit(cafeRate(1L, "1.00").build());
            BenefitCandidate big = BenefitCandidate.builder()
                    .targetType(TargetType.ALL)
                    .rule(cafeRate(2L, "3.00").build())
                    .build();

            SpendingSimulation result = simulate(List.of(small, big), cafeMonth(10_000L, 3));

            // 한 결제에 혜택 하나만 적용되므로 큰 쪽만 쌓인다
            assertThat(result.breakdowns()).hasSize(1);
            assertThat(result.breakdowns().get(0).benefitId()).isEqualTo(2L);
            assertThat(result.breakdowns().get(0).appliedCount()).isEqualTo(3);
            assertThat(result.totalBenefitAmount()).isEqualTo(900L);
        }
    }

    @Nested
    @DisplayName("경계")
    class Edges {

        @Test
        void 소비가_없으면_빈_결과다() {
            SpendingSimulation result =
                    simulate(List.of(cafeBenefit(cafeRate(1L, "10.00").build())), List.of());

            assertThat(result.totalBenefitAmount()).isZero();
            assertThat(result.breakdowns()).isEmpty();
        }

        @Test
        void 혜택이_없는_카드는_0원이다() {
            SpendingSimulation result = simulate(List.of(), cafeMonth(10_000L, 5));

            assertThat(result.totalBenefitAmount()).isZero();
        }

        @Test
        void 날짜가_뒤섞여_들어와도_날짜순으로_처리한다() {
            // 일 한도 판정이 입력 순서에 좌우되면 같은 소비에 다른 답이 나온다
            List<BenefitCandidate> candidates = List.of(
                    cafeBenefit(cafeRate(1L, "10.00").dailyLimit(500L).build()));
            LocalDate day1 = LocalDate.of(2026, 7, 1);
            LocalDate day2 = LocalDate.of(2026, 7, 2);
            List<SimulatedPayment> shuffled = List.of(
                    cafePayment(day2, 10_000L), cafePayment(day1, 10_000L),
                    cafePayment(day2, 10_000L), cafePayment(day1, 10_000L));

            SpendingSimulation result = simulator.simulate(
                    candidates, metStatus(null), null, Map.of(), shuffled);

            assertThat(result.totalBenefitAmount()).isEqualTo(1_000L);
        }
    }
}
