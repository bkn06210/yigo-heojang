package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.TierLimitOverride;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TierLimitResolverTest {

    private final TierLimitResolver resolver = new TierLimitResolver();

    private BenefitRule baseRule() {
        return BenefitRule.builder()
                .benefitId(1L)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal("10.00"))
                .monthlyLimit(10000L)
                .build();
    }

    private TierLimitOverride monthly(Long monthlyLimit) {
        return new TierLimitOverride(monthlyLimit, null, null, null);
    }

    @Test
    void 구간_값이_하나도_없으면_원값을_그대로_돌려준다() {
        BenefitRule resolved = resolver.resolve(baseRule(), TierLimitOverride.none());

        assertThat(resolved.getMonthlyLimit()).isEqualTo(10000L);
        assertThat(resolved.getBenefitValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void 구간_한도가_있으면_덮어쓴다() {
        BenefitRule resolved = resolver.resolve(baseRule(), monthly(5000L));

        assertThat(resolved.getMonthlyLimit()).isEqualTo(5000L);
        assertThat(resolved.getBenefitValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void 구간_한도_0은_상속이_아니라_0이다() {
        BenefitRule resolved = resolver.resolve(baseRule(), monthly(0L));

        assertThat(resolved.getMonthlyLimit()).isZero();
    }

    @Test
    void 구간_혜택값이_있으면_율까지_덮어쓴다() {
        BenefitRule resolved = resolver.resolve(
                baseRule(), new TierLimitOverride(null, null, null, new BigDecimal("2.00")));

        assertThat(resolved.getBenefitValue()).isEqualByComparingTo("2.00");
        assertThat(resolved.getMonthlyLimit()).isEqualTo(10000L);
    }

    @Test
    void 한도와_혜택값을_동시에_덮어쓴다() {
        BenefitRule resolved = resolver.resolve(
                baseRule(), new TierLimitOverride(30000L, null, null, new BigDecimal("1.50")));

        assertThat(resolved.getMonthlyLimit()).isEqualTo(30000L);
        assertThat(resolved.getBenefitValue()).isEqualByComparingTo("1.50");
    }

    /** 기간별 한도는 축이 따로다 — 한쪽만 지정한 구간이 다른 축까지 덮어쓰면 안 된다 */
    @Test
    void 분기_한도와_연_한도는_각각_따로_덮어쓴다() {
        BenefitRule base = baseRule().toBuilder()
                .quarterlyLimit(50000L)
                .yearlyLimit(200000L)
                .build();

        BenefitRule resolved = resolver.resolve(base, new TierLimitOverride(null, 90000L, null, null));

        assertThat(resolved.getQuarterlyLimit()).isEqualTo(90000L);
        assertThat(resolved.getYearlyLimit()).isEqualTo(200000L);
        assertThat(resolved.getMonthlyLimit()).isEqualTo(10000L);
    }

    @Test
    void 덮어쓰지_않은_다른_필드는_보존된다() {
        BenefitRule base = baseRule().toBuilder()
                .maxBenefitPerTxn(3000L)
                .requirePerformance(true)
                .requirePaymentType("SIMPLE_PAY")
                .yearlyCountLimit(4)
                .useSharedLimit(true)
                .build();

        BenefitRule resolved = resolver.resolve(base, monthly(5000L));

        assertThat(resolved.getBenefitId()).isEqualTo(1L);
        assertThat(resolved.getBenefitKind()).isEqualTo(BenefitKind.DISCOUNT);
        assertThat(resolved.getCalcMethod()).isEqualTo(CalcMethod.RATE);
        assertThat(resolved.getMaxBenefitPerTxn()).isEqualTo(3000L);
        assertThat(resolved.isRequirePerformance()).isTrue();
        assertThat(resolved.getRequirePaymentType()).isEqualTo("SIMPLE_PAY");
        assertThat(resolved.getYearlyCountLimit()).isEqualTo(4);
        assertThat(resolved.isUseSharedLimit()).isTrue();
    }
}
