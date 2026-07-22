package com.wallet.engine.model;

/**
 * 혜택 적용 예외의 종류 (benefit_exclusion.exclusion_type).
 * 예) "외식 5% (단, 배달앱 제외)" → 대상은 대분류 외식, 여기에 CATEGORY 'DELIVERY' 한 행.
 *
 * TRANSACTION_ATTR는 거래 속성(무이자할부 등) 제외인데, 추천 시점에는
 * 아직 확정되지 않은 정보가 많아 현재 엔진은 CATEGORY/MERCHANT/PAYMENT_TYPE만 해석한다.
 */
public enum ExclusionType {
    CATEGORY,
    MERCHANT,
    PAYMENT_TYPE,
    TRANSACTION_ATTR
}
