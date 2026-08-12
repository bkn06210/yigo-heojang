package com.wallet.notification.batch.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.notification.batch.model.BenefitCandidateRow;
import com.wallet.notification.batch.model.BenefitLimitCandidate;
import com.wallet.notification.batch.model.BenefitLimitStatus;
import com.wallet.notification.batch.model.BenefitLimitUnit;
import com.wallet.notification.batch.model.SharedLimitCandidateRow;
import com.wallet.notification.mapper.BenefitLimitCandidateMapper;

class BenefitLimitCandidateCalculatorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 오늘이 몇 년 몇 월인가만 쓰이므로, 하루로 고정해두고 모든 테스트에서 공유한다.
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    private BenefitLimitCandidateMapper mapper;
    private BenefitLimitCandidateCalculator calculator;

    @BeforeEach
    void setUp() {
        mapper = mock(BenefitLimitCandidateMapper.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay(KST).toInstant(), KST);
        calculator = new BenefitLimitCandidateCalculator(mapper, clock);

        // 개별적으로 stub하지 않는 테스트가 NPE 없이 "후보 없음"으로 흐르도록 기본값을 비워둔다.
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07")).thenReturn(List.of());
        when(mapper.findSharedLimitCandidateRows("2026-08", "2026-07")).thenReturn(List.of());
    }

    private BenefitCandidateRow benefitRow(
        long userCardId, long memberId, long cardId, String cardName,
        long benefitId, String benefitName, String limitGroupCode,
        String requirePerformance, String performancePeriod,
        Long effectiveMonthlyLimit, boolean monthPerformanceMet, long usedAmount
    ) {
        return BenefitCandidateRow.builder()
            .userCardId(userCardId)
            .memberId(memberId)
            .cardId(cardId)
            .cardName(cardName)
            .benefitId(benefitId)
            .benefitName(benefitName)
            .limitGroupCode(limitGroupCode)
            .requirePerformance(requirePerformance)
            .performancePeriod(performancePeriod)
            .effectiveMonthlyLimit(effectiveMonthlyLimit)
            .monthPerformanceMet(monthPerformanceMet)
            .usedAmount(usedAmount)
            .build();
    }

    // 실적 조건이 없는(N) 개별 혜택을 만들 때 자주 쓰는 축약형.
    private BenefitCandidateRow individualRow(
        long userCardId, long benefitId, String benefitName, Long limit, long used
    ) {
        return benefitRow(userCardId, 1L, 100L, "카드A", benefitId, benefitName, null,
            "N", "MONTH", limit, true, used);
    }

    @Test
    @DisplayName("개별 혜택 - 80~100% 미만이면 NEAR 후보가 된다")
    void individual_near() {
        // given
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(individualRow(10L, 1L, "카페 할인", 10_000L, 9_000L))); // 90%

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then
        assertThat(result).hasSize(1);
        BenefitLimitCandidate candidate = result.get(0);
        assertThat(candidate.unit()).isEqualTo(BenefitLimitUnit.INDIVIDUAL);
        assertThat(candidate.status()).isEqualTo(BenefitLimitStatus.NEAR);
        assertThat(candidate.deduplicationKey()).isEqualTo("BENEFIT_LIMIT:B:10:1:2026-08:NEAR");
    }

    @Test
    @DisplayName("개별 혜택 - 100% 이상이면 EXHAUSTED 후보가 된다")
    void individual_exhausted() {
        // given
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(individualRow(11L, 2L, "주유 할인", 10_000L, 10_000L))); // 100%

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(BenefitLimitStatus.EXHAUSTED);
        assertThat(result.get(0).deduplicationKey()).isEqualTo("BENEFIT_LIMIT:B:11:2:2026-08:EXHAUSTED");
    }

    @Test
    @DisplayName("개별 혜택 - 80% 미만이면 후보가 아니다")
    void individual_belowThreshold_noCandidate() {
        // given
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(individualRow(12L, 3L, "영화 할인", 10_000L, 7_999L))); // 79.99%

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("개별 혜택 - QUARTER 실적을 요구하면 항상 1차 대상에서 제외된다")
    void individual_quarterRequired_excluded() {
        // given: 사용량은 100%지만 QUARTER 요구라 애초에 평가 대상이 아니다.
        BenefitCandidateRow row = benefitRow(13L, 1L, 100L, "카드A", 4L, "여행 적립", null,
            "Y", "QUARTER", 10_000L, true, 10_000L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07")).thenReturn(List.of(row));

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("개별 혜택 - MONTH 실적을 요구하는데 미충족이면 제외된다")
    void individual_monthRequired_notMet_excluded() {
        // given
        BenefitCandidateRow row = benefitRow(14L, 1L, 100L, "카드A", 5L, "쇼핑 할인", null,
            "Y", "MONTH", 10_000L, false, 10_000L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07")).thenReturn(List.of(row));

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("개별 혜택 - MONTH 실적을 요구하고 충족했으면 포함된다")
    void individual_monthRequired_met_included() {
        // given
        BenefitCandidateRow row = benefitRow(15L, 1L, 100L, "카드A", 6L, "통신 할인", null,
            "Y", "MONTH", 10_000L, true, 9_500L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07")).thenReturn(List.of(row));

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("개별 혜택 - 유효 월 한도가 NULL이면 제외된다")
    void individual_nullLimit_excluded() {
        // given
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(individualRow(16L, 7L, "무제한 혜택", null, 10_000L)));

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("개별 혜택 - 유효 월 한도가 0이면 제외된다")
    void individual_zeroLimit_excluded() {
        // given
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(individualRow(17L, 8L, "혜택 없음", 0L, 0L)));

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("그룹 혜택 - 적격 구성원의 사용량을 합산해 그룹 후보를 만든다")
    void group_success_sumsEligibleMembers() {
        // given: 같은 그룹, 같은 한도(20,000원)를 가진 두 혜택. 합산 사용량 18,000 = 90%.
        BenefitCandidateRow member1 = benefitRow(20L, 1L, 200L, "카드B", 10L, "생활 할인1", "LIVING",
            "N", "MONTH", 20_000L, true, 10_000L);
        BenefitCandidateRow member2 = benefitRow(20L, 1L, 200L, "카드B", 11L, "생활 할인2", "LIVING",
            "N", "MONTH", 20_000L, true, 8_000L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(member1, member2));

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then
        assertThat(result).hasSize(1);
        BenefitLimitCandidate candidate = result.get(0);
        assertThat(candidate.unit()).isEqualTo(BenefitLimitUnit.GROUP);
        assertThat(candidate.usedAmount()).isEqualTo(18_000L);
        assertThat(candidate.status()).isEqualTo(BenefitLimitStatus.NEAR);
        assertThat(candidate.deduplicationKey()).isEqualTo("BENEFIT_LIMIT:G:20:LIVING:2026-08:NEAR");
    }

    @Test
    @DisplayName("그룹 혜택 - 구성원의 유효 월 한도가 서로 다르면 데이터 오류로 보고 건너뛴다")
    void group_limitMismatch_dataError_skipped() {
        // given: 같은 그룹인데 한도가 20,000과 30,000으로 서로 다르다.
        BenefitCandidateRow member1 = benefitRow(21L, 1L, 200L, "카드B", 12L, "묶음 혜택1", "BUNDLE",
            "N", "MONTH", 20_000L, true, 10_000L);
        BenefitCandidateRow member2 = benefitRow(21L, 1L, 200L, "카드B", 13L, "묶음 혜택2", "BUNDLE",
            "N", "MONTH", 30_000L, true, 10_000L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(member1, member2));

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("그룹 혜택 - 구성원 중 QUARTER 실적 요구가 있으면 그룹 전체가 제외된다")
    void group_hasQuarterMember_wholeGroupExcluded() {
        // given: member1은 정상, member2는 QUARTER 요구 — 그룹 전체가 제외돼야 한다.
        BenefitCandidateRow member1 = benefitRow(22L, 1L, 200L, "카드B", 14L, "정상 혜택", "MIXED",
            "N", "MONTH", 20_000L, true, 19_000L); // 혼자라면 95%로 EXHAUSTED 근처
        BenefitCandidateRow member2 = benefitRow(22L, 1L, 200L, "카드B", 15L, "분기 혜택", "MIXED",
            "Y", "QUARTER", 20_000L, true, 0L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(member1, member2));

        // when & then
        assertThat(calculator.calculate()).isEmpty();
    }

    @Test
    @DisplayName("그룹 혜택 - MONTH 실적 미충족 구성원은 합산에서만 빠지고 그룹은 살아있다")
    void group_partiallyIneligibleMember_excludedFromSum() {
        // given: member2는 MONTH 실적 미충족이라 사용량 5,000이 합산에서 빠져야 한다.
        // eligible한 member1만 합산하면 16,000 / 20,000 = 80% → NEAR.
        BenefitCandidateRow member1 = benefitRow(23L, 1L, 200L, "카드B", 16L, "정상 혜택", "PARTIAL",
            "N", "MONTH", 20_000L, true, 16_000L);
        BenefitCandidateRow member2 = benefitRow(23L, 1L, 200L, "카드B", 17L, "미충족 혜택", "PARTIAL",
            "Y", "MONTH", 20_000L, false, 5_000L);
        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(member1, member2));

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).usedAmount()).isEqualTo(16_000L);
        assertThat(result.get(0).status()).isEqualTo(BenefitLimitStatus.NEAR);
    }

    @Test
    @DisplayName("카드 통합 한도 - NEAR와 EXHAUSTED를 정확히 판정한다")
    void shared_nearAndExhausted() {
        // given
        SharedLimitCandidateRow nearRow = sharedRow(30L, 100_000L, 85_000L); // 85%
        SharedLimitCandidateRow exhaustedRow = sharedRow(31L, 50_000L, 50_000L); // 100%
        when(mapper.findSharedLimitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(nearRow, exhaustedRow));

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.unit() == BenefitLimitUnit.SHARED);
        assertThat(result)
            .extracting(BenefitLimitCandidate::userCardId, BenefitLimitCandidate::status)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(31L, BenefitLimitStatus.EXHAUSTED),
                org.assertj.core.groups.Tuple.tuple(30L, BenefitLimitStatus.NEAR)
            );
    }

    private SharedLimitCandidateRow sharedRow(long userCardId, long limit, long used) {
        return SharedLimitCandidateRow.builder()
            .userCardId(userCardId)
            .memberId(1L)
            .cardId(300L)
            .cardName("카드C")
            .sharedMonthlyLimit(limit)
            .sharedLimitUsed(used)
            .build();
    }

    @Test
    @DisplayName("정렬 - EXHAUSTED가 NEAR보다 먼저, 동일 상태 안에서는 사용률 내림차순")
    void sortOrder_exhaustedFirst_thenUsageRateDesc() {
        // given
        BenefitCandidateRow near60 = individualRow(40L, 1L, "혜택A", 10_000L, 6_000L); // NEAR? 60%는 미만이므로 후보 아님 — 90%로 조정
        // 명확한 케이스로 재구성한다.
        BenefitCandidateRow near90 = individualRow(41L, 1L, "NEAR-90", 10_000L, 9_000L);   // NEAR 90%
        BenefitCandidateRow near85 = individualRow(42L, 2L, "NEAR-85", 10_000L, 8_500L);   // NEAR 85%
        BenefitCandidateRow exhausted100 = individualRow(43L, 3L, "EXHAUSTED-100", 10_000L, 10_000L); // EXHAUSTED

        when(mapper.findBenefitCandidateRows("2026-08", "2026-07"))
            .thenReturn(List.of(near90, near85, exhausted100));

        // when
        List<BenefitLimitCandidate> result = calculator.calculate();

        // then: EXHAUSTED가 먼저, 그 다음 NEAR 중에서는 90% > 85%
        assertThat(result)
            .extracting(BenefitLimitCandidate::benefitId)
            .containsExactly(3L, 1L, 2L);
    }
}