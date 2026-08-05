package com.wallet.engine.service;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.assembler.BenefitCandidateAssembler;
import com.wallet.engine.assembler.CardStateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.CardBenefitSelector;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.CardStateMapper;
import com.wallet.engine.dao.PaymentTargetMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.UserCardMapper;
import com.wallet.engine.dao.dto.BenefitPeriodUsageRow;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.CardPerformanceSumRow;
import com.wallet.engine.dao.dto.OptionSelectionRow;
import com.wallet.engine.dao.dto.PaymentTargetRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.UserCardRow;
import com.wallet.engine.dto.RecommendationItem;
import com.wallet.engine.dto.RecommendationRequest;
import com.wallet.engine.dto.RecommendationResponse;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CapType;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 결제 직전 카드 추천 서비스 — 보유카드마다 예상 혜택을 계산해 이득이 큰 순서로 정렬한다.
 *
 * 새로운 계산 규칙은 여기 없다. 이미 검증된 계산기들에 DB에서 꺼낸 값을 물려주는 조립 계층이다:
 * <pre>
 * 보유카드 조회
 *   ├ 상태·소진 일괄 조회 (회원 1회)
 *   ├ 실적구간  일괄 조회 (회원 1회)
 *   └ 카드마다: 전월실적 → 구간 판정 → 혜택 조회 → 카드 계산 → 최적 혜택 1개
 *        정렬(혜택액 내림차순, 동점이면 userCardId 오름차순) → 순위 부여
 * </pre>
 *
 * <b>전월실적은 저장된 집계값을 읽는다</b> — 지난달 거래를 다시 합산하지 않는다.
 * 재합산은 스냅샷 생성·보정 경로(PerformanceSnapshotService)의 몫이며 여기서 호출하지 않는다.
 *
 * 조회를 회원 단위로 묶은 것은 보유카드가 늘어도 쿼리 수가 고정되게 하기 위함이다.
 * 다만 혜택 조회만은 카드별로 남는다 — 조회 키인 tierId가 카드마다 실적 판정을 거쳐야 나오기 때문이다.
 */
@Service
public class RecommendationService {

    /** 혜택이 하나도 적용되지 않은 카드의 근거 문구 */
    private static final String NO_BENEFIT_REASON = "적용 가능한 혜택 없음";

    private final UserCardMapper userCardMapper;
    private final CardStateMapper cardStateMapper;
    private final BenefitMapper benefitMapper;
    private final PerformanceMapper performanceMapper;
    private final PaymentTargetMapper paymentTargetMapper;
    private final CardStateAssembler cardStateAssembler;
    private final BenefitCandidateAssembler benefitCandidateAssembler;
    private final PerformanceInputAssembler performanceInputAssembler;

    // 계산기는 스테이트리스 순수 함수라 빈으로 두지 않는다 — 프레임워크에서 떼어 둔 설계를 유지한다
    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();
    private final CardBenefitSelector cardBenefitSelector = new CardBenefitSelector();

    public RecommendationService(UserCardMapper userCardMapper,
                                 CardStateMapper cardStateMapper,
                                 BenefitMapper benefitMapper,
                                 PerformanceMapper performanceMapper,
                                 PaymentTargetMapper paymentTargetMapper,
                                 CardStateAssembler cardStateAssembler,
                                 BenefitCandidateAssembler benefitCandidateAssembler,
                                 PerformanceInputAssembler performanceInputAssembler) {
        this.userCardMapper = userCardMapper;
        this.cardStateMapper = cardStateMapper;
        this.benefitMapper = benefitMapper;
        this.performanceMapper = performanceMapper;
        this.paymentTargetMapper = paymentTargetMapper;
        this.cardStateAssembler = cardStateAssembler;
        this.benefitCandidateAssembler = benefitCandidateAssembler;
        this.performanceInputAssembler = performanceInputAssembler;
    }

    /**
     * 보유카드별 예상 혜택을 계산해 순위를 매긴다.
     *
     * 혜택이 없는 카드도 목록에 담는다 — 표시 개수는 화면이 자른다.
     * 보유카드가 없으면 빈 목록이다(에러가 아니다).
     *
     * 응답의 실적 미달 경고·포인트 안내는 아직 산출하지 않아 비어서 나간다
     * ({@link RecommendationResponse} 참고).
     *
     * @param memberId 회원 ID (토큰에서 추출한 값)
     * @param request  결제 예정 정보
     * @param today    기준일. 기준월과 일 소진 리셋 판정에 쓴다
     */
    @Transactional(readOnly = true)
    public RecommendationResponse recommend(long memberId, RecommendationRequest request, LocalDate today) {
        long expectedAmount = validateExpectedAmount(request);
        PaymentRequest paymentRequest = PaymentRequest.estimated(
                resolvePaymentTarget(request), expectedAmount, request.getPaymentType());

        List<UserCardRow> cards = userCardMapper.findActiveCards(memberId);
        if (cards.isEmpty()) {
            return RecommendationResponse.of(List.of());
        }

        YearMonth baseMonth = YearMonth.from(today);
        String baseYearMonth = baseMonth.toString();
        String previousYearMonth = baseMonth.minusMonths(1).toString();

        Map<Long, List<CardMonthlyStateRow>> statesByUserCard = cardStateMapper
                .findStates(memberId, baseYearMonth, previousYearMonth).stream()
                .collect(Collectors.groupingBy(CardMonthlyStateRow::getUserCardId));
        Map<Long, List<BenefitUsageRow>> usagesByUserCard = cardStateMapper
                .findUsages(memberId, baseYearMonth).stream()
                .collect(Collectors.groupingBy(BenefitUsageRow::getUserCardId));
        // 분기·연 한도는 저장값이 없어 월 소진 행을 그 기간만큼 합산해 얻는다
        Map<Long, List<BenefitPeriodUsageRow>> quarterUsagesByUserCard = cardStateMapper
                .findPeriodUsages(memberId, UsagePeriod.quarterStart(baseMonth).toString(), baseYearMonth).stream()
                .collect(Collectors.groupingBy(BenefitPeriodUsageRow::getUserCardId));
        Map<Long, List<BenefitPeriodUsageRow>> yearUsagesByUserCard = cardStateMapper
                .findPeriodUsages(memberId, UsagePeriod.yearStart(baseMonth).toString(), baseYearMonth).stream()
                .collect(Collectors.groupingBy(BenefitPeriodUsageRow::getUserCardId));
        Map<Long, Map<String, String>> optionSelectionsByUserCard = cardStateMapper
                .findOptionSelections(memberId, baseYearMonth).stream()
                .collect(Collectors.groupingBy(OptionSelectionRow::getUserCardId,
                        Collectors.toMap(OptionSelectionRow::getOptionGroupCode,
                                OptionSelectionRow::getSelectedOptionKey)));
        // 전분기 실적 — 분기 구간표를 쓰는 혜택의 판정 기준. 전월실적과 같이 저장 집계값을 읽는다
        Map<Long, Long> quarterPerformanceByUserCard = cardStateMapper
                .findPerformanceSums(memberId,
                        UsagePeriod.previousQuarterStart(baseMonth).toString(),
                        UsagePeriod.previousQuarterEnd(baseMonth).toString()).stream()
                .collect(Collectors.toMap(
                        CardPerformanceSumRow::getUserCardId, CardPerformanceSumRow::getPerformanceAmount));
        Map<Long, List<PerformanceTierRow>> tiersByCard = performanceMapper
                .findTiersByCardIds(cards.stream().map(UserCardRow::getCardId).distinct().toList()).stream()
                .collect(Collectors.groupingBy(PerformanceTierRow::getCardId));

        List<CardOutcome> outcomes = new ArrayList<>();
        for (UserCardRow card : cards) {
            outcomes.add(evaluateCard(card, paymentRequest, today, baseYearMonth, previousYearMonth,
                    statesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    usagesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    quarterUsagesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    yearUsagesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    optionSelectionsByUserCard.getOrDefault(card.getUserCardId(), Map.of()),
                    quarterPerformanceByUserCard.getOrDefault(card.getUserCardId(), 0L),
                    tiersByCard.getOrDefault(card.getCardId(), List.of())));
        }
        return RecommendationResponse.of(toRankedItems(outcomes));
    }

    /** 카드 한 장의 실적 판정 → 혜택 계산. 여기까지는 순위를 모른다. */
    private CardOutcome evaluateCard(UserCardRow card, PaymentRequest paymentRequest, LocalDate today,
                                     String baseYearMonth, String previousYearMonth,
                                     List<CardMonthlyStateRow> stateRows, List<BenefitUsageRow> usageRows,
                                     List<BenefitPeriodUsageRow> quarterUsageRows,
                                     List<BenefitPeriodUsageRow> yearUsageRows,
                                     Map<String, String> selectedOptionKeys,
                                     long quarterPerformanceAmount,
                                     List<PerformanceTierRow> tierRows) {
        long prevPerformanceAmount = cardStateAssembler.resolvePrevPerformanceAmount(
                stateRows, baseYearMonth, previousYearMonth);

        // 구간이 없으면 judge가 예외를 던진다 — 모든 카드가 0원 구간을 갖는다는 전제가 깨진 시드 오류다.
        // 조용히 넘기면 실적 조건이 있는 혜택이 통째로 사라지므로 드러나게 둔다.
        List<PerformanceTier> monthTiers = performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH);
        PerformanceStatus status = tierResolver.judge(monthTiers, prevPerformanceAmount);
        PerformanceStatus quarterStatus = judgeQuarter(tierRows, quarterPerformanceAmount);

        CardState cardState = cardStateAssembler.toCardState(
                stateRows, baseYearMonth, usageRows, quarterUsageRows, yearUsageRows,
                selectedOptionKeys, status, quarterStatus, today);

        // 판정된 구간의 개별한도를 함께 가져온다. 매칭 여부로 거르지 않는 이유는 묶음 한도 합산이다.
        List<BenefitRow> benefitRows = benefitMapper.findActiveBenefits(
                card.getCardId(), status.tierId(), quarterStatus == null ? null : quarterStatus.tierId());
        List<BenefitCandidate> candidates = benefitCandidateAssembler.toCandidates(
                benefitRows, benefitMapper.findExclusions(card.getCardId()));

        CardBenefitSelection selection = cardBenefitSelector.select(candidates, cardState, paymentRequest);
        // 동적 전환 판정용 가상 계산 — 이 결과는 응답 금액에 쓰지 않고 순위 비교에만 쓴다
        CardBenefitSelection idealSelection =
                cardBenefitSelector.select(candidates, withoutUsage(cardState), paymentRequest);

        return new CardOutcome(card.getUserCardId(), card.getCardName(), selection, idealSelection,
                buildReason(selection, benefitRows));
    }

    /**
     * 전분기 실적으로 분기 구간을 판정한다. 분기 구간표가 없는 카드면 null —
     * 판정할 구간표가 없으니 분기 실적 조건을 충족했다고 볼 근거가 없다.
     */
    private PerformanceStatus judgeQuarter(List<PerformanceTierRow> tierRows, long quarterPerformanceAmount) {
        List<PerformanceTier> quarterTiers =
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.QUARTER);
        return quarterTiers.isEmpty() ? null : tierResolver.judge(quarterTiers, quarterPerformanceAmount);
    }

    /**
     * 소진 기록만 지운 가상 상태 — "이번 달 아직 안 썼다면 얼마였을까".
     *
     * 한도 자체는 그대로 둔다. 한도까지 없애면 실제로는 받을 수 없는 금액이 나와,
     * "평소엔 이 카드가 유리하다"는 안내가 사실과 어긋난다.
     * 실적 판정 결과(performanceMet·통합한도)는 지난달 실적으로 이미 정해진 값이라 건드리지 않는다.
     * 선택형 혜택의 그달 선택도 그대로 둔다 — 지우면 고른 혜택이 후보에서 빠져,
     * 가상 계산에서만 그 카드의 혜택이 통째로 사라진다.
     */
    private CardState withoutUsage(CardState cardState) {
        return new CardState(
                cardState.monthStatus(), cardState.quarterStatus(), 0L, List.of(),
                cardState.selectedOptionKeys());
    }

    /**
     * 혜택액 내림차순으로 세우고 순위를 매긴다. 동점이면 userCardId 오름차순 —
     * 같은 입력에 같은 순서가 나오게 하는 규칙이다(테스트 재현성).
     *
     * 정렬 뒤 동적 전환을 판정한다. 소진이 없었다면 1위였을 카드와 실제 1위가 다르면,
     * 실제 1위에 표시해 "평소와 다른 카드를 추천하는 이유"를 화면이 설명할 수 있게 한다.
     */
    private List<RecommendationItem> toRankedItems(List<CardOutcome> outcomes) {
        outcomes.sort(byBenefitDesc(CardOutcome::selection));
        boolean switched = isDynamicSwitch(outcomes);

        List<RecommendationItem> items = new ArrayList<>(outcomes.size());
        for (int index = 0; index < outcomes.size(); index++) {
            CardOutcome outcome = outcomes.get(index);
            CardBenefitSelection selection = outcome.selection();
            items.add(new RecommendationItem(
                    index + 1,
                    outcome.userCardId(),
                    outcome.cardName(),
                    selection.benefitAmount(),
                    selection.estimate(),
                    selection.benefitKind(),
                    outcome.reason(),
                    // 표시는 실제 1위에만 — "이 카드를 추천하는 이유"를 붙이는 자리다
                    index == 0 && switched));
        }
        return items;
    }

    /**
     * 소진이 없었다면 1위였을 카드와 실제 1위가 다른가.
     *
     * 다르다는 것은 "원래 제일 좋은 카드가 한도·횟수를 소진해 밀려났다"는 뜻이다.
     * 가상 1위조차 혜택이 0원이면 애초에 받을 혜택이 없는 상황이라 표시하지 않는다.
     */
    private boolean isDynamicSwitch(List<CardOutcome> sortedOutcomes) {
        if (sortedOutcomes.size() < 2) {
            return false;
        }
        CardOutcome idealTop = sortedOutcomes.stream()
                .min(byBenefitDesc(CardOutcome::idealSelection))
                .orElseThrow();

        return idealTop.idealSelection().benefitAmount() > 0
                && idealTop.userCardId() != sortedOutcomes.get(0).userCardId();
    }

    /**
     * 혜택액 내림차순, 동점이면 userCardId 오름차순.
     * 실제 결과와 가상 결과에 같은 기준을 써야 "1위가 바뀌었다"는 비교가 성립한다.
     */
    private Comparator<CardOutcome> byBenefitDesc(Function<CardOutcome, CardBenefitSelection> selector) {
        return Comparator
                .comparingLong((CardOutcome outcome) -> selector.apply(outcome).benefitAmount()).reversed()
                .thenComparingLong(CardOutcome::userCardId);
    }

    /**
     * 추천 근거를 조립한다. 혜택명(benefit_name)과 계산 금액만 쓴다 —
     * 약관 원문(description)은 계산에 쓰지 않듯 근거 문구에도 쓰지 않는다.
     */
    private String buildReason(CardBenefitSelection selection, List<BenefitRow> benefitRows) {
        if (!selection.hasBenefit()) {
            return NO_BENEFIT_REASON;
        }
        BenefitRow row = benefitRows.stream()
                .filter(candidate -> candidate.getBenefitId() == selection.benefitId())
                .findFirst()
                .orElse(null);
        String benefitName = row == null ? "혜택" : row.getBenefitName();

        // 조건은 통과했는데 0원인 경우 — "0원"보다 이유를 보여준다
        if (selection.benefitAmount() == 0L) {
            return benefitName + " — " + zeroBenefitReason(selection, row);
        }
        String amount = String.format("%,d원", selection.benefitAmount());
        return selection.estimate() ? benefitName + " 예상 " + amount : benefitName + " " + amount;
    }

    /**
     * 혜택이 적용됐는데 0원인 이유.
     *
     * 0원의 원인이 하나가 아니다. 한도를 다 쓴 것과, 스탬프형(COUNT_STEP)이라 아직 지급 회차가
     * 아닌 것은 사용자가 해야 할 일이 정반대다 — 앞은 다음 달을 기다려야 하고,
     * 뒤는 몇 번 더 쓰면 받는다. 둘을 같은 문구로 묶으면 화면이 사실과 다른 말을 하게 된다.
     */
    private String zeroBenefitReason(CardBenefitSelection selection, BenefitRow row) {
        if (selection.appliedCap() != CapType.NONE) {
            return "잔여 한도 없음";
        }
        if (row != null && CalcMethod.COUNT_STEP.name().equals(row.getCalcMethod())) {
            return row.getStepCount() + "회마다 적립 (이번 결제는 해당 없음)";
        }
        return "적용 금액 없음";
    }

    /**
     * 요청의 merchantId·categoryId를 매칭에 쓸 형태로 펼친다.
     * 없는 id는 무시하지 않고 404로 실패시킨다 — 요청한 곳과 다른 기준으로 계산한 결과를 주면 안 된다.
     */
    private PaymentTarget resolvePaymentTarget(RecommendationRequest request) {
        if (request.getMerchantId() != null) {
            return toPaymentTarget(paymentTargetMapper.findByMerchantId(request.getMerchantId()));
        }
        if (request.getCategoryId() != null) {
            return toPaymentTarget(paymentTargetMapper.findByCategoryId(request.getCategoryId()));
        }
        // 장소 미정 — 전 가맹점(ALL) 혜택만 매칭된다
        return PaymentTarget.unspecified();
    }

    private PaymentTarget toPaymentTarget(PaymentTargetRow row) {
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return PaymentTarget.builder()
                .merchantId(row.getMerchantId())
                .merchantCode(row.getMerchantCode())
                .categoryId(row.getCategoryId())
                .categoryCode(row.getCategoryCode())
                .parentCategoryId(row.getParentCategoryId())
                .parentCategoryCode(row.getParentCategoryCode())
                .build();
    }

    /** 계산기에 넣기 전에 막는다 — PaymentRequest는 0 이하를 예외로 던지므로 400으로 바꿔 준다 */
    private long validateExpectedAmount(RecommendationRequest request) {
        Long expectedAmount = request.getExpectedAmount();
        if (expectedAmount == null || expectedAmount <= 0L) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
        return expectedAmount;
    }

    /**
     * 정렬 전 중간 결과 — 순위는 전체를 세운 뒤에야 정해진다.
     *
     * @param selection      실제 계산 결과. 응답 금액은 이것만 쓴다
     * @param idealSelection 소진 전이었다면 얼마였을지. 동적 전환 판정에만 쓰고 응답에 내보내지 않는다
     */
    private record CardOutcome(long userCardId, String cardName, CardBenefitSelection selection,
                               CardBenefitSelection idealSelection, String reason) {
    }
}
