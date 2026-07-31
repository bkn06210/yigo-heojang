package com.wallet.engine.service;

import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dto.BenefitUsageStatus;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.model.PerformanceTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 현황 응답 조립기 단위 테스트 — DB·시계 없이 순수 로직만 검증한다.
 *
 * 통합 테스트(SettlementServiceIntegrationTest)가 매퍼 SQL을 덮는 것과 달리, 여기서는
 * 조립 규칙 자체를 고정한다: 두 축(전월실적·당월누적) 계산과 묶음 한도 그룹 합산.
 */
class CardMonthlyStatusBuilderTest {

    private final CardMonthlyStatusBuilder builder = new CardMonthlyStatusBuilder();

    /** 0원 구간 + 30만 구간(통합한도 2만) */
    private static final List<PerformanceTier> TIERS = List.of(
            new PerformanceTier(1, 0, null),
            new PerformanceTier(2, 300_000, 20_000L));

    @Test
    @DisplayName("전월실적으로 충족·통합한도를, 당월누적으로 목표·달성률을 계산한다")
    void 두_축을_각각_계산한다() {
        // 전월 35만 → 30만 구간 충족(통합한도 2만). 당월 24만 → 목표 30만, 달성률 80%
        CardMonthlyStatus status = builder.build(
                12, "삼성 iD ON", "2026-08",
                350_000, 240_000, 8_000, TIERS, List.of(), Map.of());

        assertThat(status.performanceMet()).isTrue();
        assertThat(status.sharedLimit()).isEqualTo(20_000L);
        assertThat(status.sharedLimitUsed()).isEqualTo(8_000L);
        assertThat(status.targetPerformance()).isEqualTo(300_000L);
        assertThat(status.remainingPerformance()).isEqualTo(60_000L);
        assertThat(status.achievementRate()).isEqualByComparingTo("80.0");
    }

    @Test
    @DisplayName("실적 조건 없는 카드(목표 0)는 달성률이 0이 아니라 null이다")
    void 실적_조건_없는_카드는_달성률이_null이다() {
        List<PerformanceTier> onlyZeroTier = List.of(new PerformanceTier(1, 0, null));

        CardMonthlyStatus status = builder.build(
                1, "기본카드", "2026-08", 0, 0, 0, onlyZeroTier, List.of(), Map.of());

        assertThat(status.performanceMet()).isFalse();
        assertThat(status.achievementRate()).isNull();
    }

    @Test
    @DisplayName("혜택별 이용 현황: 잔여·이용률은 계산값이다")
    void 혜택_이용률과_잔여는_계산값이다() {
        BenefitRow row = benefitRow(10, "편의점 5% 할인", null, 5_000L, null);

        CardMonthlyStatus status = builder.build(
                1, "카드", "2026-08", 350_000, 0, 0, TIERS,
                List.of(row), Map.of(10L, 2_000L));

        BenefitUsageStatus benefit = only(status);
        assertThat(benefit.benefitId()).isEqualTo(10L);
        assertThat(benefit.usedAmount()).isEqualTo(2_000L);
        assertThat(benefit.monthlyLimit()).isEqualTo(5_000L);
        assertThat(benefit.remainingLimit()).isEqualTo(3_000L);
        assertThat(benefit.usageRate()).isEqualByComparingTo("40.0");
    }

    @Test
    @DisplayName("묶음 한도(같은 코드)는 소진액을 그룹 합산으로 내려준다")
    void 묶음_한도는_그룹_합산으로_내려간다() {
        // 통신·공과금이 같은 코드 LIVING으로 월 5,000원 한도를 공유한다
        BenefitRow telecom = benefitRow(20, "통신 10% 할인", "LIVING", 5_000L, null);
        BenefitRow utility = benefitRow(21, "공과금 10% 할인", "LIVING", 5_000L, null);

        CardMonthlyStatus status = builder.build(
                1, "카드", "2026-08", 350_000, 0, 0, TIERS,
                List.of(telecom, utility), Map.of(20L, 1_000L, 21L, 2_000L));

        // 두 혜택 모두 그룹 합산(3,000) 기준으로 같은 값을 갖는다 — 화면이 배로 더하지 않게
        assertThat(status.benefits()).hasSize(2);
        assertThat(status.benefits()).allSatisfy(benefit -> {
            assertThat(benefit.usedAmount()).isEqualTo(3_000L);
            assertThat(benefit.remainingLimit()).isEqualTo(2_000L);
            assertThat(benefit.usageRate()).isEqualByComparingTo("60.0");
        });
    }

    @Test
    @DisplayName("한도 없는 혜택도 목록에 담되 한도·잔여·이용률은 null이다")
    void 한도_없는_혜택은_한도_관련_값이_null이다() {
        BenefitRow limited = benefitRow(10, "편의점 5% 할인", null, 5_000L, null);
        BenefitRow unlimited = benefitRow(30, "전 가맹점 0.7% 적립", null, null, null);

        CardMonthlyStatus status = builder.build(
                1, "카드", "2026-08", 350_000, 0, 0, TIERS,
                List.of(limited, unlimited), Map.of(10L, 1_000L, 30L, 5_000L));

        assertThat(status.benefits()).extracting(BenefitUsageStatus::benefitId)
                .containsExactly(10L, 30L);
        // 한도 없는 혜택 — 소진액은 실제 값, 나머지 셋은 "제약 없음"이라 null(0이 아니다)
        BenefitUsageStatus noLimit = status.benefits().get(1);
        assertThat(noLimit.usedAmount()).isEqualTo(5_000L);
        assertThat(noLimit.monthlyLimit()).isNull();
        assertThat(noLimit.remainingLimit()).isNull();
        assertThat(noLimit.usageRate()).isNull();
    }

    @Test
    @DisplayName("소진이 한도를 넘어도 이용률은 100%를 넘지 않는다")
    void 이용률은_100을_넘지_않는다() {
        // 엔진 가산으로는 나올 수 없는 상태(계산기가 한도에서 자른다). 손으로 넣은 데이터에만 있다
        BenefitRow row = benefitRow(60, "그룹 한도 초과 혜택", null, 10_000L, null);

        CardMonthlyStatus status = builder.build(
                1, "카드", "2026-08", 350_000, 0, 0, TIERS, List.of(row), Map.of(60L, 11_600L));

        BenefitUsageStatus benefit = only(status);
        // 잔여는 0으로 깎으면서 이용률만 116%를 내보내면 응답이 자기모순이다
        assertThat(benefit.remainingLimit()).isZero();
        assertThat(benefit.usageRate()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("한도 0(혜택 없음)은 잔여 0이되 이용률은 null이다")
    void 한도_0인_혜택은_이용률이_null이다() {
        BenefitRow row = benefitRow(50, "구간 미달로 한도 0", null, 0L, null);

        CardMonthlyStatus status = builder.build(
                1, "카드", "2026-08", 350_000, 0, 0, TIERS, List.of(row), Map.of());

        BenefitUsageStatus benefit = only(status);
        assertThat(benefit.monthlyLimit()).isZero();
        assertThat(benefit.remainingLimit()).isZero();
        // 0으로 나눌 수 없다 — 이용률 0%("아직 안 씀")로 뭉개면 안 된다
        assertThat(benefit.usageRate()).isNull();
    }

    @Test
    @DisplayName("구간별 개별한도가 있으면 base가 아니라 그 구간의 한도를 쓴다")
    void 구간별_한도가_있으면_그_한도를_쓴다() {
        // base monthly_limit은 NULL, 판정 구간의 개별한도 1만원이 유효값이 된다
        BenefitRow row = benefitRow(40, "구간 혜택", null, null, 10_000L);

        CardMonthlyStatus status = builder.build(
                1, "카드", "2026-08", 350_000, 0, 0, TIERS,
                List.of(row), Map.of(40L, 5_000L));

        BenefitUsageStatus benefit = only(status);
        assertThat(benefit.monthlyLimit()).isEqualTo(10_000L);
        assertThat(benefit.remainingLimit()).isEqualTo(5_000L);
        assertThat(benefit.usageRate()).isEqualByComparingTo("50.0");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private BenefitRow benefitRow(long benefitId, String name, String groupCode,
                                  Long monthlyLimit, Long tierMonthlyLimit) {
        BenefitRow row = new BenefitRow();
        row.setBenefitId(benefitId);
        row.setBenefitName(name);
        row.setLimitGroupCode(groupCode);
        row.setMonthlyLimit(monthlyLimit);
        row.setTierMonthlyLimit(tierMonthlyLimit);
        return row;
    }

    private BenefitUsageStatus only(CardMonthlyStatus status) {
        assertThat(status.benefits()).hasSize(1);
        return status.benefits().get(0);
    }
}
