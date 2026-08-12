package com.wallet.engine.service;

import com.wallet.engine.dao.dto.CategorySpendingRow;
import com.wallet.engine.dto.BenefitSummary;
import com.wallet.engine.dto.BenefitUsageStatus;
import com.wallet.engine.dto.BriefingType;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusBriefing;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.dto.CardStatusSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 카드별 상세 현황 목록 → 전체 현황 응답(#2) 조립기.
 *
 * <b>상세(#3)를 먼저 만들고 목록으로 접는다.</b> 목록용 실적 계산을 따로 짜면
 * 홈에서 본 달성률과 카드 상세에서 본 달성률이 어긋날 수 있는데, 같은 객체에서 파생시키면
 * 구조적으로 그럴 수 없다. 여기서 새로 하는 계산은 두 가지뿐이다:
 *   · 혜택 요약  — 아직 쓸 수 있는 것만 남기고, 묶음 한도는 그룹당 한 줄로 접기
 *   · 브리핑     — 실적이 가장 임박한 카드 하나 고르기 + 문구 생성
 *
 * DB·시계를 모르는 순수 조립이라 단위 테스트로 규칙을 고정할 수 있다.
 */
@Component
public class CardStatusOverviewBuilder {

    /**
     * 카드별 상세 현황을 홈 응답으로 접는다.
     *
     * @param statuses 보유 카드별 상세 현황 (조회 순서 = 응답 순서)
     * @param context  브리핑 판정에 필요한 카드 현황 밖의 값들
     */
    public CardStatusOverview build(List<CardMonthlyStatus> statuses, BriefingContext context) {
        List<CardStatusSummary> cards = statuses.stream().map(this::toSummary).toList();
        return new CardStatusOverview(buildBriefing(statuses, cards, context), cards);
    }

    private CardStatusSummary toSummary(CardMonthlyStatus status) {
        return new CardStatusSummary(
                status.userCardId(),
                status.cardName(),
                status.yearMonth(),
                status.currentPerformanceAmount(),
                status.targetPerformance(),
                status.remainingPerformance(),
                status.achievementRate(),
                status.performanceMet(),
                status.sharedLimit(),
                status.sharedLimitUsed(),
                toBenefitsSummary(status.benefits(), status.sharedLimit(), status.sharedLimitUsed()));
    }

    /**
     * "남은 혜택" 요약 — <b>지금 쓸 수 있는 혜택만</b>, 묶음 한도는 그룹당 한 줄로.
     *
     * 카드 상세(#3)와 목적이 달라 기준도 다르다. 상세는 "이 카드에 어떤 혜택이 있나"라서 전부
     * 보여주지만, 홈 위젯은 "지금 뭘 받을 수 있나"를 보는 자리다. 그래서 여기서만 두 가지를 뺀다.
     *   · 한도를 다 쓴 혜택(개별 잔여 0, 또는 통합한도를 쓰는데 카드의 통합 잔여가 0)
     *   · 실적 미충족 혜택 — 이번 달엔 계산기가 아예 적용하지 않는다.
     *     남겨두면 "남은 혜택 5,000원"과 "실적 미달"이 한 화면에 같이 뜨는 모순이 된다.
     *     그 카드는 브리핑이 "이 카드부터 채우라"고 안내하므로 정보가 사라지지는 않는다.
     *
     * 한도가 없는 혜택(잔여 null)은 <b>남긴다</b> — null은 "제약 없음"이라 언제든 받을 수 있다는
     * 뜻이지 소진이 아니다(NULL≠0).
     *
     * 묶음을 접지 않으면 화면이 같은 지갑을 여러 번 더한다. 대신 접히면서 사라지는 혜택명은
     * "외 N건"으로 표시에 남긴다 — 정보를 지우지 않으면서 금액은 한 번만 노출하기 위함이다.
     *
     * <b>잔여액 자체에 통합할인한도를 섞지는 않는다.</b> 통합 잔여는 카드 단위 값이라
     * (sharedLimit − sharedLimitUsed) 응답에 이미 따로 있고, 성격이 다른 두 잔액이라 합치지 않는다.
     * 화면은 통합 잔여를 카드 단위로 한 줄 두고 그 안에서 개별 잔여를 보여준다.
     * 다만 통합 잔여가 0이면 그 한도를 쓰는 혜택은 <b>목록에서 뺀다</b> — 개별 잔액이 남아 있어도
     * 실제로는 못 받으므로, 위의 "한도를 다 쓴 혜택"과 같은 경우다. 금액을 합치는 것과 다르다.
     *
     * @param card 이 카드의 요약. 실적 충족은 혜택마다 축이 달라 혜택 쪽 값을 쓰고,
     *             통합 잔여만 카드 단위로 본다
     */
    private List<BenefitSummary> toBenefitsSummary(List<BenefitUsageStatus> benefits,
                                                   Long sharedLimit, long sharedLimitUsed) {
        boolean sharedLimitExhausted = sharedLimitExhausted(sharedLimit, sharedLimitUsed);
        List<BenefitUsageStatus> usableBenefits = benefits.stream()
                .filter(benefit -> !benefit.requirePerformance() || benefit.performanceMet())
                .filter(benefit -> !benefit.useSharedLimit() || !sharedLimitExhausted)
                .toList();

        List<BenefitSummary> result = new ArrayList<>();
        for (List<BenefitUsageStatus> group : groupByLimit(usableBenefits).values()) {
            // 대표는 그룹에서 가장 작은 benefit_id — 같은 입력에 같은 응답이 나오게 하는 규칙
            BenefitUsageStatus representative = group.stream()
                    .min(Comparator.comparingLong(BenefitUsageStatus::benefitId))
                    .orElseThrow();
            Long remainingLimit = representative.remainingLimit();
            if (remainingLimit != null && remainingLimit == 0L) {
                continue;
            }
            result.add(new BenefitSummary(
                    representative.benefitId(),
                    displayName(representative.benefitName(), group.size()),
                    representative.limitGroupCode(),
                    remainingLimit));
        }
        return result;
    }

    /**
     * 한도를 공유하는 단위로 묶는다. 묶음 코드가 없는 혜택은 각자가 한 그룹이다 —
     * null끼리 한 덩어리가 되지 않도록 혜택 id로 유일한 키를 만든다.
     * 순서는 입력 순서를 유지한다(그룹은 첫 구성원이 나온 자리에 놓인다).
     */
    private Map<String, List<BenefitUsageStatus>> groupByLimit(List<BenefitUsageStatus> benefits) {
        Map<String, List<BenefitUsageStatus>> groups = new LinkedHashMap<>();
        for (BenefitUsageStatus benefit : benefits) {
            String key = benefit.limitGroupCode() != null
                    ? "GROUP:" + benefit.limitGroupCode()
                    : "BENEFIT:" + benefit.benefitId();
            groups.computeIfAbsent(key, unused -> new ArrayList<>()).add(benefit);
        }
        return groups;
    }

    private String displayName(String representativeName, int groupSize) {
        return groupSize > 1 ? representativeName + " 외 " + (groupSize - 1) + "건" : representativeName;
    }

    /**
     * 지금 무엇을 말할지 고른다.
     *
     * 여러 상황이 동시에 성립할 수 있어 순서를 못박는다. 위에 있을수록 <b>사용자가 지금
     * 할 수 있는 일이 구체적</b>이다 — "카페는 이 카드로"가 "실적을 채우세요"보다 실행하기 쉽다.
     *
     * <pre>
     * ① 카드 없음      다른 안내가 성립하지 않는다
     * ② 안 쓰는 혜택    결제하는 업종인데 그 혜택을 못 받고 있다
     * ③ 실적 임박      이 카드부터 채우면 된다
     * ④ 전부 달성      더 채울 것이 없다
     * ⑤ 소비 관찰      권할 혜택은 없지만 소비가 몰린 업종이 있다
     * ⑥ 그 외          이번 달 결제가 아직 없다
     * </pre>
     */
    private CardStatusBriefing buildBriefing(List<CardMonthlyStatus> statuses,
                                             List<CardStatusSummary> cards,
                                             BriefingContext context) {
        if (cards.isEmpty()) {
            return message(BriefingType.NO_CARD, context, Map.of());
        }

        CardStatusBriefing unusedBenefit = unusedBenefit(statuses, context);
        if (unusedBenefit != null) {
            return unusedBenefit;
        }

        CardStatusBriefing performanceNear = performanceNear(cards, context);
        if (performanceNear != null) {
            return performanceNear;
        }

        if (hasAchievedEveryTarget(cards)) {
            return message(BriefingType.ALL_ACHIEVED, context, Map.of());
        }

        CardStatusBriefing insight = spendingInsight(context);
        return insight != null ? insight : message(BriefingType.GETTING_STARTED, context, Map.of());
    }

    /**
     * 결제하는 업종인데 그 혜택을 못 받고 있는 자리를 찾는다.
     *
     * <b>"놓친 혜택"과 다르다.</b> 지난 거래를 되짚어 "얼마 놓쳤다"를 합산하지 않는다.
     * 거래마다 따로 계산해 더하면 월 한도에 막히는 몫이 빠져 실제보다 큰 금액이 나온다
     * (카페 20만원의 10%는 2만원이 아니라 한도에 막혀 5천원이다).
     * 여기서는 현재 상태만 보고 <b>앞으로 무엇을 하면 되는지</b>만 말한다.
     *
     * 걸러내는 것:
     *   · 이번 달 계산에 안 잡히는 혜택 — 실적 조건을 못 채운 혜택.
     *     권해봐야 적용되지 않아 "이 카드로 결제하세요"가 거짓말이 된다.
     *     <b>카드가 아니라 혜택의 실적 축으로 본다</b> — 한 카드가 전월 축과 전분기 축을 함께 쓸 수 있어,
     *     전월 실적만 보면 분기 실적이 모자란 혜택을 권하게 된다
     *   · 이미 받고 있는 혜택(소진액 &gt; 0) — 안 쓰는 것이 아니다
     *   · 한도를 다 쓴 혜택 — 더 결제해도 안 나온다. 개별 잔여 0뿐 아니라 <b>통합할인한도를 쓰는데
     *     카드의 통합 잔여가 0</b>인 경우도 여기다(개별 한도가 아예 없는 혜택이 여기 걸린다).
     *     잔여 null은 제약이 없다는 뜻이라 남긴다
     *   · 업종을 겨냥하지 않는 혜택 — 소비 업종과 맞춰볼 기준이 없다
     *   · 제외된 업종의 소비 — 대상 업종에 속해도 그 혜택이 배제하는 업종이면 근거가 못 된다
     *
     * 후보가 여럿이면 <b>소비가 큰 업종</b>을 고른다. 그래야 안내가 실제 생활에 가깝다.
     * 금액까지 같으면 benefitId 오름차순 — 같은 입력에 같은 답이 나오게 하는 재현성 규칙이다.
     */
    private CardStatusBriefing unusedBenefit(List<CardMonthlyStatus> statuses,
                                             BriefingContext context) {
        UnusedBenefit best = null;

        for (CardMonthlyStatus status : statuses) {
            boolean sharedLimitExhausted =
                    sharedLimitExhausted(status.sharedLimit(), status.sharedLimitUsed());
            for (BenefitUsageStatus benefit : status.benefits()) {
                if (benefit.requirePerformance() && !benefit.performanceMet()) {
                    continue;
                }
                if (benefit.usedAmount() > 0) {
                    continue;
                }
                Long remainingLimit = benefit.remainingLimit();
                if (remainingLimit != null && remainingLimit == 0L) {
                    continue;
                }
                if (benefit.useSharedLimit() && sharedLimitExhausted) {
                    continue;
                }
                BriefingBenefitTarget target = context.benefitTargets().get(benefit.benefitId());
                if (target == null) {
                    continue;
                }
                CategorySpendingRow spending = findSpending(context.spending(), target);
                if (spending == null) {
                    continue;
                }

                UnusedBenefit candidate = new UnusedBenefit(status, benefit, spending);
                if (best == null || candidate.isBetterThan(best)) {
                    best = candidate;
                }
            }
        }

        if (best == null) {
            return null;
        }
        return new CardStatusBriefing(
                BriefingType.UNUSED_BENEFIT,
                best.status.userCardId(),
                best.status.cardName(),
                null,
                null,
                fill(BriefingMessages.pick(BriefingType.UNUSED_BENEFIT, context.yearMonth()), Map.of(
                        "category", best.spending.getCategoryName(),
                        "count", String.valueOf(best.spending.getPaymentCount()),
                        "card", best.status.cardName(),
                        "benefit", best.benefit.benefitName())));
    }

    /**
     * 이 혜택이 겨냥한 업종에 결제가 있었는지 찾는다.
     *
     * 상위 분류도 함께 본다. 혜택이 대분류(외식)를 겨냥하면 하위 중분류(카페) 결제도 대상이라,
     * 중분류만 대조하면 대분류 혜택이 통째로 안 잡힌다. 혜택 매칭 규칙과 같은 방향이다.
     *
     * <b>제외 업종은 건너뛴다.</b> 대분류를 겨냥하면서 그 아래 한 중분류를 빼는 혜택이 있어
     * ("교통 10%, 단 고속시외버스 제외"), 대상만 대조하면 제외된 소비를 근거로 카드를 권하게 된다.
     * 제외에 걸린 줄은 버리고 <b>다음 후보를 계속 본다</b> — 같은 혜택이 다른 업종 소비로는
     * 근거가 될 수 있기 때문이다. 소비가 금액 내림차순이라 남은 것 중 첫 줄이 여전히 가장 큰 업종이다.
     */
    private CategorySpendingRow findSpending(List<CategorySpendingRow> spending,
                                             BriefingBenefitTarget target) {
        return spending.stream()
                .filter(row -> target.targetCategoryId() == row.getCategoryId()
                        || (row.getParentCategoryId() != null
                            && target.targetCategoryId() == row.getParentCategoryId()))
                .filter(row -> target.covers(row.getCategoryCode(), row.getParentCategoryCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 카드의 통합할인한도가 바닥났는가.
     *
     * null은 "통합한도가 없는 카드"라 막을 것이 없다(NULL≠0). 0은 "그 구간엔 혜택이 없다"는 뜻이라
     * 처음부터 바닥난 것과 같다.
     */
    private boolean sharedLimitExhausted(Long sharedLimit, long sharedLimitUsed) {
        return sharedLimit != null && sharedLimit - sharedLimitUsed <= 0;
    }

    /** 안 쓰고 있는 혜택 후보 하나. 고르는 기준이 두 값에 걸쳐 있어 묶어 둔다. */
    private record UnusedBenefit(CardMonthlyStatus status,
                                 BenefitUsageStatus benefit,
                                 CategorySpendingRow spending) {

        boolean isBetterThan(UnusedBenefit other) {
            if (spending.getTotalAmount() != other.spending.getTotalAmount()) {
                return spending.getTotalAmount() > other.spending.getTotalAmount();
            }
            return benefit.benefitId() < other.benefit.benefitId();
        }
    }

    /**
     * 실적 달성이 가장 임박한 카드를 고른다.
     *
     * 후보에서 빠지는 카드가 둘이다.
     *   · 실적 조건이 없는 카드(목표 0, 달성률 null) — 채울 실적 자체가 없다
     *   · 이미 최고 구간까지 채운 카드(남은 실적 0) — 더 채워도 달라지는 게 없다
     *
     * 남은 카드 중 달성률이 가장 높은 하나. 동률이면 남은 금액이 적은 쪽, 그래도 같으면
     * userCardId 오름차순으로 정한다 — 같은 입력에 같은 카드가 나오게 하는 재현성 규칙이다.
     *
     * @param cards 보유 카드 요약 전체. 문구의 "카드 N장"은 후보 수가 아니라 보유 수다
     */
    private CardStatusBriefing performanceNear(List<CardStatusSummary> cards, BriefingContext context) {
        return cards.stream()
                .filter(card -> card.achievementRate() != null)
                .filter(card -> card.remainingPerformance() > 0)
                .min(byImminence())
                .map(card -> toPerformanceBriefing(card, cards.size(), context))
                .orElse(null);
    }

    private Comparator<CardStatusSummary> byImminence() {
        return Comparator.comparing(CardStatusSummary::achievementRate).reversed()
                .thenComparingLong(CardStatusSummary::remainingPerformance)
                .thenComparingLong(CardStatusSummary::userCardId);
    }

    private CardStatusBriefing toPerformanceBriefing(CardStatusSummary card, int totalCardCount,
                                                     BriefingContext context) {
        return new CardStatusBriefing(
                BriefingType.PERFORMANCE_NEAR,
                card.userCardId(),
                card.cardName(),
                card.achievementRate(),
                card.remainingPerformance(),
                fill(BriefingMessages.pick(BriefingType.PERFORMANCE_NEAR, context.yearMonth()), Map.of(
                        "cardCount", String.valueOf(totalCardCount),
                        "card", card.cardName(),
                        "rate", formatRate(card.achievementRate()))));
    }

    /**
     * 실적 조건이 있는 카드를 전부 채웠는가.
     *
     * 조건이 있는 카드가 하나도 없으면 <b>달성한 것이 아니다</b>. 채울 실적이 없었을 뿐인데
     * "모두 채우셨습니다"라고 하면 하지 않은 일을 했다고 말하는 것이 된다.
     */
    private boolean hasAchievedEveryTarget(List<CardStatusSummary> cards) {
        List<CardStatusSummary> withTarget = cards.stream()
                .filter(card -> card.achievementRate() != null)
                .toList();
        return !withTarget.isEmpty()
                && withTarget.stream().allMatch(card -> card.remainingPerformance() == 0);
    }

    /** 권할 혜택은 없지만 소비가 몰린 업종이 있을 때. 관찰만 전한다. */
    private CardStatusBriefing spendingInsight(BriefingContext context) {
        if (context.spending().isEmpty()) {
            return null;
        }
        // 조회가 금액 내림차순이라 첫 줄이 가장 많이 쓴 업종이다.
        CategorySpendingRow top = context.spending().get(0);
        return message(BriefingType.SPENDING_INSIGHT, context, Map.of(
                "category", top.getCategoryName(),
                "count", String.valueOf(top.getPaymentCount())));
    }

    private CardStatusBriefing message(BriefingType type, BriefingContext context,
                                       Map<String, String> values) {
        return new CardStatusBriefing(type, null, null, null, null,
                fill(BriefingMessages.pick(type, context.yearMonth()), values));
    }

    /** 문구의 자리표시자를 서버가 채운다. 숫자를 문장에 끼우는 일까지 LLM에 맡기지 않는다. */
    private String fill(String template, Map<String, String> values) {
        String filled = template;
        for (Map.Entry<String, String> value : values.entrySet()) {
            filled = filled.replace("{" + value.getKey() + "}", value.getValue());
        }
        return filled;
    }

    /** 문구용 달성률 — 90.0은 "90", 85.5는 "85.5". 의미 없는 소수점 0을 문장에 남기지 않는다. */
    private String formatRate(BigDecimal achievementRate) {
        BigDecimal stripped = achievementRate.stripTrailingZeros();
        // stripTrailingZeros는 0을 지수 표기(0E-1)로 만들 수 있어 scale을 되돌린다
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}
