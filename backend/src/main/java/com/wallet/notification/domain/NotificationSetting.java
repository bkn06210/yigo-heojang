package com.wallet.notification.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * 회원별 알림 수신 설정. notification_setting 테이블 한 행에 대응한다.
 * <p>
 * 회원당 한 행만 존재한다(UNIQUE KEY uk_notification_setting_member).
 * 아직 설정을 한 번도 바꾼 적 없는 회원은 행 자체가 없으며,
 * 그 경우 {@link #defaultEnabled(Long)}가 기본값을 대신한다.
 */
@Getter
public class NotificationSetting {

    private final Long notificationSettingId;
    private final Long memberId;
    private final boolean performanceShortageEnabled;
    private final boolean benefitLimitEnabled;
    private final LocalDateTime updatedAt;

    /**
     * MyBatis resultMap의 constructor 매핑이 이 시그니처를 그대로 쓴다.
     * 인자 순서를 바꾸면 XML의 arg 순서도 함께 바꿔야 한다.
     */
    @Builder
    public NotificationSetting(
        Long notificationSettingId,
        Long memberId,
        boolean performanceShortageEnabled,
        boolean benefitLimitEnabled,
        LocalDateTime updatedAt
    ) {
        this.notificationSettingId = notificationSettingId;
        this.memberId = memberId;
        this.performanceShortageEnabled = performanceShortageEnabled;
        this.benefitLimitEnabled = benefitLimitEnabled;
        this.updatedAt = updatedAt;
    }

    /**
     * 설정 행이 없는 회원에게 적용할 기본값.
     * <p>
     * 테이블 DEFAULT도 1(수신)이라 "행이 없는 회원"과 "한 번도 안 바꾼 회원"이
     * 같은 값을 보게 된다. 기본값을 여기서만 정의해 두 곳이 어긋나지 않게 한다.
     */
    public static NotificationSetting defaultEnabled(Long memberId) {
        return NotificationSetting.builder()
            .memberId(memberId)
            .performanceShortageEnabled(true)
            .benefitLimitEnabled(true)
            .build();
    }
}
