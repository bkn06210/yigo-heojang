package com.wallet.engine.service;

import com.wallet.engine.dto.BenefitSummary;
import com.wallet.engine.dto.BenefitUsageStatus;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.dto.CardStatusSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 현황(#2) 조립기 단위 테스트 — DB·시계 없이 접기 규칙만 검증한다.
 *
 * 실적 계산은 여기서 하지 않는다(상세에서 이미 계산돼 넘어온다). 고정하는 것은 둘이다:
 * 묶음 한도를 그룹당 한 줄로 접는 규칙, 브리핑 카드를 고르는 규칙.
 */
class CardStatusOverviewBuilderTest {

    private final CardStatusOverviewBuilder builder = new CardStatusOverviewBuilder();

    @Test
    @DisplayName("묶음 한도 혜택은 그룹당 한 줄로 접히고 대표는 가장 작은 benefit_id다")
    void 묶음_혜택은_한_줄로_접힌다() {
        // 통신·공과금·마트가 LIVING 코드로 월 5,000원 한도를 공유한다(잔여 2,000원 지갑 하나)
        CardMonthlyStatus status = status(1, "생활카드", List.of(
                benefit(61, "통신 10% 할인", "LIVING", 2_000L),
                benefit(62, "공과금 10% 할인", "LIVING", 2_000L),
                benefit(63, "마트 10% 할인", "LIVING", 2_000L)));

        List<BenefitSummary> summary = builder.build(List.of(status)).cards().get(0).benefitsSummary();

        // 세 줄로 내려가면 화면이 6,000원으로 더한다 — 한 줄, 한 번만
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).benefitId()).isEqualTo(61L);
        assertThat(summary.get(0).benefitName()).isEqualTo("통신 10% 할인 외 2건");
        assertThat(summary.get(0).limitGroupCode()).isEqualTo("LIVING");
        assertThat(summary.get(0).remainingLimit()).isEqualTo(2_000L);
    }

    @Test
    @DisplayName("다 쓴 혜택은 요약에서 빠지고, 한도 없는 혜택은 잔여 null로 남는다")
    void 남은_혜택만_요약한다() {
        CardMonthlyStatus status = status(1, "카드", List.of(
                benefit(10, "편의점 5% 할인", null, 3_000L),
                benefit(20, "주유 할인", null, 0L),
                benefit(30, "전 가맹점 0.7% 적립", null, null)));

        List<BenefitSummary> summary = builder.build(List.of(status)).cards().get(0).benefitsSummary();

        // 잔여 0 = 이번 달 다 씀(뺀다) / 잔여 null = 한도 제약 없음(남긴다). NULL≠0
        assertThat(summary).extracting(BenefitSummary::benefitId).containsExactly(10L, 30L);
        assertThat(summary.get(1).remainingLimit()).isNull();
    }

    @Test
    @DisplayName("실적 미충족 카드에서는 실적 조건부 혜택을 요약에서 뺀다")
    void 실적_미충족이면_실적_조건부_혜택은_요약에서_빠진다() {
        List<BenefitUsageStatus> benefits = List.of(
                benefit(10, "실적 필요 없는 혜택", null, 3_000L, false),
                benefit(20, "실적 조건부 혜택", null, 5_000L, true));

        // 실적 미충족 — 조건부 혜택은 이번 달 계산기가 아예 적용하지 않으므로 "남은 혜택"이 아니다
        CardStatusSummary notMet = builder.build(List.of(
                statusWithPerformance(1, "미달카드", benefits, false))).cards().get(0);
        assertThat(notMet.benefitsSummary()).extracting(BenefitSummary::benefitId).containsExactly(10L);

        // 충족했으면 둘 다 남는다
        CardStatusSummary met = builder.build(List.of(
                statusWithPerformance(1, "충족카드", benefits, true))).cards().get(0);
        assertThat(met.benefitsSummary()).extracting(BenefitSummary::benefitId).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("브리핑은 아직 못 채운 카드 중 달성률이 가장 높은 카드를 고른다")
    void 브리핑은_달성률이_가장_높은_미달성_카드다() {
        CardStatusOverview overview = builder.build(List.of(
                performanceStatus(11, "50퍼센트카드", "50.0", 100_000),
                performanceStatus(12, "90퍼센트카드", "90.0", 50_000),
                performanceStatus(13, "달성카드", "120.0", 0),      // 남은 실적 0 = 이미 채움
                noPerformanceStatus(14, "실적조건없는카드")));       // 달성률 null

        assertThat(overview.briefing()).isNotNull();
        assertThat(overview.briefing().userCardId()).isEqualTo(12L);
        assertThat(overview.briefing().cardName()).isEqualTo("90퍼센트카드");
        assertThat(overview.briefing().remainingPerformance()).isEqualTo(50_000L);
        // 문구의 카드 수는 후보 수(2)가 아니라 보유 카드 수(4)다
        assertThat(overview.briefing().message())
                .isEqualTo("보유하신 카드 4장 중 90퍼센트카드 카드 실적이 90%로 가장 임박했어요. "
                        + "이번 달은 이 카드부터 채우는 걸 추천드려요.");
    }

    @Test
    @DisplayName("채울 실적이 있는 카드가 없으면 브리핑은 null이다")
    void 후보가_없으면_브리핑은_null이다() {
        CardStatusOverview overview = builder.build(List.of(
                performanceStatus(13, "달성카드", "120.0", 0),
                noPerformanceStatus(14, "실적조건없는카드")));

        assertThat(overview.briefing()).isNull();
        // 브리핑이 없어도 카드 목록은 그대로 내려간다
        assertThat(overview.cards()).hasSize(2);
    }

    @Test
    @DisplayName("달성률이 같으면 남은 금액이 적은 카드를 고른다")
    void 달성률_동률이면_남은_금액이_적은_카드다() {
        CardStatusOverview overview = builder.build(List.of(
                performanceStatus(21, "큰목표카드", "80.0", 100_000),
                performanceStatus(22, "작은목표카드", "80.0", 20_000)));

        assertThat(overview.briefing().userCardId()).isEqualTo(22L);
    }

    @Test
    @DisplayName("요약의 실적 값은 상세와 같은 값을 그대로 쓴다")
    void 요약은_상세에서_파생된다() {
        CardMonthlyStatus detail = new CardMonthlyStatus(
                12, "삼성 iD ON", "2026-08",
                520_000, 500_000, 400_000, 100_000, new BigDecimal("80.0"),
                true, 20_000L, 8_000, List.of());

        CardStatusSummary summary = builder.build(List.of(detail)).cards().get(0);

        assertThat(summary.currentPerformanceAmount()).isEqualTo(400_000L);
        assertThat(summary.targetPerformance()).isEqualTo(500_000L);
        assertThat(summary.remainingPerformance()).isEqualTo(100_000L);
        assertThat(summary.achievementRate()).isEqualByComparingTo("80.0");
        assertThat(summary.performanceMet()).isTrue();
        assertThat(summary.sharedLimit()).isEqualTo(20_000L);
        assertThat(summary.sharedLimitUsed()).isEqualTo(8_000L);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    /** 실적을 충족한 카드 — 혜택 요약 규칙만 보는 테스트용 */
    private CardMonthlyStatus status(long userCardId, String cardName, List<BenefitUsageStatus> benefits) {
        return statusWithPerformance(userCardId, cardName, benefits, true);
    }

    private CardMonthlyStatus statusWithPerformance(long userCardId, String cardName,
                                                    List<BenefitUsageStatus> benefits,
                                                    boolean performanceMet) {
        return new CardMonthlyStatus(userCardId, cardName, "2026-08",
                0, 0, 0, 0, null, performanceMet, null, 0, benefits);
    }

    /** 실적 조건이 있는 카드 — 달성률과 남은 금액만 브리핑 판정에 쓰인다 */
    private CardMonthlyStatus performanceStatus(long userCardId, String cardName,
                                                String achievementRate, long remainingPerformance) {
        return new CardMonthlyStatus(userCardId, cardName, "2026-08",
                0, 500_000, 0, remainingPerformance, new BigDecimal(achievementRate),
                true, null, 0, List.of());
    }

    /** 실적 조건이 없는 카드 — 목표 0이라 달성률이 null이다 */
    private CardMonthlyStatus noPerformanceStatus(long userCardId, String cardName) {
        return new CardMonthlyStatus(userCardId, cardName, "2026-08",
                0, 0, 0, 0, null, false, null, 0, List.of());
    }

    private BenefitUsageStatus benefit(long benefitId, String name, String groupCode, Long remainingLimit) {
        return benefit(benefitId, name, groupCode, remainingLimit, false);
    }

    private BenefitUsageStatus benefit(long benefitId, String name, String groupCode,
                                       Long remainingLimit, boolean requirePerformance) {
        return new BenefitUsageStatus(benefitId, name, groupCode, 0L,
                remainingLimit == null ? null : remainingLimit + 1_000L, remainingLimit, null,
                requirePerformance);
    }
}
