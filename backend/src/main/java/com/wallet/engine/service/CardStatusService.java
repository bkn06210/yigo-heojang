package com.wallet.engine.service;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.assembler.CardStateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.CardStateMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.UserCardMapper;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.CardPerformanceSumRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.UserCardRow;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카드 현황 조회 서비스 — 전체 보유 카드 현황(#2)과 개별 카드 상세(#3).
 *
 * 읽기 전용이다. 상태 행이 없어도 만들지 않는다 — 행 생성은 정산(결제)의 몫이고,
 * 조회가 행을 만들면 "결제도 안 했는데 상태가 생기는" 부작용이 난다. 없으면 0으로 본다.
 *
 * <pre>
 * 보유카드 조회 (개별 조회면 여기서 1장으로 좁힌다)
 *   ├ 상태·소진 일괄 조회 (회원 1회)
 *   ├ 실적구간   일괄 조회 (회원 1회)
 *   └ 카드마다: 전월실적 → 구간 판정 → 그 구간의 혜택 조회 → 현황 조립
 * </pre>
 *
 * <b>두 API가 같은 경로를 쓴다.</b> 개별 조회는 카드 목록을 1장으로 좁힐 뿐 조립 로직이 같다.
 * 목록과 상세가 다른 코드로 계산되면 같은 카드의 달성률이 화면마다 다르게 보일 수 있는데,
 * 경로를 하나로 두면 그 어긋남이 생길 수 없다.
 *
 * 전월실적은 <b>저장된 집계값</b>을 읽는다(지난달 거래 재합산 아님) — 추천과 같은 규칙이며,
 * 재합산은 스냅샷 생성·보정 경로(PerformanceSnapshotService)의 몫이다.
 */
@Service
public class CardStatusService {

    private final UserCardMapper userCardMapper;
    private final CardStateMapper cardStateMapper;
    private final PerformanceMapper performanceMapper;
    private final BenefitMapper benefitMapper;
    private final CardStateAssembler cardStateAssembler;
    private final PerformanceInputAssembler performanceInputAssembler;
    private final CardMonthlyStatusBuilder statusBuilder;
    private final CardStatusOverviewBuilder overviewBuilder;

    // 순수 계산기는 스테이트리스라 빈으로 두지 않는다 — 프레임워크에서 떼어 둔 설계를 유지한다
    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();

    public CardStatusService(UserCardMapper userCardMapper,
                             CardStateMapper cardStateMapper,
                             PerformanceMapper performanceMapper,
                             BenefitMapper benefitMapper,
                             CardStateAssembler cardStateAssembler,
                             PerformanceInputAssembler performanceInputAssembler,
                             CardMonthlyStatusBuilder statusBuilder,
                             CardStatusOverviewBuilder overviewBuilder) {
        this.userCardMapper = userCardMapper;
        this.cardStateMapper = cardStateMapper;
        this.performanceMapper = performanceMapper;
        this.benefitMapper = benefitMapper;
        this.cardStateAssembler = cardStateAssembler;
        this.performanceInputAssembler = performanceInputAssembler;
        this.statusBuilder = statusBuilder;
        this.overviewBuilder = overviewBuilder;
    }

    /**
     * 보유 카드 전부의 현황과 홈 브리핑을 반환한다.
     * 보유 카드가 없으면 에러가 아니라 빈 응답이다(카드를 아직 등록하지 않은 정상 상태).
     *
     * @param memberId  회원 ID (토큰에서 추출 — 소유권 필터)
     * @param baseMonth 기준 연월
     */
    @Transactional(readOnly = true)
    public CardStatusOverview getOverview(long memberId, YearMonth baseMonth) {
        List<CardMonthlyStatus> statuses = buildStatuses(memberId, baseMonth, null);
        if (statuses.isEmpty()) {
            return CardStatusOverview.empty();
        }
        return overviewBuilder.build(statuses);
    }

    /**
     * 보유 카드 한 장의 상세 현황을 반환한다.
     *
     * 없는 카드와 남의 카드를 <b>똑같이 404</b>로 돌려준다 — 403으로 구분하면 "그 id의 카드는
     * 존재한다"는 사실이 새어 나간다. 조회 자체가 회원 소유로 제한돼 있어 목록에 없으면 404다.
     *
     * @param userCardId 보유 카드 ID
     */
    @Transactional(readOnly = true)
    public CardMonthlyStatus getCardStatus(long memberId, long userCardId, YearMonth baseMonth) {
        return buildStatuses(memberId, baseMonth, userCardId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 카드별 현황을 조립한다.
     *
     * 상태·소진·실적구간은 회원 단위로 한 번에 읽어 카드 수와 무관하게 쿼리 수를 고정한다.
     * 혜택 조회만 카드별로 남는데, 조회 키인 tierId가 카드마다 실적 판정을 거쳐야 나오기 때문이다.
     *
     * @param onlyUserCardId 개별 조회면 그 카드 ID, 전체 조회면 null
     */
    private List<CardMonthlyStatus> buildStatuses(long memberId, YearMonth baseMonth, Long onlyUserCardId) {
        List<UserCardRow> cards = userCardMapper.findActiveCards(memberId).stream()
                .filter(card -> onlyUserCardId == null || onlyUserCardId == card.getUserCardId())
                .toList();
        if (cards.isEmpty()) {
            return List.of();
        }

        String baseYearMonth = baseMonth.toString();
        String previousYearMonth = baseMonth.minusMonths(1).toString();

        Map<Long, List<CardMonthlyStateRow>> statesByUserCard = cardStateMapper
                .findStates(memberId, baseYearMonth, previousYearMonth).stream()
                .collect(Collectors.groupingBy(CardMonthlyStateRow::getUserCardId));
        Map<Long, List<BenefitUsageRow>> usagesByUserCard = cardStateMapper
                .findUsages(memberId, baseYearMonth).stream()
                .collect(Collectors.groupingBy(BenefitUsageRow::getUserCardId));
        Map<Long, List<PerformanceTierRow>> tiersByCard = performanceMapper
                .findTiersByCardIds(cards.stream().map(UserCardRow::getCardId).distinct().toList()).stream()
                .collect(Collectors.groupingBy(PerformanceTierRow::getCardId));
        // 전분기 실적 — 분기 구간표를 쓰는 혜택의 개별한도 조회 키를 정한다
        Map<Long, Long> quarterPerformanceByUserCard = cardStateMapper
                .findPerformanceSums(memberId,
                        UsagePeriod.previousQuarterStart(baseMonth).toString(),
                        UsagePeriod.previousQuarterEnd(baseMonth).toString()).stream()
                .collect(Collectors.toMap(
                        CardPerformanceSumRow::getUserCardId, CardPerformanceSumRow::getPerformanceAmount));

        List<CardMonthlyStatus> statuses = new ArrayList<>(cards.size());
        for (UserCardRow card : cards) {
            statuses.add(buildStatus(card, baseYearMonth, previousYearMonth,
                    statesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    usagesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    tiersByCard.getOrDefault(card.getCardId(), List.of()),
                    quarterPerformanceByUserCard.getOrDefault(card.getUserCardId(), 0L)));
        }
        return statuses;
    }

    /** 카드 한 장의 현황. 조회한 값을 계산기에 물려주기만 하고 새 계산 규칙은 두지 않는다. */
    private CardMonthlyStatus buildStatus(UserCardRow card, String baseYearMonth, String previousYearMonth,
                                          List<CardMonthlyStateRow> stateRows, List<BenefitUsageRow> usageRows,
                                          List<PerformanceTierRow> tierRows, long quarterPerformanceAmount) {
        long prevPerformanceAmount = cardStateAssembler.resolvePrevPerformanceAmount(
                stateRows, baseYearMonth, previousYearMonth);

        // 구간이 없으면 judge가 예외를 던진다 — 모든 카드가 0원 구간을 갖는다는 전제가 깨진 시드 오류다.
        // 실적 진행률(달성률·남은실적)은 전월 축 기준이라 분기 구간표를 섞으면 목표 금액이 뒤바뀐다.
        List<PerformanceTier> tiers = performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH);
        PerformanceStatus status = tierResolver.judge(tiers, prevPerformanceAmount);

        List<PerformanceTier> quarterTiers =
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.QUARTER);
        Long quarterTierId = quarterTiers.isEmpty()
                ? null
                : tierResolver.judge(quarterTiers, quarterPerformanceAmount).tierId();

        // 혜택별 개별한도는 구간마다 다르므로 판정된 구간 기준으로 조회한다
        List<BenefitRow> benefitRows = benefitMapper.findActiveBenefits(
                card.getCardId(), status.tierId(), quarterTierId);
        Map<Long, Long> usedAmountByBenefit = usageRows.stream()
                .collect(Collectors.toMap(BenefitUsageRow::getBenefitId, BenefitUsageRow::getUsedAmount));

        return statusBuilder.build(
                card.getUserCardId(), card.getCardName(), baseYearMonth,
                prevPerformanceAmount, currentPerformanceAmount(stateRows, baseYearMonth),
                sharedLimitUsed(stateRows, baseYearMonth),
                tiers, benefitRows, usedAmountByBenefit);
    }

    /** 기준월 누적 실적인정액. 그 달에 아직 결제가 없으면 행이 없고, 그때 0은 오류가 아니라 정답이다. */
    private long currentPerformanceAmount(List<CardMonthlyStateRow> stateRows, String baseYearMonth) {
        CardMonthlyStateRow baseMonthRow = findBaseMonthRow(stateRows, baseYearMonth);
        return baseMonthRow == null ? 0L : baseMonthRow.getCurrentPerformanceAmount();
    }

    /**
     * 기준월 통합한도 소진액.
     * <b>직전월 행에서 집으면 안 된다</b> — 이번 달 한도가 이미 소진된 것으로 표시된다.
     */
    private long sharedLimitUsed(List<CardMonthlyStateRow> stateRows, String baseYearMonth) {
        CardMonthlyStateRow baseMonthRow = findBaseMonthRow(stateRows, baseYearMonth);
        return baseMonthRow == null ? 0L : baseMonthRow.getSharedLimitUsed();
    }

    private CardMonthlyStateRow findBaseMonthRow(List<CardMonthlyStateRow> stateRows, String baseYearMonth) {
        return stateRows.stream()
                .filter(row -> baseYearMonth.equals(row.getBaseYearMonth()))
                .findFirst()
                .orElse(null);
    }
}
