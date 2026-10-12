package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineArbitraries.StampMode;
import com.wallet.engine.calculator.EngineSpecs.CardSpec;
import com.wallet.engine.calculator.EngineSpecs.PaymentSpec;
import com.wallet.engine.model.PortfolioSimulation;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;
import com.wallet.engine.model.SpendingSimulation;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 카드 여러 장에 결제를 나눠 흘린 결과의 불변식.
 */
class CardPortfolioSimulatorPropertyTest {

    private static final int TRIES = 5_000;
    private static final String SEED = "20261012";

    private final CardPortfolioSimulator simulator = new CardPortfolioSimulator();

    @Provide
    Arbitrary<List<CardSpec>> portfolios() {
        return EngineArbitraries.cardLists(2, 3, StampMode.WITH_STAMP, 3);
    }

    @Provide
    Arbitrary<List<CardSpec>> portfoliosWithoutStamp() {
        return EngineArbitraries.cardLists(2, 3, StampMode.WITHOUT_STAMP, 3);
    }

    @Provide
    Arbitrary<List<PaymentSpec>> payments() {
        return EngineArbitraries.paymentLists(1, 12);
    }

    private PortfolioSimulation simulate(List<CardSpec> cards, List<PaymentSpec> payments) {
        return simulator.simulate(EngineSpecs.toSimulatedCards(cards), EngineSpecs.toPayments(payments));
    }

    /**
     * "카드를 더하면 총 혜택이 줄지 않는다"는 성립하지 않는다 — 결제마다 그 순간 최대인 카드를 고르기 때문이다.
     * 그래서 총액끼리 뺀 값이 아니라 {@link PortfolioSimulation#gainOver}가 0 이상임을 검사하고,
     * 뺀 값이 음수로 나온 빈도는 통계로 찍어 둔다.
     */
    @Property(tries = TRIES, seed = SEED)
    void 카드를_한_장_더했을_때의_순증은_0_이상이다(
            @ForAll("portfolios") List<CardSpec> cards, @ForAll("payments") List<PaymentSpec> payments) {
        assertGainIsNeverNegative(cards, payments);
    }

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형이_없는_카드들에서도_카드를_한_장_더했을_때의_순증은_0_이상이다(
            @ForAll("portfoliosWithoutStamp") List<CardSpec> cards,
            @ForAll("payments") List<PaymentSpec> payments) {
        assertGainIsNeverNegative(cards, payments);
    }

    private void assertGainIsNeverNegative(List<CardSpec> cards, List<PaymentSpec> payments) {
        PortfolioSimulation heldOnly = simulate(cards.subList(0, cards.size() - 1), payments);
        PortfolioSimulation withCandidate = simulate(cards, payments);
        long difference = withCandidate.totalBenefitAmount() - heldOnly.totalBenefitAmount();

        Statistics.label("후보 포함 − 보유만")
                .collect(difference < 0 ? "음수" : difference == 0 ? "0" : "양수");
        assertThat(withCandidate.gainOver(heldOnly)).isGreaterThanOrEqualTo(0L);
        if (difference >= 0) {
            assertThat(withCandidate.gainOver(heldOnly)).isEqualTo(difference);
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 카드가_한_장이면_한_장짜리_시뮬레이터와_같은_값이다(
            @ForAll("portfolios") List<CardSpec> cards, @ForAll("payments") List<PaymentSpec> payments) {
        CardSpec only = cards.get(0);
        List<SimulatedPayment> simulatedPayments = EngineSpecs.toPayments(payments);

        PortfolioSimulation portfolio = simulator.simulate(List.of(only.toSimulatedCard()), simulatedPayments);
        SpendingSimulation single = new CardSpendingSimulator().simulate(only.toCandidates(),
                only.toMonthStatus(), only.toQuarterStatus(), only.selectedOptionKeys(), simulatedPayments);

        assertThat(portfolio.totalBenefitAmount()).isEqualTo(single.totalBenefitAmount());
        assertThat(portfolio.shares().get(0).breakdowns()).isEqualTo(single.breakdowns());
    }

    @Property(tries = TRIES, seed = SEED)
    void 총_혜택액은_카드별_몫의_합이고_결제금액_합을_넘지_않는다(
            @ForAll("portfolios") List<CardSpec> cards, @ForAll("payments") List<PaymentSpec> payments) {
        PortfolioSimulation result = simulate(cards, payments);

        assertThat(result.shares().stream().mapToLong(PortfolioSimulation.CardShare::benefitAmount).sum())
                .isEqualTo(result.totalBenefitAmount());
        assertThat(result.totalBenefitAmount())
                .isBetween(0L, payments.stream().mapToLong(PaymentSpec::amount).sum());
        assertThat(result.shares().stream().mapToInt(PortfolioSimulation.CardShare::appliedPaymentCount).sum())
                .isLessThanOrEqualTo(payments.size());
    }

    @Property(tries = TRIES, seed = SEED)
    void 카드_목록의_순서를_뒤집어도_결과가_같다(
            @ForAll("portfolios") List<CardSpec> cards, @ForAll("payments") List<PaymentSpec> payments) {
        List<SimulatedCard> reversed = new ArrayList<>(EngineSpecs.toSimulatedCards(cards));
        Collections.reverse(reversed);

        assertThat(simulator.simulate(reversed, EngineSpecs.toPayments(payments)))
                .isEqualTo(simulate(cards, payments));
    }

    @Property(tries = TRIES, seed = SEED)
    void 같은_입력이면_같은_결과다(
            @ForAll("portfolios") List<CardSpec> cards, @ForAll("payments") List<PaymentSpec> payments) {
        assertThat(simulate(cards, payments)).isEqualTo(simulate(cards, payments));
    }
}
