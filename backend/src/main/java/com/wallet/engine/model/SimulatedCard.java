package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 시뮬레이션에 참여하는 카드 한 장 — 혜택 목록과 실적 판정 결과.
 *
 * cardId는 시뮬레이션 안에서 카드를 구분하는 값이다. 보유 카드는 user_card_id, 미보유 후보는
 * card_id를 넣는다 — 둘을 한 번에 돌릴 때 겹치지 않도록 호출자가 정한다.
 *
 * @param candidates         이 카드의 활성 혜택 <b>전부</b>. 매칭될 것만 넘기면 묶음 한도 합산이 틀린다
 * @param monthStatus        전월 실적으로 판정한 구간
 * @param quarterStatus      전분기 축 판정 결과. 분기 구간표가 없는 카드면 null
 * @param selectedOptionKeys 선택형 혜택 묶음별로 고른 선택지. 안 고른 묶음의 혜택은 적용되지 않는다
 */
public record SimulatedCard(
        long cardId,
        List<BenefitCandidate> candidates,
        PerformanceStatus monthStatus,
        PerformanceStatus quarterStatus,
        Map<String, String> selectedOptionKeys
) {

    public SimulatedCard {
        if (monthStatus == null) {
            throw new IllegalArgumentException("전월 실적 판정 결과는 필수다");
        }
        candidates = candidates == null ? List.of() : Collections.unmodifiableList(List.copyOf(candidates));
        selectedOptionKeys = selectedOptionKeys == null ? Map.of() : Map.copyOf(selectedOptionKeys);
    }

    /** 분기 구간표도 선택형 혜택도 없는 카드 */
    public SimulatedCard(long cardId, List<BenefitCandidate> candidates, PerformanceStatus monthStatus) {
        this(cardId, candidates, monthStatus, null, Map.of());
    }
}
