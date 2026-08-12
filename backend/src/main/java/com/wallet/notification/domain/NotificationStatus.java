package com.wallet.notification.domain;

/**
 * notification.notification_status 컬럼과 1:1 대응하는 발송 상태.
 * <p>
 * MyBatis는 파라미터/결과값이 Java enum이면 기본적으로 EnumTypeHandler를 사용한다.
 * 이 핸들러는 DB에 쓸 때는 enum.name()을, 읽을 때는 enum.valueOf(문자열)을 호출한다.
 * 즉 별도 설정 없이 "PENDING" <-> NotificationStatus.PENDING 이 자동으로 오간다.
 * (단, enum 이름과 DB에 저장된 문자열 값이 정확히 같아야 한다.)
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}