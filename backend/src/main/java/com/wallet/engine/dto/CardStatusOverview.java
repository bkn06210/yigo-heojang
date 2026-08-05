package com.wallet.engine.dto;

import java.util.List;

/**
 * 전체 보유 카드 현황(#2) 응답 — 대시보드 홈·보유카드 목록 화면용.
 *
 * 카드가 여러 장이어도 한 번의 호출로 전부 담는다(카드마다 상세를 반복 호출하지 않는다).
 * 보유 카드가 0장인 것은 에러가 아니라 정상 상태다 — {@code cards=[]}, {@code briefing=null}.
 *
 * @param briefing 실적 달성이 가장 임박한 카드 안내. 후보가 없으면 null
 * @param cards    보유 카드별 요약
 */
public record CardStatusOverview(
        CardStatusBriefing briefing,
        List<CardStatusSummary> cards
) {

    public static CardStatusOverview empty() {
        return new CardStatusOverview(null, List.of());
    }
}
