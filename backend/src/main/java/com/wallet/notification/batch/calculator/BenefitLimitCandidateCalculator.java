package com.wallet.notification.batch.calculator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.wallet.notification.batch.model.BenefitCandidateRow;
import com.wallet.notification.batch.model.BenefitLimitCandidate;
import com.wallet.notification.batch.model.BenefitLimitStatus;
import com.wallet.notification.batch.model.BenefitLimitUnit;
import com.wallet.notification.batch.model.SharedLimitCandidateRow;
import com.wallet.notification.mapper.BenefitLimitCandidateMapper;

/**
 * 혜택 월 한도(개별·그룹·카드 통합) 임박·소진 알림 후보를 계산한다 (규칙 문서 3장).
 *
 * 실적 부족(2장)과 달리 이 알림은 "오늘이 며칠인가"로 실행 여부가 갈리지 않는다 —
 * 배치는 매일 돌고, 그날의 사용량이 임계값을 넘었으면 그날 바로 후보가 된다.
 * 그래서 Clock은 "오늘이 몇 년 몇 월인가"(baseYearMonth 계산)에만 쓰인다.
 */
@Component
@RequiredArgsConstructor
public class BenefitLimitCandidateCalculator {
    private static final Logger log = LoggerFactory.getLogger(BenefitLimitCandidateCalculator.class);

    // 3.7절 정렬: 1순위 EXHAUSTED > NEAR, 2순위 사용률 내림차순, 3순위 한도금액 내림차순, 4순위 dedupKey 오름차순.
    private static final Comparator<BenefitLimitCandidate> CANDIDATE_ORDER =
        Comparator.comparing((BenefitLimitCandidate c) -> statusRank(c.status()))
            .thenComparing(Comparator.comparingDouble(BenefitLimitCandidate::usageRate).reversed())
            .thenComparing(Comparator.comparingLong(BenefitLimitCandidate::limitAmount).reversed())
            .thenComparing(BenefitLimitCandidate::deduplicationKey);

    private final BenefitLimitCandidateMapper mapper;
    private final Clock clock;

    public List<BenefitLimitCandidate> calculate() {
        LocalDate today = LocalDate.now(clock);
        YearMonth currentYearMonth = YearMonth.from(today);
        String baseYearMonth = currentYearMonth.toString();
        String prevYearMonth = currentYearMonth.minusMonths(1).toString();

        List<BenefitCandidateRow> benefitRows =
            mapper.findBenefitCandidateRows(baseYearMonth, prevYearMonth);
        List<SharedLimitCandidateRow> sharedRows =
            mapper.findSharedLimitCandidateRows(baseYearMonth, prevYearMonth);

        Stream<BenefitLimitCandidate> individualCandidates = benefitRows.stream()
            .filter(row -> row.getLimitGroupCode() == null)
            .filter(this::isEligible)
            .map(row -> toIndividualCandidate(row, baseYearMonth))
            .flatMap(Optional::stream);

        Stream<BenefitLimitCandidate> groupCandidates = groupByCardAndLimitGroup(benefitRows)
            .entrySet().stream()
            .map(entry -> toGroupCandidate(entry.getKey(), entry.getValue(), baseYearMonth))
            .flatMap(Optional::stream);

        Stream<BenefitLimitCandidate> sharedCandidates = sharedRows.stream()
            .map(row -> toSharedCandidate(row, baseYearMonth))
            .flatMap(Optional::stream);

        return Stream.of(individualCandidates, groupCandidates, sharedCandidates)
            .flatMap(s -> s)
            .sorted(CANDIDATE_ORDER)
            .toList();
    }

    private Map<GroupKey, List<BenefitCandidateRow>> groupByCardAndLimitGroup(
        List<BenefitCandidateRow> benefitRows
    ) {
        return benefitRows.stream()
            .filter(row -> row.getLimitGroupCode() != null)
            .collect(Collectors.groupingBy(
                row -> new GroupKey(row.getUserCardId(), row.getLimitGroupCode())
            ));
    }

    // 3.3절 실적 필터: N이면 통과, Y+QUARTER면 항상 제외, Y+MONTH면 충족했을 때만 통과.
    private boolean isEligible(BenefitCandidateRow row) {
        if (!"Y".equals(row.getRequirePerformance())) {
            return true;
        }
        if ("QUARTER".equals(row.getPerformancePeriod())) {
            return false;
        }
        return row.isMonthPerformanceMet();
    }

    private boolean isQuarterRequired(BenefitCandidateRow row) {
        return "Y".equals(row.getRequirePerformance()) && "QUARTER".equals(row.getPerformancePeriod());
    }

    private Optional<BenefitLimitCandidate> toIndividualCandidate(BenefitCandidateRow row, String yearMonth) {
        Long limit = row.getEffectiveMonthlyLimit();
        if (limit == null || limit <= 0) {
            return Optional.empty();
        }

        long used = row.getUsedAmount();
        return resolveStatus(used, limit).map(status -> {
            String dedupKey = "BENEFIT_LIMIT:B:%d:%d:%s:%s"
                .formatted(row.getUserCardId(), row.getBenefitId(), yearMonth, status.name());

            return new BenefitLimitCandidate(
                row.getMemberId(), row.getUserCardId(), row.getCardId(), row.getCardName(),
                BenefitLimitUnit.INDIVIDUAL, row.getBenefitId(), row.getBenefitName(), null,
                limit, used, usageRate(used, limit), status, yearMonth, dedupKey
            );
        });
    }

    private Optional<BenefitLimitCandidate> toGroupCandidate(
        GroupKey key,
        List<BenefitCandidateRow> members,
        String yearMonth
    ) {
        // 3.4절: 그룹 구성원 중 하나라도 QUARTER 실적을 요구하면 그룹 전체를 1차 대상에서 제외한다.
        // 일부만 빼고 계산하면 실제로는 적용되지 않는 혜택의 한도까지 섞여 잘못된 사용량이 된다.
        if (members.stream().anyMatch(this::isQuarterRequired)) {
            return Optional.empty();
        }

        List<BenefitCandidateRow> eligibleMembers = members.stream()
            .filter(this::isEligible)
            .toList();

        if (eligibleMembers.isEmpty()) {
            return Optional.empty();
        }

        // 3.4절: 같은 그룹 구성원의 유효 월 한도는 모두 같아야 한다.
        // 다르면 어느 쪽이 맞는지 임의로 고를 수 없으므로 데이터 오류로 기록하고 건너뛴다.
        Set<Long> distinctLimits = eligibleMembers.stream()
            .map(BenefitCandidateRow::getEffectiveMonthlyLimit)
            .collect(Collectors.toSet());

        if (distinctLimits.size() != 1) {
            log.warn(
                "혜택 그룹 유효 월 한도 불일치 - 그룹 알림 생성을 건너뜁니다. userCardId={}, limitGroupCode={}, limits={}",
                key.userCardId(), key.limitGroupCode(), distinctLimits
            );
            return Optional.empty();
        }

        Long limit = distinctLimits.iterator().next();
        if (limit == null || limit <= 0) {
            return Optional.empty();
        }
        long finalLimit = limit;

        long usedAmount = eligibleMembers.stream()
            .mapToLong(BenefitCandidateRow::getUsedAmount)
            .sum();

        BenefitCandidateRow sample = eligibleMembers.get(0);

        return resolveStatus(usedAmount, finalLimit).map(status -> {
            String dedupKey = "BENEFIT_LIMIT:G:%d:%s:%s:%s"
                .formatted(key.userCardId(), key.limitGroupCode(), yearMonth, status.name());

            return new BenefitLimitCandidate(
                sample.getMemberId(), key.userCardId(), sample.getCardId(), sample.getCardName(),
                BenefitLimitUnit.GROUP, null, null, key.limitGroupCode(),
                finalLimit, usedAmount, usageRate(usedAmount, finalLimit), status, yearMonth, dedupKey
            );
        });
    }

    private Optional<BenefitLimitCandidate> toSharedCandidate(SharedLimitCandidateRow row, String yearMonth) {
        long limit = row.getSharedMonthlyLimit();
        long used = row.getSharedLimitUsed();

        return resolveStatus(used, limit).map(status -> {
            String dedupKey = "BENEFIT_LIMIT:S:%d:%s:%s"
                .formatted(row.getUserCardId(), yearMonth, status.name());

            return new BenefitLimitCandidate(
                row.getMemberId(), row.getUserCardId(), row.getCardId(), row.getCardName(),
                BenefitLimitUnit.SHARED, null, null, null,
                limit, used, usageRate(used, limit), status, yearMonth, dedupKey
            );
        });
    }

    // 규칙 문서 3.6절: EXHAUSTED = 사용액 >= 한도, NEAR = 80% 이상 100% 미만. 둘 다 아니면 후보가 아니다.
    private Optional<BenefitLimitStatus> resolveStatus(long used, long limit) {
        if (used >= limit) {
            return Optional.of(BenefitLimitStatus.EXHAUSTED);
        }
        if (used * 100 >= limit * 80L) {
            return Optional.of(BenefitLimitStatus.NEAR);
        }
        return Optional.empty();
    }

    private double usageRate(long used, long limit) {
        return used * 100.0 / limit;
    }

    private static int statusRank(BenefitLimitStatus status) {
        return status == BenefitLimitStatus.EXHAUSTED ? 0 : 1;
    }

    private record GroupKey(long userCardId, String limitGroupCode) {
    }
}