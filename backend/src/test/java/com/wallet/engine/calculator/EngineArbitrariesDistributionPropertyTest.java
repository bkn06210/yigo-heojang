package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineArbitraries.StampMode;
import com.wallet.engine.calculator.EngineSpecs.BenefitSpec;
import com.wallet.engine.calculator.EngineSpecs.CardSpec;
import com.wallet.engine.calculator.EngineSpecs.ContextSpec;
import com.wallet.engine.calculator.EngineSpecs.PaymentSpec;
import com.wallet.engine.calculator.EngineSpecs.RuleSpec;
import com.wallet.engine.model.BenefitResult;
import com.wallet.engine.model.CalcMethod;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 생성기가 실제로 무엇을 만드는지 집계해 찍는다.
 *
 * 속성 테스트의 "N건 통과"는 그 N건이 무엇이었느냐에 따라 값이 달라진다. 미적용으로 끝나는 입력만
 * 잔뜩 만들면 어떤 불변식도 통과한다. 그래서 계산 방식·NULL 한도·0 한도·묶음·실제 지급 여부의
 * 비율을 테스트 출력에 남겨, 생성기가 한쪽으로 쏠리면 눈에 보이게 한다.
 */
class EngineArbitrariesDistributionPropertyTest {

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
    Arbitrary<CardSpec> cards() {
        return EngineArbitraries.cards(1L, StampMode.WITH_STAMP, 4);
    }

    @Provide
    Arbitrary<List<PaymentSpec>> payments() {
        return EngineArbitraries.paymentLists(1, 14);
    }

    @Property(tries = TRIES, seed = SEED)
    void 혜택_규칙과_계산_결과의_분포(@ForAll("rules") RuleSpec rule, @ForAll("contexts") ContextSpec context) {
        BenefitResult result = calculator.calculate(rule.toRule(), context.toContext());

        Statistics.label("계산 방식").collect(rule.method());
        Statistics.label("계산 대상 종류인가").collect(rule.isCalculableKind());
        Statistics.label("월 금액 한도").collect(describe(rule.amountLimits().monthly()));
        Statistics.label("일 금액 한도").collect(describe(rule.amountLimits().daily()));
        Statistics.label("월 횟수 한도").collect(describe(rule.countLimits().monthly()));
        Statistics.label("건당 최소금액").collect(rule.minTxnAmount() == null ? "없음" : "있음");
        Statistics.label("건당 최대").collect(describe(rule.maxBenefitPerTxn()));
        Statistics.label("대상금액 상한").collect(rule.maxEligibleAmount() == null ? "없음" : "있음");
        Statistics.label("실적 조건").collect(rule.requirePerformance() ? "필요" : "무관");
        Statistics.label("계산 결과").collect(!result.applied()
                ? "미적용 " + result.notApplicableReason()
                : result.benefitAmount() == 0 ? "적용 0원" : "적용 " + result.appliedCap());

        assertThat(result.benefitAmount()).isNotNegative();
    }

    @Property(tries = TRIES, seed = SEED)
    void 카드와_결제_열의_분포(@ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);
        long paidPayments = ledger.entries().stream().filter(PaymentLedger.Entry::paid).count();

        Statistics.label("카드의 혜택 수").collect(Math.min(card.benefits().size(), 8));
        Statistics.label("금액 묶음이 있는 카드").collect(
                card.benefits().stream().anyMatch(benefit -> benefit.limitGroupCode() != null));
        Statistics.label("횟수 묶음을 따로 둔 카드").collect(
                card.benefits().stream().anyMatch(benefit -> benefit.countGroupCode() != null));
        Statistics.label("스탬프형이 있는 카드").collect(card.hasStampBenefit());
        Statistics.label("실적 충족").collect(card.monthStatus().performanceMet());
        Statistics.label("통합한도").collect(describe(card.monthStatus().sharedMonthlyLimit()));
        Statistics.label("혜택을 받은 결제 수").collect(paidPayments == 0 ? "0건" : paidPayments <= 3 ? "1~3건" : "4건 이상");
        Statistics.label("스탬프가 찍힌 결제가 있는가").collect(
                ledger.entries().stream().anyMatch(entry -> !entry.stampedBenefitIds().isEmpty()));

        assertThat(ledger.totalDiscount()).isNotNegative();
        assertThat(card.benefits()).extracting(BenefitSpec::benefitId).doesNotHaveDuplicates();
        assertThat(card.benefits()).allSatisfy(benefit -> {
            if (benefit.rule().method() != CalcMethod.RATE) {
                assertThat(benefit.rule().maxEligibleAmount()).isNull();
            }
        });
    }

    private static String describe(Number limit) {
        if (limit == null) {
            return "NULL(제약 없음)";
        }
        return limit.longValue() == 0 ? "0(혜택 없음)" : "양수";
    }
}
