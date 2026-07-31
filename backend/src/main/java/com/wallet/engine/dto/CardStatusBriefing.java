package com.wallet.engine.dto;

import java.math.BigDecimal;

/**
 * 홈 상단 브리핑 — "이번 달 어느 카드부터 채우면 되는가" 한 줄 안내.
 *
 * 판단도 문구도 엔진이 만든다. LLM은 쓰지 않는다(CLAUDE.md: 계산·판단은 규칙 엔진, LLM은 표현만).
 * 화면 표기에서 "AI 브리핑" 같은 라벨을 붙이지 않는 이유이기도 하다 — 규칙으로 고른 결과다.
 *
 * @param achievementRate      해당 카드 달성률(%)
 * @param remainingPerformance 다음 구간까지 남은 실적 금액
 * @param message              안내 문구 (엔진 템플릿 생성)
 */
public record CardStatusBriefing(
        long userCardId,
        String cardName,
        BigDecimal achievementRate,
        long remainingPerformance,
        String message
) {
}
