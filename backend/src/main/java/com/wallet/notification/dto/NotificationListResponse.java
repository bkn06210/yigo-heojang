package com.wallet.notification.dto;

import java.util.List;

/**
 * 알림 목록 조회 성공 응답 DTO.
 * <p>
 * totalCount는 전체 알림 개수가 아니라 "이번 응답에 담긴 개수"다.
 * 전체 개수를 구하려면 COUNT 쿼리를 별도로 한 번 더 날려야 하는데,
 * 다음 페이지가 있는지만 알면 되는 무한 스크롤/더보기 UI에서는
 * hasNext만으로 충분하므로 COUNT 쿼리를 추가하지 않는다.
 */
public record NotificationListResponse(
    List<NotificationListItemResponse> notifications,
    int totalCount,
    boolean hasNext
) {
    public static NotificationListResponse from(
        List<NotificationListItemResponse> notifications,
        boolean hasNext
    ) {
        return new NotificationListResponse(
            List.copyOf(notifications),
            notifications.size(),
            hasNext
        );
    }
}
