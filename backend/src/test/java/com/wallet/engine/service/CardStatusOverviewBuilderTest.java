package com.wallet.engine.service;

import com.wallet.engine.dto.BenefitSummary;
import com.wallet.engine.dao.dto.CategorySpendingRow;
import com.wallet.engine.dto.BenefitUsageStatus;
import com.wallet.engine.dto.BriefingType;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.dto.CardStatusSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

        List<BenefitSummary> summary = build(List.of(status)).cards().get(0).benefitsSummary();

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

        List<BenefitSummary> summary = build(List.of(status)).cards().get(0).benefitsSummary();

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
        CardStatusSummary notMet = build(List.of(
                statusWithPerformance(1, "미달카드", benefits, false))).cards().get(0);
        assertThat(notMet.benefitsSummary()).extracting(BenefitSummary::benefitId).containsExactly(10L);

        // 충족했으면 둘 다 남는다
        CardStatusSummary met = build(List.of(
                statusWithPerformance(1, "충족카드", benefits, true))).cards().get(0);
        assertThat(met.benefitsSummary()).extracting(BenefitSummary::benefitId).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("브리핑은 아직 못 채운 카드 중 달성률이 가장 높은 카드를 고른다")
    void 브리핑은_달성률이_가장_높은_미달성_카드다() {
        CardStatusOverview overview = build(List.of(
                performanceStatus(11, "50퍼센트카드", "50.0", 100_000),
                performanceStatus(12, "90퍼센트카드", "90.0", 50_000),
                performanceStatus(13, "달성카드", "120.0", 0),      // 남은 실적 0 = 이미 채움
                noPerformanceStatus(14, "실적조건없는카드")));       // 달성률 null

        assertThat(overview.briefing()).isNotNull();
        assertThat(overview.briefing().userCardId()).isEqualTo(12L);
        assertThat(overview.briefing().cardName()).isEqualTo("90퍼센트카드");
        assertThat(overview.briefing().remainingPerformance()).isEqualTo(50_000L);
        assertThat(overview.briefing().type()).isEqualTo(BriefingType.PERFORMANCE_NEAR);
        // 문구는 달마다 갈리므로 글자 그대로 비교하지 않는다. 고정할 것은 채워진 값이다.
        // 카드 수는 후보 수(2)가 아니라 보유 카드 수(4)이고, 달성률은 의미 없는 소수점 0을 뗀다.
        assertThat(overview.briefing().message()).contains("4장", "90퍼센트카드", "90%");
        assertThat(overview.briefing().message()).doesNotContain("90.0%", "{");
    }

    @Test
    @DisplayName("실적 조건이 있는 카드를 전부 채웠으면 달성을 알린다")
    void 실적을_전부_채우면_달성을_알린다() {
        CardStatusOverview overview = build(List.of(
                performanceStatus(13, "달성카드", "120.0", 0),
                noPerformanceStatus(14, "실적조건없는카드")));

        assertThat(overview.briefing().type()).isEqualTo(BriefingType.ALL_ACHIEVED);
        // 특정 카드를 가리키는 안내가 아니다
        assertThat(overview.briefing().userCardId()).isNull();
        assertThat(overview.cards()).hasSize(2);
    }

    @Test
    @DisplayName("실적 조건이 있는 카드가 하나도 없으면 달성했다고 하지 않는다")
    void 실적_조건이_없으면_달성이_아니다() {
        // 채울 실적이 없었을 뿐인데 "모두 채우셨습니다"라고 하면 하지 않은 일을 했다고 말하는 것이다
        CardStatusOverview overview = build(List.of(noPerformanceStatus(14, "실적조건없는카드")));

        assertThat(overview.briefing().type()).isNotEqualTo(BriefingType.ALL_ACHIEVED);
    }

    @Test
    @DisplayName("보유 카드가 없으면 카드 등록을 안내한다")
    void 카드가_없으면_등록을_안내한다() {
        CardStatusOverview overview = build(List.of());

        assertThat(overview.briefing().type()).isEqualTo(BriefingType.NO_CARD);
        assertThat(overview.briefing().message()).isNotBlank();
        assertThat(overview.cards()).isEmpty();
    }

    @Test
    @DisplayName("결제한 업종인데 그 혜택을 안 쓰고 있으면 그 카드를 권한다")
    void 안_쓰는_혜택을_권한다() {
        CardMonthlyStatus card = statusWithPerformance(1, "핏카드",
                List.of(benefit(31, "커피 10% 적립", null, 8_000L)), true);

        CardStatusOverview overview = builder.build(List.of(card), new BriefingContext(
                "2026-08",
                Map.of(31L, 102L),                       // 이 혜택은 카페(102)를 겨냥한다
                List.of(spending(102, "카페", null, 12, 240_000))));

        assertThat(overview.briefing().type()).isEqualTo(BriefingType.UNUSED_BENEFIT);
        assertThat(overview.briefing().cardName()).isEqualTo("핏카드");
        assertThat(overview.briefing().message()).contains("카페", "12", "핏카드", "커피 10% 적립");
        // 월 총계를 말하지 않는다 — 거래별 계산을 더하면 한도에 막히는 몫이 빠져 실제보다 커진다
        assertThat(overview.briefing().message()).doesNotContain("240,000", "240000");
    }

    @Test
    @DisplayName("혜택이 대분류를 겨냥하면 하위 업종 결제도 대상으로 본다")
    void 대분류_혜택은_하위_업종_결제에_매칭된다() {
        CardMonthlyStatus card = statusWithPerformance(1, "생활카드",
                List.of(benefit(41, "외식 5% 할인", null, 5_000L)), true);

        CardStatusOverview overview = builder.build(List.of(card), new BriefingContext(
                "2026-08",
                Map.of(41L, 100L),                       // 혜택 대상은 대분류 외식(100)
                List.of(spending(102, "카페", 100L, 3, 30_000))));  // 결제는 하위 중분류 카페

        assertThat(overview.briefing().type()).isEqualTo(BriefingType.UNUSED_BENEFIT);
    }

    @Test
    @DisplayName("이미 받고 있거나 한도를 다 쓴 혜택은 권하지 않는다")
    void 쓰고_있는_혜택은_권하지_않는다() {
        BenefitUsageStatus used = new BenefitUsageStatus(31, "커피 10% 적립", null,
                3_000L, 10_000L, 7_000L, null, false);          // 이미 3,000원 받았다
        BenefitUsageStatus exhausted = new BenefitUsageStatus(32, "편의점 할인", null,
                0L, 5_000L, 0L, null, false);                    // 한도를 다 썼다

        CardStatusOverview overview = builder.build(
                List.of(statusWithPerformance(1, "카드", List.of(used, exhausted), true)),
                new BriefingContext("2026-08", Map.of(31L, 102L, 32L, 103L),
                        List.of(spending(102, "카페", null, 5, 50_000),
                                spending(103, "편의점", null, 4, 40_000))));

        assertThat(overview.briefing().type()).isNotEqualTo(BriefingType.UNUSED_BENEFIT);
    }

    @Test
    @DisplayName("실적을 못 채운 카드의 실적 조건부 혜택은 권하지 않는다")
    void 실적_미충족_카드의_조건부_혜택은_권하지_않는다() {
        // 권해봐야 이번 달 계산기가 적용하지 않는다 — "이 카드로 결제하세요"가 거짓말이 된다
        CardMonthlyStatus card = statusWithPerformance(1, "미달카드",
                List.of(benefit(31, "커피 10% 적립", null, 8_000L, true)), false);

        CardStatusOverview overview = builder.build(List.of(card), new BriefingContext(
                "2026-08", Map.of(31L, 102L), List.of(spending(102, "카페", null, 12, 240_000))));

        assertThat(overview.briefing().type()).isNotEqualTo(BriefingType.UNUSED_BENEFIT);
    }

    @Test
    @DisplayName("권할 혜택이 없으면 소비가 가장 많은 업종을 전한다")
    void 권할_혜택이_없으면_소비를_전한다() {
        CardStatusOverview overview = builder.build(
                List.of(noPerformanceStatus(14, "실적조건없는카드")),
                new BriefingContext("2026-08", Map.of(),
                        List.of(spending(102, "카페", null, 9, 90_000))));

        assertThat(overview.briefing().type()).isEqualTo(BriefingType.SPENDING_INSIGHT);
        assertThat(overview.briefing().message()).contains("카페", "9");
    }

    @Test
    @DisplayName("이번 달 결제가 없으면 계산이 결제부터 시작된다고 알린다")
    void 결제가_없으면_시작을_알린다() {
        CardStatusOverview overview = build(List.of(noPerformanceStatus(14, "실적조건없는카드")));

        assertThat(overview.briefing().type()).isEqualTo(BriefingType.GETTING_STARTED);
    }

    @Test
    @DisplayName("같은 달에는 같은 문구가 나온다")
    void 같은_달에는_문구가_바뀌지_않는다() {
        // 무작위로 고르면 새로고침할 때마다 말이 바뀌어 매번 지어낸다는 인상을 준다
        List<CardMonthlyStatus> statuses = List.of(performanceStatus(11, "카드", "50.0", 100_000));

        String first = build(statuses).briefing().message();
        String second = build(statuses).briefing().message();

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("달성률이 같으면 남은 금액이 적은 카드를 고른다")
    void 달성률_동률이면_남은_금액이_적은_카드다() {
        CardStatusOverview overview = build(List.of(
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

        CardStatusSummary summary = build(List.of(detail)).cards().get(0);

        assertThat(summary.currentPerformanceAmount()).isEqualTo(400_000L);
        assertThat(summary.targetPerformance()).isEqualTo(500_000L);
        assertThat(summary.remainingPerformance()).isEqualTo(100_000L);
        assertThat(summary.achievementRate()).isEqualByComparingTo("80.0");
        assertThat(summary.performanceMet()).isTrue();
        assertThat(summary.sharedLimit()).isEqualTo(20_000L);
        assertThat(summary.sharedLimitUsed()).isEqualTo(8_000L);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    /** 소비 기록도 업종 혜택도 없는 상태 — 혜택 요약·실적 브리핑 규칙만 보는 테스트용 */
    private CardStatusOverview build(List<CardMonthlyStatus> statuses) {
        return builder.build(statuses, new BriefingContext("2026-08", Map.of(), List.of()));
    }

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

    /** 이번 달 업종별 소비 한 줄. parentCategoryId는 대분류 결제면 null이다 */
    private CategorySpendingRow spending(long categoryId, String categoryName, Long parentCategoryId,
                                         int paymentCount, long totalAmount) {
        CategorySpendingRow row = new CategorySpendingRow();
        row.setCategoryId(categoryId);
        row.setCategoryName(categoryName);
        row.setParentCategoryId(parentCategoryId);
        row.setPaymentCount(paymentCount);
        row.setTotalAmount(totalAmount);
        return row;
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
