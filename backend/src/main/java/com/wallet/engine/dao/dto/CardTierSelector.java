package com.wallet.engine.dao.dto;

/**
 * 카드 하나와 그 카드에 판정된 실적 구간 — 혜택을 여러 장 한 번에 조회할 때 쓴다.
 *
 * 구간이 카드마다 다르므로 카드 목록만으로는 조회 조건이 서지 않는다. 구간별 개별한도
 * (benefit_tier_limit)를 붙이려면 카드마다 어느 구간으로 판정됐는지 함께 넘겨야 한다.
 *
 * @param quarterTierId 분기 구간표가 없는 카드면 null. 그 카드의 분기 혜택은 구간값이 안 붙는다
 */
public record CardTierSelector(long cardId, long monthTierId, Long quarterTierId) {
}
