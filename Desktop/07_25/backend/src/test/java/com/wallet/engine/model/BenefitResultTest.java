package com.wallet.engine.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BenefitResultTest {

    @Test
    void 적용_결과를_만든다() {
        BenefitResult result = BenefitResult.of(1500L, true, CapType.NONE);

        assertThat(result.applied()).isTrue();
        assertThat(result.benefitAmount()).isEqualTo(1500L);
        assertThat(result.estimate()).isTrue();
        assertThat(result.notApplicableReason()).isNull();
    }

    @Test
    void 미적용_결과는_0원_확정_NONE이다() {
        BenefitResult result = BenefitResult.notApplicable(NotApplicableReason.PERFORMANCE_NOT_MET);

        assertThat(result.applied()).isFalse();
        assertThat(result.benefitAmount()).isZero();
        assertThat(result.estimate()).isFalse();
        assertThat(result.appliedCap()).isEqualTo(CapType.NONE);
        assertThat(result.notApplicableReason()).isEqualTo(NotApplicableReason.PERFORMANCE_NOT_MET);
    }

    @Test
    void 불법_조합은_생성_단계에서_막힌다() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new BenefitResult(false, 100L, false, NotApplicableReason.PERFORMANCE_NOT_MET, CapType.NONE));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new BenefitResult(true, 100L, false, NotApplicableReason.PERFORMANCE_NOT_MET, CapType.NONE));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new BenefitResult(false, 0L, false, null, CapType.NONE));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new BenefitResult(true, -1L, false, null, CapType.NONE));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new BenefitResult(true, 100L, false, null, null));
    }
}
