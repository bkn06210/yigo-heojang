package com.wallet.engine.service;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.assembler.CardStateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.CardStateMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.SpendingMapper;
import com.wallet.engine.dao.UserCardMapper;
import com.wallet.engine.dao.dto.BenefitExclusionRow;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.CardPerformanceSumRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.UserCardRow;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.model.ExclusionType;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SpendingMapper spendingMapper;

    // 순수 계산기는 스테이트리스라 빈으로 두지 않는다 — 프레임워크에서 떼어 둔 설계를 유지한다
    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();

    public CardStatusService(UserCardMapper userCardMapper,
                             CardStateMapper cardStateMapper,
                             PerformanceMapper performanceMapper,
                             BenefitMapper benefitMapper,
                             CardStateAssembler cardStateAssembler,
                             PerformanceInputAssembler performanceInputAssembler,
                             CardMonthlyStatusBuilder statusBuilder,
                             CardStatusOverviewBuilder overviewBuilder,
                             SpendingMapper spendingMapper) {
        this.userCardMapper = userCardMapper;
        this.cardStateMapper = cardStateMapper;
        this.performanceMapper = performanceMapper;
        this.benefitMapper = benefitMapper;
        this.cardStateAssembler = cardStateAssembler;
        this.performanceInputAssembler = performanceInputAssembler;
        this.statusBuilder = statusBuilder;
        this.overviewBuilder = overviewBuilder;
        this.spendingMapper = spendingMapper;
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
        StatusBundle bundle = buildStatuses(memberId, baseMonth, null);
        // 카드가 없어도 조립기를 태운다 — 카드 등록을 안내하는 브리핑이 그 자리에서 나온다.
        return overviewBuilder.build(bundle.statuses(), new BriefingContext(
                baseMonth.toString(),
                bundle.benefitTargets(),
                spendingMapper.findMonthlySpendingByCategory(memberId, baseMonth.toString())));
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
        return buildStatuses(memberId, baseMonth, userCardId).statuses().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 카드별 현황과, 그 과정에서 읽은 혜택의 적용 범위(대상 업종 + 제외 업종).
     *
     * 함께 내보내는 것은 브리핑이 "결제한 업종인데 혜택을 못 받는" 자리를 찾기 위해서다.
     * 혜택 조회는 카드마다 판정된 실적 구간으로 이뤄지므로, 여기서 흘려보내면 같은 조회를
     * 한 번 더 해야 한다. 현황 응답에는 넣지 않는다 — 화면이 쓰지 않는 값이다.
     */
    private record StatusBundle(List<CardMonthlyStatus> statuses,
                                Map<Long, BriefingBenefitTarget> benefitTargets) {

        static StatusBundle empty() {
            return new StatusBundle(List.of(), Map.of());
        }
    }

    /**
     * 카드별 현황을 조립한다.
     *
     * 상태·소진·실적구간은 회원 단위로 한 번에 읽어 카드 수와 무관하게 쿼리 수를 고정한다.
     * 혜택 조회만 카드별로 남는데, 조회 키인 tierId가 카드마다 실적 판정을 거쳐야 나오기 때문이다.
     *
     * @param onlyUserCardId 개별 조회면 그 카드 ID, 전체 조회면 null
     */
    private StatusBundle buildStatuses(long memberId, YearMonth baseMonth, Long onlyUserCardId) {
        List<UserCardRow> cards = userCardMapper.findActiveCards(memberId).stream()
                .filter(card -> onlyUserCardId == null || onlyUserCardId == card.getUserCardId())
                .toList();
        if (cards.isEmpty()) {
            return StatusBundle.empty();
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
        // 혜택 id는 카드가 달라도 겹치지 않으므로 카드별로 나누지 않고 한 표에 모은다.
        Map<Long, BriefingBenefitTarget> benefitTargets = new HashMap<>();
        for (UserCardRow card : cards) {
            statuses.add(buildStatus(card, baseYearMonth, previousYearMonth,
                    statesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    usagesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    tiersByCard.getOrDefault(card.getCardId(), List.of()),
                    quarterPerformanceByUserCard.getOrDefault(card.getUserCardId(), 0L),
                    benefitTargets));
        }
        return new StatusBundle(statuses, benefitTargets);
    }

    /** 카드 한 장의 현황. 조회한 값을 계산기에 물려주기만 하고 새 계산 규칙은 두지 않는다. */
    private CardMonthlyStatus buildStatus(UserCardRow card, String baseYearMonth, String previousYearMonth,
                                          List<CardMonthlyStateRow> stateRows, List<BenefitUsageRow> usageRows,
                                          List<PerformanceTierRow> tierRows, long quarterPerformanceAmount,
                                          Map<Long, BriefingBenefitTarget> benefitTargets) {
        long prevPerformanceAmount = cardStateAssembler.resolvePrevPerformanceAmount(
                stateRows, baseYearMonth, previousYearMonth);

        // 구간이 없으면 judge가 예외를 던진다 — 모든 카드가 0원 구간을 갖는다는 전제가 깨진 시드 오류다.
        // 실적 진행률(달성률·남은실적)은 전월 축 기준이라 분기 구간표를 섞으면 목표 금액이 뒤바뀐다.
        List<PerformanceTier> tiers = performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH);
        PerformanceStatus status = tierResolver.judge(tiers, prevPerformanceAmount);

        // 전분기 축은 개별한도 조회 키이자 분기 실적 혜택의 충족 판정 기준이다. tierId만 뽑아 쓰면
        // 그 혜택이 전월 실적으로 판정돼, 분기 실적이 모자란데도 받을 수 있다고 응답한다.
        List<PerformanceTier> quarterTiers =
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.QUARTER);
        PerformanceStatus quarterStatus = quarterTiers.isEmpty()
                ? null
                : tierResolver.judge(quarterTiers, quarterPerformanceAmount);

        // 혜택별 개별한도는 구간마다 다르므로 판정된 구간 기준으로 조회한다
        List<BenefitRow> benefitRows = benefitMapper.findActiveBenefits(
                card.getCardId(), status.tierId(), quarterStatus == null ? null : quarterStatus.tierId());
        Map<Long, Long> usedAmountByBenefit = usageRows.stream()
                .collect(Collectors.toMap(BenefitUsageRow::getBenefitId, BenefitUsageRow::getUsedAmount));

        // 업종을 겨냥한 혜택만 담는다. 전 가맹점·특정 가맹점 혜택은 소비 업종과 맞춰볼 기준이 없다.
        // 제외 업종을 함께 담는 이유는, 대상만 보고 권하면 "교통 10%, 단 고속버스 제외" 같은 혜택을
        // 고속버스 소비를 근거로 권하게 되기 때문이다.
        Map<Long, Set<String>> excludedCategories = excludedCategoriesByBenefit(card.getCardId());
        for (BenefitRow benefit : benefitRows) {
            if (benefit.getTargetCategoryId() != null) {
                benefitTargets.put(benefit.getBenefitId(), new BriefingBenefitTarget(
                        benefit.getTargetCategoryId(),
                        excludedCategories.getOrDefault(benefit.getBenefitId(), Set.of())));
            }
        }

        return statusBuilder.build(
                card.getUserCardId(), card.getCardName(), baseYearMonth,
                prevPerformanceAmount, currentPerformanceAmount(stateRows, baseYearMonth),
                sharedLimitUsed(stateRows, baseYearMonth),
                tiers, benefitRows, usedAmountByBenefit, quarterStatus);
    }

    /**
     * 카드의 혜택별 제외 업종 코드.
     *
     * <b>업종 축만 추린다.</b> 브리핑이 맞춰보는 상대가 업종별 소비 집계라, 결제수단·가맹점·거래속성
     * 제외는 대조할 값 자체가 없다(그 축들은 결제 시점에 BenefitMatcher가 판정한다).
     */
    private Map<Long, Set<String>> excludedCategoriesByBenefit(long cardId) {
        return benefitMapper.findExclusions(cardId).stream()
                .filter(row -> ExclusionType.CATEGORY.name().equals(row.getExclusionType()))
                .collect(Collectors.groupingBy(
                        BenefitExclusionRow::getBenefitId,
                        Collectors.mapping(BenefitExclusionRow::getExclusionValue, Collectors.toSet())));
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
