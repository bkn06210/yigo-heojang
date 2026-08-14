package com.wallet.notification.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * notification 테이블 한 행을 표현하는 도메인 객체.
 * <p>
 * pointHistoryId처럼 이번 알림 규칙(실적 부족 / 혜택 한도)과 무관한 컬럼도 있는데,
 * 스키마 전체를 그대로 반영해두어야 나중에 다른 종류의 알림(포인트 적립 알림 등)이
 * 추가될 때 테이블 구조와 도메인 구조가 계속 1:1로 맞는다.
 *
 * @NoArgsConstructor : 인자 없는 생성자. 프레임워크(MyBatis, Jackson 등)가
 * "일단 빈 객체를 만들고 나중에 채운다" 방식으로 동작할 때 필요하다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long notificationId;
    private Long memberId;

    // 개별 알림이면 값이 있고, 다이제스트 알림이면 규칙 문서 4장에 따라 NULL로 저장한다.
    private Long userCardId;
    private Long benefitId;

    // 이번 알림 규칙과 무관한 컬럼. 값을 채우지 않으면 자연스럽게 NULL로 저장된다.
    private Long pointHistoryId;

    private NotificationType notificationType;
    private String title;
    private String content;
    private NotificationStatus notificationStatus;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime deletedAt;

    // PERF_SHORTAGE:{userCardId}:MONTH:{yearMonth}:D7 같은 형태의 중복 방지 키.
    // notification 테이블의 UNIQUE(member_id, deduplication_key) 제약과 짝을 이룬다.
    private String deduplicationKey;
}