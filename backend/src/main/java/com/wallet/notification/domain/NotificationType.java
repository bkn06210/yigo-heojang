package com.wallet.notification.domain;

/**
 * notification.notification_type 컬럼에 저장되는 값.
 * DB 컬럼 타입은 VARCHAR라 자유 문자열도 들어갈 수 있지만,
 * 값의 종류를 코드로 만들어 오타·누락을 막기 위해 enum으로 감싼다.
 * <p>
 * PERFORMANCE_SHORTAGE / BENEFIT_LIMIT : 후보 1건당 만들어지는 개별 알림
 * *_DIGEST : 후보가 3건을 초과할 때의 다이제스트 알림
 * <p>
 * 실적 부족과 혜택 한도는 알림 규칙상 서로 섞이지 않으므로
 * 개별/다이제스트 여부와 무관하게 이 두 축으로만 구분한다.
 */
public enum NotificationType {
    PERFORMANCE_SHORTAGE,
    PERFORMANCE_SHORTAGE_DIGEST,
    BENEFIT_LIMIT,
    BENEFIT_LIMIT_DIGEST
}
