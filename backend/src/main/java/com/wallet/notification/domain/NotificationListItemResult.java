package com.wallet.notification.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 목록 조회 쿼리 결과 한 행을 담는 객체.
 * <p>
 * Notification(도메인 전체)을 그대로 쓰지 않고 목록 전용 객체를 따로 둔 이유:
 * 목록 화면에는 deduplicationKey, benefitId 같은 내부 처리용 값까지 보여줄 필요가 없다.
 */
@Getter
@Builder
@AllArgsConstructor
public class NotificationListItemResult {
    private Long notificationId;
    private NotificationType notificationType;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    // 읽음 여부를 boolean으로 미리 변환하지 않고 원본 시각을 그대로 들고 있는다.
    // "언제 읽었는지"가 필요해질 수도 있고, boolean 변환은 응답 DTO에서 하는 게
    // "쿼리 결과는 있는 그대로, 가공은 변환 단계에서"라는 책임 분리에 맞다.
    private LocalDateTime readAt;
}