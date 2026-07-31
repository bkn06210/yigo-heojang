package com.wallet.engine.service;

import com.wallet.engine.dto.BenefitSummary;
import com.wallet.engine.dto.BenefitUsageStatus;
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

    private static final String BRIEFING_MESSAGE_FORMAT =
            "보유하신 카드 %d장 중 %s 카드 실적이 %s%%로 가장 임박했어요. 이번 달은 이 카드부터 채우는 걸 추천드려요.";

    /**
     * 카드별 상세 현황을 홈 응답으로 접는다.
     *
     * @param statuses 보유 카드별 상세 현황 (조회 순서 = 응답 순서)
     */
    public CardStatusOverview build(List<CardMonthlyStatus> statuses) {
        List<CardStatusSummary> cards = statuses.stream().map(this::toSummary).toList();
        return new CardStatusOverview(buildBriefing(cards), cards);
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
                toBenefitsSummary(status.benefits()));
    }

    /**
     * "남은 혜택" 요약 — 아직 쓸 수 있는 혜택만, 묶음 한도는 그룹당 한 줄로.
     *
     * 한도를 다 쓴 혜택(잔여 0)은 뺀다. 한도가 없는 혜택(잔여 null)은 <b>남긴다</b> —
     * null은 "제약 없음"이라 언제든 받을 수 있다는 뜻이지 소진이 아니다(NULL≠0).
     *
     * 묶음을 접지 않으면 화면이 같은 지갑을 여러 번 더한다. 대신 접히면서 사라지는 혜택명은
     * "외 N건"으로 표시에 남긴다 — 정보를 지우지 않으면서 금액은 한 번만 노출하기 위함이다.
     */
    private List<BenefitSummary> toBenefitsSummary(List<BenefitUsageStatus> benefits) {
        List<BenefitSummary> result = new ArrayList<>();
        for (List<BenefitUsageStatus> group : groupByLimit(benefits).values()) {
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
    private CardStatusBriefing buildBriefing(List<CardStatusSummary> cards) {
        return cards.stream()
                .filter(card -> card.achievementRate() != null)
                .filter(card -> card.remainingPerformance() > 0)
                .min(byImminence())
                .map(card -> toBriefing(card, cards.size()))
                .orElse(null);
    }

    private Comparator<CardStatusSummary> byImminence() {
        return Comparator.comparing(CardStatusSummary::achievementRate).reversed()
                .thenComparingLong(CardStatusSummary::remainingPerformance)
                .thenComparingLong(CardStatusSummary::userCardId);
    }

    private CardStatusBriefing toBriefing(CardStatusSummary card, int totalCardCount) {
        String message = String.format(BRIEFING_MESSAGE_FORMAT,
                totalCardCount, card.cardName(), formatRate(card.achievementRate()));
        return new CardStatusBriefing(
                card.userCardId(), card.cardName(), card.achievementRate(),
                card.remainingPerformance(), message);
    }

    /** 문구용 달성률 — 90.0은 "90", 85.5는 "85.5". 의미 없는 소수점 0을 문장에 남기지 않는다. */
    private String formatRate(BigDecimal achievementRate) {
        BigDecimal stripped = achievementRate.stripTrailingZeros();
        // stripTrailingZeros는 0을 지수 표기(0E-1)로 만들 수 있어 scale을 되돌린다
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}
