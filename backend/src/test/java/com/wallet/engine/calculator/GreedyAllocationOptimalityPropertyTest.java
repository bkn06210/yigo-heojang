package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineArbitraries.StampMode;
import com.wallet.engine.calculator.EngineSpecs.CardSpec;
import com.wallet.engine.calculator.EngineSpecs.PaymentSpec;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 탐욕 배분(결제마다 그 순간 최대인 카드)과 최적 배분(모든 배분 중 최대)의 차이를 잰다.
 *
 * 결제 수가 적으면 모든 배분을 직접 돌려 볼 수 있다(카드 3장 · 결제 6건이면 729가지).
 * 그 최댓값을 기준으로 삼아 탐욕 배분이 얼마나 자주, 얼마나 손해 보는지를 센다.
 *
 * 검사하는 것은 어느 배분 방식에서도 참이어야 하는 것뿐이다 — 최적은 탐욕보다 나쁠 수 없고,
 * 최적끼리는 카드를 더해도 줄지 않는다. 탐욕 배분의 손해 자체는 실패로 치지 않고 집계해 찍는다.
 * 수치는 이 생성기가 만든 사례에서의 비율이며 실제 회원 소비에서의 비율이 아니다.
 */
class GreedyAllocationOptimalityPropertyTest {

    private static final int TRIES = 3_000;
    private static final String SEED = "20261012";

    private final CardPortfolioSimulator simulator = new CardPortfolioSimulator();
    private final Tally tally = new Tally();

    /** 보유 카드 1~2장에 후보 한 장을 더하는 상황 */
    record Scenario(List<CardSpec> held, CardSpec candidate, List<PaymentSpec> payments) {

        List<CardSpec> all() {
            List<CardSpec> all = new ArrayList<>(held);
            all.add(candidate);
            return all;
        }

        boolean hasStampBenefit() {
            return all().stream().anyMatch(CardSpec::hasStampBenefit);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("\n  보유:");
            held.forEach(card -> builder.append("\n    ").append(card));
            builder.append("\n  후보:\n    ").append(candidate).append("\n  결제:");
            payments.forEach(payment -> builder.append("\n    ").append(payment));
            return builder.toString();
        }
    }

    private static Arbitrary<Scenario> scenarios(StampMode stampMode) {
        return Combinators.combine(
                EngineArbitraries.cardLists(2, 3, stampMode, 2), EngineArbitraries.paymentLists(2, 6)
        ).as((cards, payments) -> new Scenario(
                cards.subList(0, cards.size() - 1), cards.get(cards.size() - 1), payments));
    }

    @Provide
    Arbitrary<Scenario> scenariosWithoutStamp() {
        return scenarios(StampMode.WITHOUT_STAMP);
    }

    @Provide
    Arbitrary<Scenario> scenariosWithStamp() {
        return scenarios(StampMode.WITH_STAMP).filter(Scenario::hasStampBenefit);
    }

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형이_없는_카드들에서_탐욕_배분과_최적_배분을_견준다(
            @ForAll("scenariosWithoutStamp") Scenario scenario) {
        measure(scenario);
    }

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형이_있는_카드들에서_탐욕_배분과_최적_배분을_견준다(
            @ForAll("scenariosWithStamp") Scenario scenario) {
        measure(scenario);
    }

    private void measure(Scenario scenario) {
        List<SimulatedCard> held = EngineSpecs.toSimulatedCards(scenario.held());
        List<SimulatedCard> withCandidate = EngineSpecs.toSimulatedCards(scenario.all());
        List<SimulatedPayment> payments = EngineSpecs.toPayments(scenario.payments());

        long heldGreedy = simulator.simulate(held, payments).totalBenefitAmount();
        long withGreedy = simulator.simulate(withCandidate, payments).totalBenefitAmount();
        long heldOptimal = PortfolioAllocations.optimalTotal(held, payments);
        long withOptimal = PortfolioAllocations.optimalTotal(withCandidate, payments);
        long heldStampAware =
                PortfolioAllocations.greedyTotal(held, payments, PortfolioAllocations.WITH_STAMP_PROGRESS);
        long withStampAware =
                PortfolioAllocations.greedyTotal(withCandidate, payments, PortfolioAllocations.WITH_STAMP_PROGRESS);

        // 실험용 배분기가 시뮬레이터와 같은 누적 규칙을 쓰는지부터 확인한다. 다르면 아래 비교가 무의미하다
        assertThat(PortfolioAllocations.greedyTotal(withCandidate, payments, PortfolioAllocations.IMMEDIATE_BENEFIT))
                .isEqualTo(withGreedy);
        // 탐욕 배분도 가능한 배분 중 하나이므로 최적보다 클 수 없다
        assertThat(withOptimal).isGreaterThanOrEqualTo(withGreedy);
        assertThat(heldOptimal).isGreaterThanOrEqualTo(heldGreedy);
        assertThat(withOptimal).isGreaterThanOrEqualTo(withStampAware);
        // 후보를 한 번도 쓰지 않는 배분도 후보 포함 배분 중 하나다. 그래서 최적끼리는 줄지 않는다
        assertThat(withOptimal).isGreaterThanOrEqualTo(heldOptimal);

        tally.add(heldGreedy, withGreedy, heldOptimal, withOptimal, heldStampAware, withStampAware);
    }

    @AfterProperty
    void report() {
        tally.print();
    }

    /** 집계. 손실률 = (최적 − 실제) ÷ 최적 */
    private static final class Tally {
        private int scenarios;
        private int scenariosWithBenefit;
        private final Loss greedy = new Loss();
        private final Loss stampAware = new Loss();
        private int greedyGainNegative;
        private int greedyGainMissed;
        private int greedyGainOverstated;
        private int greedyGainUnderstated;
        private int stampAwareGainNegative;
        private int stampAwareGainMissed;

        void add(long heldGreedy, long withGreedy, long heldOptimal, long withOptimal,
                 long heldStampAware, long withStampAware) {
            scenarios += 1;
            if (withOptimal > 0) {
                scenariosWithBenefit += 1;
            }
            greedy.add(withGreedy, withOptimal);
            stampAware.add(withStampAware, withOptimal);

            long greedyGain = withGreedy - heldGreedy;
            long optimalGain = withOptimal - heldOptimal;
            long stampAwareGain = withStampAware - heldStampAware;
            greedyGainNegative += greedyGain < 0 ? 1 : 0;
            greedyGainMissed += (greedyGain <= 0 && optimalGain > 0) ? 1 : 0;
            greedyGainOverstated += greedyGain > optimalGain ? 1 : 0;
            greedyGainUnderstated += (greedyGain > 0 && greedyGain < optimalGain) ? 1 : 0;
            stampAwareGainNegative += stampAwareGain < 0 ? 1 : 0;
            stampAwareGainMissed += (stampAwareGain <= 0 && optimalGain > 0) ? 1 : 0;
        }

        void print() {
            if (scenarios == 0) {
                return;
            }
            StringBuilder report = new StringBuilder();
            report.append(String.format(Locale.ROOT,
                    "%n[탐욕 대 최적] 사례 %d건 (그중 받을 혜택이 있는 사례 %d건)%n", scenarios, scenariosWithBenefit));
            report.append(String.format(Locale.ROOT, "  %-28s %s%n", "탐욕(지금 방식)", greedy.describe(scenarios)));
            report.append(String.format(Locale.ROOT, "  %-28s %s%n", "스탬프 진행 반영(실험)", stampAware.describe(scenarios)));
            report.append(String.format(Locale.ROOT,
                    "  순증(후보 포함 − 보유만), 탐욕: 음수 %d건(%.2f%%) / 최적은 양수인데 0 이하라 추천에서 빠짐 %d건(%.2f%%)"
                            + " / 최적보다 크게 나옴 %d건(%.2f%%) / 양수지만 최적보다 작게 나옴 %d건(%.2f%%)%n",
                    greedyGainNegative, percent(greedyGainNegative, scenarios),
                    greedyGainMissed, percent(greedyGainMissed, scenarios),
                    greedyGainOverstated, percent(greedyGainOverstated, scenarios),
                    greedyGainUnderstated, percent(greedyGainUnderstated, scenarios)));
            report.append(String.format(Locale.ROOT,
                    "  순증, 스탬프 진행 반영(실험): 음수 %d건(%.2f%%) / 추천에서 빠짐 %d건(%.2f%%)%n",
                    stampAwareGainNegative, percent(stampAwareGainNegative, scenarios),
                    stampAwareGainMissed, percent(stampAwareGainMissed, scenarios)));
            System.out.print(report);
        }

        private static double percent(long part, long whole) {
            return whole == 0 ? 0.0 : part * 100.0 / whole;
        }

        /** 한 배분 방식이 최적보다 얼마나 덜 받았는가 */
        private static final class Loss {
            private int losingCases;
            private double sumOfLossRates;
            private double maxLossRate;
            private long totalActual;
            private long totalOptimal;

            void add(long actual, long optimal) {
                totalActual += actual;
                totalOptimal += optimal;
                if (actual < optimal) {
                    double lossRate = (optimal - actual) / (double) optimal;
                    losingCases += 1;
                    sumOfLossRates += lossRate;
                    maxLossRate = Math.max(maxLossRate, lossRate);
                }
            }

            String describe(int scenarios) {
                return String.format(Locale.ROOT,
                        "최적보다 손해 %d건(%.2f%%), 손해 사례의 평균 손실률 %.1f%%, 최대 손실률 %.1f%%, 전체 혜택 합 기준 손실 %.2f%%",
                        losingCases, percent(losingCases, scenarios),
                        losingCases == 0 ? 0.0 : sumOfLossRates * 100.0 / losingCases,
                        maxLossRate * 100.0,
                        totalOptimal == 0 ? 0.0 : (totalOptimal - totalActual) * 100.0 / totalOptimal);
            }
        }
    }
}
