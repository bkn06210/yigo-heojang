package com.wallet.engine.model;

import java.util.List;

/**
 * 대사 결과 — 무엇을 얼마나 맞춰 봤고, 그중 무엇이 어긋났는지.
 *
 * 대조 한 건은 "저장값 하나 ↔ 원본 재계산값 하나"다. 상태 행 하나가 여러 건을 만든다
 * (카드 월 상태는 당월실적·통합한도·전월실적, 혜택 소진은 금액·횟수).
 *
 * @param stateRowCount   대조한 카드 월 상태 행 수
 * @param usageRowCount   대조한 혜택 소진 행 수
 * @param expenseCount    원본으로 읽은 소비내역 건수(취소 건 포함)
 * @param comparisonCount 맞춰 본 건수. 일치 + 불일치이며 대조 불가는 포함하지 않는다
 * @param matchedCount    일치 건수
 * @param discrepancies   불일치 목록. 보유카드 → 연월 → 혜택 순
 * @param unverifiables   원본으로 복원할 수 없어 맞춰 보지 못한 목록
 */
public record ReconciliationReport(int stateRowCount,
                                   int usageRowCount,
                                   int expenseCount,
                                   int comparisonCount,
                                   int matchedCount,
                                   List<ReconciliationDiscrepancy> discrepancies,
                                   List<ReconciliationUnverifiable> unverifiables) {

    public ReconciliationReport {
        discrepancies = List.copyOf(discrepancies);
        unverifiables = List.copyOf(unverifiables);
    }

    public int mismatchedCount() {
        return discrepancies.size();
    }

    public int unverifiableCount() {
        return unverifiables.size();
    }

    /** 불일치가 하나도 없는가. 대조 불가는 불일치가 아니므로 있어도 true다 */
    public boolean consistent() {
        return discrepancies.isEmpty();
    }
}
