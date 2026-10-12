package com.wallet.engine.model;

/**
 * 불일치의 모양 — 저장값과 원본 재계산값이 어떻게 어긋났는지.
 *
 * 값이 다르다는 사실만으로는 원인을 좁히기 어렵다. 한쪽이 아예 없는 경우는 원인이 다르므로
 * (기록 삭제·다른 달로 귀속·정산을 거치지 않은 소비내역) 따로 가른다.
 */
public enum DiscrepancyKind {

    /** 양쪽 다 있는데 값이 다르다 */
    VALUE_MISMATCH,

    /** 상태에는 값이 있는데 그것을 뒷받침하는 원본 기록이 없다 */
    STATE_WITHOUT_SOURCE,

    /** 원본에는 기록이 있는데 상태 행이 없다 */
    SOURCE_WITHOUT_STATE
}
