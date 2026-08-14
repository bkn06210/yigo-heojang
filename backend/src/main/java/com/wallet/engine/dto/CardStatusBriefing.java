package com.wallet.engine.dto;

import java.math.BigDecimal;

/**
 * 홈 상단 브리핑 — "지금 무엇을 하면 되는가" 한 줄 안내.
 *
 * 판단도 문구도 엔진이 만든다. 요청마다 LLM을 부르지 않는다 — 홈은 앱을 열자마자 보여야 하는
 * 화면이라 1~3초를 기다릴 수 없다. 문장은 미리 써둔 것 중에서 고르고, 숫자는 서버가 끼운다.
 * 화면 표기에서 "AI 브리핑" 같은 라벨을 붙이지 않는 이유이기도 하다 — 규칙으로 고른 결과다.
 *
 * <b>카드에 매인 값은 상황에 따라 비어 있다.</b> 보유 카드가 없거나 소비 관찰을 말하는 상황에는
 * 가리킬 카드가 없다. 화면은 {@code type}으로 분기하고 문장은 {@code message}를 그대로 쓴다 —
 * 문장을 뜯어 분기하면 문구가 바뀔 때마다 화면이 깨진다.
 *
 * @param type                 이 브리핑이 말하는 상황
 * @param userCardId           가리키는 카드. 카드와 무관한 상황이면 null
 * @param cardName             가리키는 카드 이름. 카드와 무관한 상황이면 null
 * @param achievementRate      해당 카드 달성률(%). 실적 안내가 아니면 null
 * @param remainingPerformance 다음 구간까지 남은 실적 금액. 실적 안내가 아니면 null
 * @param message              안내 문구 (자리표시자를 서버가 채운 완성 문장)
 */
public record CardStatusBriefing(
        BriefingType type,
        Long userCardId,
        String cardName,
        BigDecimal achievementRate,
        Long remainingPerformance,
        String message
) {
}
