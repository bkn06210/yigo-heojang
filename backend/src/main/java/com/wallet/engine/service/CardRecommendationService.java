package com.wallet.engine.service;

import com.wallet.engine.assembler.BenefitCandidateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.CardPortfolioSimulator;
import com.wallet.engine.calculator.CardSpendingSimulator;
import com.wallet.engine.calculator.PerformanceAmountCalculator;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.CardCatalogMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.SpendingMapper;
import com.wallet.engine.dao.UserCardMapper;
import com.wallet.engine.dao.dto.BenefitExclusionRow;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dao.dto.CardTierSelector;
import com.wallet.engine.dao.dto.PerformanceExclusionRow;
import com.wallet.engine.dao.dto.CardCatalogRow;
import com.wallet.engine.dao.dto.OptionKeyRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.SpentTransactionRow;
import com.wallet.engine.dao.dto.UserCardRow;
import com.wallet.engine.dto.CardRecommendation;
import com.wallet.engine.dto.CardRecommendationResponse;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import com.wallet.engine.model.PerformanceTransaction;
import com.wallet.engine.model.PortfolioSimulation;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 소비 내역을 근거로 카드를 추천한다 — 보유 카드와 미보유 카드를 함께 견준다.
 *
 * <b>무엇을 계산하는가</b>
 * <pre>
 * X = 지금 보유 카드들로 지난달 결제를 최적 배분 → 총 혜택
 * Y = 보유 카드 + 후보 한 장으로 최적 배분      → 총 혜택
 * 순증 = Y − X
 * </pre>
 *
 * 후보를 안 써도 되므로 순증은 음수가 될 수 없다. "카페는 늘지만 마트는 주는" 손해가 구조적으로
 * 생기지 않고, 얼마를 그 카드로 옮길지 가정할 필요도 없다 — 배분을 계산이 정한다.
 *
 * <b>실적을 무엇으로 보는가</b> — 지난달 소비 전부를 그 카드로 결제했다고 보고 판정한다.
 * 아직 쓰지 않은 카드에는 실적 이력이 없고, 배분 결과로 실적을 정하면 배분과 실적이 서로를
 * 참조해 순환한다. 카드를 발급한다면 그 카드를 주력으로 쓴다고 보는 것이 자연스럽다.
 * 실적 제외 항목은 카드마다 다르므로 카드별로 뺀다.
 *
 * <b>연회비는 정렬에 넣지 않는다.</b> 몇 달 쓸지 모르는 값을 순위에 섞으면 보이지 않는 가정이
 * 순위를 흔든다. 대신 손익분기 개월(연회비 ÷ 월 순증)을 함께 내려 회원이 판단하게 한다.
 */
@Service
public class CardRecommendationService {

    /**
     * 시뮬레이션할 미보유 카드 수 상한.
     *
     * 후보 하나마다 한 달치 결제를 다시 흘려보내므로 비용이 후보 수에 비례한다.
     * 맞는 혜택이 많은 순으로 잘라도 상위가 바뀌지 않는다 — 그 회원이 쓰지 않는 업종의 카드는
     * 어차피 순증이 0이다.
     */
    private static final int MAX_CANDIDATE_CARDS = 10;

    private final SpendingMapper spendingMapper;
    private final UserCardMapper userCardMapper;
    private final BenefitMapper benefitMapper;
    private final PerformanceMapper performanceMapper;
    private final CardCatalogMapper cardCatalogMapper;
    private final BenefitCandidateAssembler benefitCandidateAssembler;
    private final PerformanceInputAssembler performanceInputAssembler;
    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();
    private final PerformanceAmountCalculator performanceAmountCalculator = new PerformanceAmountCalculator();
    private final CardPortfolioSimulator portfolioSimulator = new CardPortfolioSimulator();
    private final CardSpendingSimulator spendingSimulator = new CardSpendingSimulator();

    public CardRecommendationService(SpendingMapper spendingMapper,
                                     UserCardMapper userCardMapper,
                                     BenefitMapper benefitMapper,
                                     PerformanceMapper performanceMapper,
                                     CardCatalogMapper cardCatalogMapper,
                                     BenefitCandidateAssembler benefitCandidateAssembler,
                                     PerformanceInputAssembler performanceInputAssembler) {
        this.spendingMapper = spendingMapper;
        this.userCardMapper = userCardMapper;
        this.benefitMapper = benefitMapper;
        this.performanceMapper = performanceMapper;
        this.cardCatalogMapper = cardCatalogMapper;
        this.benefitCandidateAssembler = benefitCandidateAssembler;
        this.performanceInputAssembler = performanceInputAssembler;
    }

    /**
     * @param memberId 회원 ID (토큰에서 추출한 값)
     * @param today    기준일. 직전월 소비를 근거로 삼는다
     */
    @Transactional(readOnly = true)
    public CardRecommendationResponse recommend(long memberId, LocalDate today) {
        String previousYearMonth = YearMonth.from(today).minusMonths(1).toString();
        List<SpentTransactionRow> transactions =
                spendingMapper.findMonthlyTransactions(memberId, previousYearMonth);
        if (transactions.isEmpty()) {
            // 소비가 없으면 근거가 없다. 빈 응답이지 에러가 아니다
            return CardRecommendationResponse.of(previousYearMonth, 0L, List.of());
        }

        List<SimulatedPayment> payments = transactions.stream().map(this::toPayment).toList();
        List<PerformanceTransaction> performanceInput = transactions.stream()
                .map(this::toPerformanceTransaction).toList();

        List<UserCardRow> heldCards = userCardMapper.findActiveCards(memberId);
        List<Long> heldCardIds = heldCards.stream().map(UserCardRow::getCardId).distinct().toList();
        List<Long> candidateCardIds = findCandidateCardIds(transactions, heldCardIds);

        List<Long> allCardIds = new ArrayList<>(heldCardIds);
        allCardIds.addAll(candidateCardIds);
        if (allCardIds.isEmpty()) {
            return CardRecommendationResponse.of(previousYearMonth, 0L, List.of());
        }

        // 조회를 카드 목록 단위로 한 번씩만 한다. 카드마다 돌면서 조회하면 쿼리 수가
        // 카드 수에 비례해 늘고, 후보를 넓힐 때 그대로 비용이 된다.
        Map<Long, List<PerformanceTierRow>> tiersByCard = performanceMapper.findTiersByCardIds(allCardIds)
                .stream().collect(Collectors.groupingBy(PerformanceTierRow::getCardId));
        Map<Long, List<PerformanceExclusionRow>> performanceExclusionsByCard = performanceMapper
                .findExclusionsByCardIds(allCardIds).stream()
                .collect(Collectors.groupingBy(PerformanceExclusionRow::getCardId));

        // 실적 판정이 먼저다 — 혜택 조회에 카드별 구간 ID가 필요하다
        Map<Long, PerformanceStatus[]> statusByCard = new LinkedHashMap<>();
        for (Long cardId : allCardIds) {
            statusByCard.put(cardId, judgePerformance(performanceInput,
                    tiersByCard.getOrDefault(cardId, List.of()),
                    performanceExclusionsByCard.getOrDefault(cardId, List.of())));
        }

        List<CardTierSelector> selectors = allCardIds.stream()
                .map(cardId -> new CardTierSelector(cardId,
                        statusByCard.get(cardId)[0].tierId(),
                        statusByCard.get(cardId)[1] == null ? null : statusByCard.get(cardId)[1].tierId()))
                .toList();
        Map<Long, List<BenefitExclusionRow>> exclusionsByCard = benefitMapper
                .findExclusionsByCards(allCardIds).stream()
                .collect(Collectors.groupingBy(BenefitExclusionRow::getCardId));
        Map<Long, List<BenefitRow>> benefitsByCard = benefitMapper.findActiveBenefitsByCards(selectors)
                .stream().collect(Collectors.groupingBy(BenefitRow::getCardId));
        Map<Long, List<OptionKeyRow>> optionKeysByCard = benefitMapper
                .findOptionKeysByCards(allCardIds).stream()
                .collect(Collectors.groupingBy(OptionKeyRow::getCardId));

        Map<Long, List<BenefitCandidate>> candidatesByCard = new LinkedHashMap<>();
        for (Long cardId : allCardIds) {
            candidatesByCard.put(cardId, benefitCandidateAssembler.toCandidates(
                    benefitsByCard.getOrDefault(cardId, List.of()),
                    exclusionsByCard.getOrDefault(cardId, List.of())));
        }

        List<SimulatedCard> heldSimulated = heldCardIds.stream()
                .map(cardId -> toSimulatedCard(cardId, payments, statusByCard.get(cardId),
                        candidatesByCard.get(cardId), optionKeysByCard.getOrDefault(cardId, List.of())))
                .toList();

        PortfolioSimulation current = portfolioSimulator.simulate(heldSimulated, payments);

        Map<Long, CardCatalogRow> catalog = cardCatalogMapper.findByCardIds(candidateCardIds).stream()
                .collect(Collectors.toMap(CardCatalogRow::getCardId, row -> row));

        List<CardRecommendation> recommendations = new ArrayList<>();
        for (Long candidateCardId : candidateCardIds) {
            SimulatedCard candidate = toSimulatedCard(candidateCardId, payments,
                    statusByCard.get(candidateCardId), candidatesByCard.get(candidateCardId),
                    optionKeysByCard.getOrDefault(candidateCardId, List.of()));

            List<SimulatedCard> withCandidate = new ArrayList<>(heldSimulated);
            withCandidate.add(candidate);
            long gain = portfolioSimulator.simulate(withCandidate, payments).totalBenefitAmount()
                    - current.totalBenefitAmount();
            if (gain <= 0) {
                // 이 카드로 옮길 만한 결제가 없다. 순증 0을 목록에 넣으면 고를 이유 없는 카드가 섞인다
                continue;
            }
            CardCatalogRow row = catalog.get(candidateCardId);
            if (row == null) {
                continue;
            }
            recommendations.add(CardRecommendation.of(
                    candidateCardId, row.getCardName(), row.getCardCompanyName(),
                    gain, row.getMinAnnualFee(), breakEvenMonths(row.getMinAnnualFee(), gain)));
        }
        recommendations.sort(CardRecommendation.BY_GAIN_DESC);
        return CardRecommendationResponse.of(
                previousYearMonth, current.totalBenefitAmount(), recommendations);
    }

    /**
     * 연회비를 월 순증으로 나눈 개월 수 — 몇 달 쓰면 연회비를 넘어서는가.
     *
     * 회원에게 "몇 달 쓸 예정이냐"를 묻지 않기 위한 값이다. 물어도 사람은 모르고, 질문만 는다.
     * 나눗셈 하나라 가정이 들어가지 않는다. 연회비가 없으면 0이다.
     */
    private int breakEvenMonths(long annualFee, long monthlyGain) {
        if (annualFee <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) annualFee / monthlyGain);
    }

    /**
     * 결제한 업종·가맹점에 혜택이 걸린 미보유 카드를 추린다.
     *
     * 대분류 id를 함께 넘기는 것은 혜택이 대분류를 겨냥할 수 있기 때문이다 — 중분류 id로만
     * 찾으면 "외식 5%" 카드가 카페 결제만 있는 회원의 후보에서 빠진다.
     */
    private List<Long> findCandidateCardIds(List<SpentTransactionRow> transactions, List<Long> heldCardIds) {
        Set<Long> categoryIds = new LinkedHashSet<>();
        Set<Long> merchantIds = new LinkedHashSet<>();
        for (SpentTransactionRow transaction : transactions) {
            if (transaction.getCategoryId() != null) {
                categoryIds.add(transaction.getCategoryId());
            }
            if (transaction.getParentCategoryId() != null) {
                categoryIds.add(transaction.getParentCategoryId());
            }
            if (transaction.getMerchantId() != null) {
                merchantIds.add(transaction.getMerchantId());
            }
        }
        if (categoryIds.isEmpty() && merchantIds.isEmpty()) {
            return List.of();
        }
        return benefitMapper.findCandidateCardIds(
                List.copyOf(categoryIds), List.copyOf(merchantIds), heldCardIds, MAX_CANDIDATE_CARDS);
    }

    /**
     * 카드 한 장의 시뮬레이션 입력을 만든다 — 실적 판정과 혜택 목록.
     *
     * 분기 축 실적은 월 실적의 3배로 본다. 분기는 석 달치라 한 달 소비를 그대로 넣으면 분기
     * 조건이 붙은 혜택이 늘 미충족으로 판정된다.
     *
     * 카드마다 혜택을 따로 조회하는 것은 실적 구간이 카드마다 달라 조회 조건이 갈리기 때문이다.
     * 후보 수를 {@link #MAX_CANDIDATE_CARDS}로 묶어 쿼리 수가 카드 수에 비례해 늘지 않게 했다.
     */
    /**
     * 카드 하나의 실적을 판정한다 — 전월 축과 전분기 축.
     *
     * 배열 두 칸으로 돌려주는 것은 두 축이 늘 짝으로 쓰이기 때문이다.
     * [0] 전월, [1] 전분기(분기 구간표가 없는 카드면 null).
     *
     * 분기 축 실적은 월 실적의 3배로 본다. 분기는 석 달치라 한 달 소비를 그대로 넣으면
     * 분기 조건이 붙은 혜택이 늘 미충족으로 판정된다.
     */
    private PerformanceStatus[] judgePerformance(List<PerformanceTransaction> performanceInput,
                                                 List<PerformanceTierRow> tierRows,
                                                 List<PerformanceExclusionRow> exclusionRows) {
        List<PerformanceTier> tiers = performanceInputAssembler.toTiers(tierRows);
        long performanceAmount = performanceAmountCalculator.calculate(performanceInput,
                performanceInputAssembler.toExclusions(exclusionRows)).amount();

        List<PerformanceTier> monthTiers = tiersOf(tiers, PerformancePeriod.MONTH);
        List<PerformanceTier> quarterTiers = tiersOf(tiers, PerformancePeriod.QUARTER);
        PerformanceStatus monthStatus = monthTiers.isEmpty()
                ? new PerformanceStatus(0L, 0L, null)
                : tierResolver.judge(monthTiers, performanceAmount);
        PerformanceStatus quarterStatus = quarterTiers.isEmpty()
                ? null
                : tierResolver.judge(quarterTiers, performanceAmount * 3);
        return new PerformanceStatus[]{monthStatus, quarterStatus};
    }

    /** 판정·조회가 끝난 값으로 시뮬레이션 입력을 만든다. 여기서는 DB를 부르지 않는다 */
    private SimulatedCard toSimulatedCard(long cardId,
                                          List<SimulatedPayment> payments,
                                          PerformanceStatus[] status,
                                          List<BenefitCandidate> candidates,
                                          List<OptionKeyRow> optionKeys) {
        return new SimulatedCard(cardId, candidates, status[0], status[1],
                chooseBestOptions(candidates, status[0], status[1], optionKeys, payments));
    }

    /**
     * 선택형 혜택 묶음마다 이 회원의 소비에 가장 유리한 선택지를 고른다.
     *
     * 회원이 고른 기록을 쓰지 않는 이유는 <b>아직 발급하지 않은 카드</b>이기 때문이다.
     * 고른 것이 없다고 두면 그 묶음의 혜택이 전부 꺼져 선택형 카드만 과소평가된다.
     * 카드를 발급한다면 자기 소비에 맞는 선택지를 고를 것이므로 그렇게 가정한다.
     *
     * 묶음을 하나씩 고정해 나간다. 묶음이 여럿이면 조합이 곱으로 늘어나는데, 실제 카드는
     * 묶음이 하나인 경우가 대부분이라 조합까지 따질 이익이 적다.
     */
    private Map<String, String> chooseBestOptions(List<BenefitCandidate> candidates,
                                                  PerformanceStatus monthStatus,
                                                  PerformanceStatus quarterStatus,
                                                  List<OptionKeyRow> optionKeys,
                                                  List<SimulatedPayment> payments) {
        Map<String, List<String>> keysByGroup = new LinkedHashMap<>();
        for (OptionKeyRow row : optionKeys) {
            keysByGroup.computeIfAbsent(row.getOptionGroupCode(), group -> new ArrayList<>())
                    .add(row.getOptionKey());
        }
        if (keysByGroup.isEmpty()) {
            return Map.of();
        }

        Map<String, String> chosen = new LinkedHashMap<>();
        keysByGroup.forEach((group, keys) -> {
            String bestKey = null;
            long bestAmount = -1L;
            for (String key : keys) {
                Map<String, String> trial = new LinkedHashMap<>(chosen);
                trial.put(group, key);
                long amount = spendingSimulator
                        .simulate(candidates, monthStatus, quarterStatus, trial, payments)
                        .totalBenefitAmount();
                // 동점이면 먼저 나온 선택지 — 정렬이 고정이라 같은 입력에 같은 답이 나온다
                if (amount > bestAmount) {
                    bestAmount = amount;
                    bestKey = key;
                }
            }
            if (bestKey != null) {
                chosen.put(group, bestKey);
            }
        });
        return chosen;
    }

    private List<PerformanceTier> tiersOf(List<PerformanceTier> tiers, PerformancePeriod period) {
        return tiers.stream().filter(tier -> tier.periodType() == period).toList();
    }

    private SimulatedPayment toPayment(SpentTransactionRow row) {
        PaymentTarget target = PaymentTarget.builder()
                .merchantId(row.getMerchantId())
                .merchantCode(row.getMerchantCode())
                .categoryId(row.getCategoryId())
                .categoryCode(row.getCategoryCode())
                .parentCategoryId(row.getParentCategoryId())
                .parentCategoryCode(row.getParentCategoryCode())
                .build();
        return new SimulatedPayment(row.getPaymentDate().toLocalDate(),
                PaymentRequest.estimated(target, row.getAmount(), row.getPaymentType()));
    }

    /**
     * 실적 계산용 거래로 바꾼다.
     *
     * 할인액을 0으로 두는 것은 이 시뮬레이션이 "이 카드로 결제했다면"을 보기 때문이다. 실제
     * 결제에서 다른 카드로 받은 할인은 이 카드의 실적 제외 판정과 무관하다.
     */
    private PerformanceTransaction toPerformanceTransaction(SpentTransactionRow row) {
        return PerformanceTransaction.builder()
                .amount(row.getAmount())
                .categoryCode(row.getCategoryCode())
                .parentCategoryCode(row.getParentCategoryCode())
                .paymentType(row.getPaymentType())
                .build();
    }
}
