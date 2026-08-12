package com.wallet.notification.dto;

import java.time.LocalDateTime;

import com.wallet.notification.domain.NotificationListItemResult;
import com.wallet.notification.domain.NotificationType;

/**
 * 알림 목록에서 알림 한 건을 표현하는 응답 DTO.
 */
public record NotificationListItemResponse(
    Long notificationId,
    NotificationType notificationType,
    String title,
    String content,
    boolean read,
    LocalDateTime createdAt
) {
    public static NotificationListItemResponse from(NotificationListItemResult result) {
        return new NotificationListItemResponse(
            result.getNotificationId(),
            result.getNotificationType(),
            result.getTitle(),
            result.getContent(),
            result.getReadAt() != null,
            result.getCreatedAt()
        );
    }
}