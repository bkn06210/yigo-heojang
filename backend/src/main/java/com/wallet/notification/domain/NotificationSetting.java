package com.wallet.notification.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * notification_setting 테이블 한 행을 표현하는 도메인 객체.
 * <p>
 * setter를 두지 않은 이유: 이 객체는 "그 시점의 회원 설정 상태"를 나타내는 값이라
 * 조회 이후에 임의로 값이 바뀌면 안 된다(불변 객체로 다룬다).
 */
@Getter
@Builder
@AllArgsConstructor
public class NotificationSetting {
    private Long notificationSettingId;
    private Long memberId;
    private boolean performanceShortageEnabled;
    private boolean benefitLimitEnabled;
    private LocalDateTime updatedAt;

    /**
     * notification_setting 행이 아예 없는 회원을 위한 기본값.
     * 규칙 문서 1.4: "설정 행이 없는 회원은 스키마 기본값과 같이 두 알림 모두
     * 활성화된 것으로 처리한다"를 코드로 옮긴 것.
     * <p>
     * notificationSettingId를 비워두는 것으로 "DB에 실제로 존재하는 행이 아니다"를 표현한다.
     */
    public static NotificationSetting defaultEnabled(Long memberId) {
        return NotificationSetting.builder()
            .memberId(memberId)
            .performanceShortageEnabled(true)
            .benefitLimitEnabled(true)
            .build();
    }
}