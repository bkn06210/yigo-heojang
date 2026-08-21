package com.wallet.engine.dao.dto;

/**
 * performance_exclusion 한 행의 조회 투영.
 *
 * exclusion_type을 PerformanceExclusionType으로 바꾸는 해석은 assembler가 한다 —
 * 모르는 타입은 스키마 위반(버그)이라 assembler에서 fail-fast로 예외를 던져야 하기 때문이다.
 */
public class PerformanceExclusionRow {

    // 카드 여러 장을 한 번에 조회할 때 어느 카드의 것인지 가른다
    private long cardId;
    private String exclusionType;
    private String exclusionValue;

    public String getExclusionType() {
        return exclusionType;
    }

    public void setExclusionType(String exclusionType) {
        this.exclusionType = exclusionType;
    }

    public String getExclusionValue() {
        return exclusionValue;
    }

    public void setExclusionValue(String exclusionValue) {
        this.exclusionValue = exclusionValue;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }
}
