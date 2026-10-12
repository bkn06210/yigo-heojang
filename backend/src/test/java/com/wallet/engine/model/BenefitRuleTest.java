package com.wallet.engine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BenefitRuleTest {

    private BenefitRule.Builder rateRule(String percent) {
        return BenefitRule.builder()
                .benefitId(1L)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal(percent));
    }

    private BenefitRule.Builder fixedRule(String amount) {
        return BenefitRule.builder()
                .benefitId(2L)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.FIXED)
                .benefitValue(new BigDecimal(amount));
    }

    @Nested
    @DisplayName("혜택값")
    class BenefitValue {

        @Test
        void 음수_혜택값은_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("-10.00").build());
            assertThatIllegalArgumentException().isThrownBy(() -> fixedRule("-500").build());
        }

        @Test
        void 정률_요율이_100을_넘으면_예외다() {
            // 10.00을 1000으로 잘못 넣으면 결제금액 전액 할인으로 계산된다. 에러 없이 금액만 틀리는 경우다
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("1000").build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("100.01").build());
        }

        @Test
        void 정률_요율_0과_100은_만들_수_있다() {
            assertThat(rateRule("0.00").build().getBenefitValue()).isEqualByComparingTo("0");
            assertThat(rateRule("100.00").build().getBenefitValue()).isEqualByComparingTo("100");
        }

        @Test
        void 정액은_100을_넘어도_된다() {
            // 요율이 아니라 원 단위 금액이다
            assertThat(fixedRule("2000").build().getBenefitValue()).isEqualByComparingTo("2000");
        }
    }

    @Nested
    @DisplayName("상한과 조건")
    class LimitsAndConditions {

        @Test
        void 음수_금액_한도는_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").dailyLimit(-1L).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").monthlyLimit(-1L).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").quarterlyLimit(-1L).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").yearlyLimit(-1L).build());
        }

        @Test
        void 음수_횟수_한도는_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").dailyCountLimit(-1).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").monthlyCountLimit(-1).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").quarterlyCountLimit(-1).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").yearlyCountLimit(-1).build());
        }

        @Test
        void 음수_건당_조건은_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").minTxnAmount(-1L).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").maxEligibleAmount(-1L).build());
            assertThatIllegalArgumentException().isThrownBy(() -> rateRule("10.00").maxBenefitPerTxn(-1L).build());
        }

        @Test
        void 한도_0은_혜택_없음이라_만들_수_있다() {
            BenefitRule rule = rateRule("10.00").monthlyLimit(0L).monthlyCountLimit(0).maxBenefitPerTxn(0L).build();

            assertThat(rule.getMonthlyLimit()).isZero();
            assertThat(rule.getMonthlyCountLimit()).isZero();
            assertThat(rule.getMaxBenefitPerTxn()).isZero();
        }

        @Test
        void 한도_NULL은_제약_없음이라_만들_수_있다() {
            BenefitRule rule = rateRule("10.00").build();

            assertThat(rule.getMonthlyLimit()).isNull();
            assertThat(rule.getMonthlyCountLimit()).isNull();
            assertThat(rule.getMaxBenefitPerTxn()).isNull();
        }
    }
}
