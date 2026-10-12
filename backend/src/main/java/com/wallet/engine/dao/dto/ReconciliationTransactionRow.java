package com.wallet.engine.dao.dto;

/**
 * 실적 재계산용 소비내역 한 행 — 대사용.
 *
 * 실적 판정에 필요한 컬럼은 전월실적 재합산과 같으므로 그 투영을 그대로 물려받고,
 * 여러 카드·여러 달을 한 번에 읽기 위한 묶음 키만 더한다. 물려받은 덕에 거래 변환을
 * PerformanceInputAssembler 하나가 맡아, 두 경로가 원값을 서로 다르게 해석할 여지가 없다.
 */
public class ReconciliationTransactionRow extends PerformanceTransactionRow {

    private long userCardId;
    /** 카드 마스터 ID. 실적 제외 규칙이 카드 상품 단위라 묶음 키와 별도로 필요하다 */
    private long cardId;
    /** 결제일시에서 뽑은 연월 (YYYY-MM) */
    private String baseYearMonth;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public String getBaseYearMonth() {
        return baseYearMonth;
    }

    public void setBaseYearMonth(String baseYearMonth) {
        this.baseYearMonth = baseYearMonth;
    }
}
