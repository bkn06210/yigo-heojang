package com.wallet.engine.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PerformanceExclusionTypeTest {

    @Test
    void 알려진_유형_문자열을_변환한다() {
        assertThat(PerformanceExclusionType.from("CATEGORY")).isEqualTo(PerformanceExclusionType.CATEGORY);
        assertThat(PerformanceExclusionType.from("PAYMENT_TYPE")).isEqualTo(PerformanceExclusionType.PAYMENT_TYPE);
        assertThat(PerformanceExclusionType.from("TRANSACTION_ATTR")).isEqualTo(PerformanceExclusionType.TRANSACTION_ATTR);
        assertThat(PerformanceExclusionType.from("MIN_TXN_AMOUNT")).isEqualTo(PerformanceExclusionType.MIN_TXN_AMOUNT);
    }

    @Test
    void 모르는_유형은_예외다() {
        // performance_exclusion엔 CHECK 제약이 없어 오타난 타입이 DB에서 안 걸린다 — 여기서 fail-fast
        assertThatIllegalArgumentException().isThrownBy(() -> PerformanceExclusionType.from("MERCHANT"));
        assertThatIllegalArgumentException().isThrownBy(() -> PerformanceExclusionType.from("category"));
    }

    @Test
    void null이나_빈_문자열은_예외다() {
        assertThatIllegalArgumentException().isThrownBy(() -> PerformanceExclusionType.from(null));
        assertThatIllegalArgumentException().isThrownBy(() -> PerformanceExclusionType.from(""));
        assertThatIllegalArgumentException().isThrownBy(() -> PerformanceExclusionType.from("  "));
    }
}
