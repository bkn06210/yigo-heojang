package com.wallet.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.notification.domain.NotificationListItemResult;
import com.wallet.notification.domain.NotificationType;
import com.wallet.notification.dto.NotificationListResponse;
import com.wallet.notification.repository.NotificationRepository;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 다음 페이지가 없으면 hasNext는 false다")
    void getNotifications_success_noNextPage() {
        // given
        Long memberId = 1L;
        NotificationListItemResult item = NotificationListItemResult.builder()
            .notificationId(100L)
            .notificationType(NotificationType.PERFORMANCE_SHORTAGE)
            .title("실적 부족 안내")
            .content("이번 달 실적이 목표에 못 미쳤어요.")
            .createdAt(LocalDateTime.of(2026, 8, 1, 9, 0))
            .readAt(null)
            .build();

        // size=20으로 요청했으니 Repository에는 21건을 요청한다.
        // 반환은 1건뿐이라 hasNext는 false여야 한다.
        when(notificationRepository.findActiveByMember(memberId, 11, 0))
            .thenReturn(List.of(item));

        // when
        NotificationListResponse response =
            notificationService.getNotifications(memberId, 0, 10);

        // then
        assertThat(response.notifications()).hasSize(1);
        assertThat(response.notifications().get(0).notificationId()).isEqualTo(100L);
        assertThat(response.notifications().get(0).read()).isFalse();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - size보다 많이 조회되면 잘라내고 hasNext는 true다")
    void getNotifications_success_hasNextPage() {
        // given
        Long memberId = 1L;
        int size = 2;

        // size + 1 = 3건을 미끼로 반환해, "다음 페이지가 있다"는 걸 재현한다.
        List<NotificationListItemResult> threeItems = List.of(
            NotificationListItemResult.builder()
                .notificationId(3L).notificationType(NotificationType.BENEFIT_LIMIT)
                .title("t3").content("c3")
                .createdAt(LocalDateTime.of(2026, 8, 3, 0, 0)).readAt(null).build(),
            NotificationListItemResult.builder()
                .notificationId(2L).notificationType(NotificationType.BENEFIT_LIMIT)
                .title("t2").content("c2")
                .createdAt(LocalDateTime.of(2026, 8, 2, 0, 0))
                .readAt(LocalDateTime.of(2026, 8, 2, 1, 0)).build(),
            NotificationListItemResult.builder()
                .notificationId(1L).notificationType(NotificationType.BENEFIT_LIMIT)
                .title("t1").content("c1")
                .createdAt(LocalDateTime.of(2026, 8, 1, 0, 0)).readAt(null).build()
        );

        when(notificationRepository.findActiveByMember(memberId, size + 1, 0))
            .thenReturn(threeItems);

        // when
        NotificationListResponse response =
            notificationService.getNotifications(memberId, 0, size);

        // then
        assertThat(response.notifications()).hasSize(2);
        assertThat(response.notifications().get(1).read()).isTrue();
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("알림 목록 조회 실패 - size가 허용 범위를 넘으면 예외가 발생한다")
    void getNotifications_fail_sizeTooLarge() {
        // given
        Long memberId = 1L;

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> notificationService.getNotifications(memberId, 0, 51)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QUERY_PARAMETER_INVALID);
        verify(notificationRepository, never()).findActiveByMember(memberId, 52, 0);
    }

    @Test
    @DisplayName("읽음 처리 성공 - 처음 읽는 알림이면 갱신한다")
    void markAsRead_success_firstRead() {
        // given
        Long memberId = 1L;
        Long notificationId = 100L;

        when(notificationRepository.markAsRead(notificationId, memberId))
            .thenReturn(1);

        // when
        notificationService.markAsRead(memberId, notificationId);

        // then
        verify(notificationRepository, never())
            .existsActiveByIdAndMemberId(notificationId, memberId);
    }

    @Test
    @DisplayName("읽음 처리 성공 - 이미 읽은 알림이면 예외 없이 종료한다(멱등)")
    void markAsRead_success_alreadyRead() {
        // given
        Long memberId = 1L;
        Long notificationId = 100L;

        when(notificationRepository.markAsRead(notificationId, memberId))
            .thenReturn(0);
        when(notificationRepository.existsActiveByIdAndMemberId(notificationId, memberId))
            .thenReturn(true);

        // when & then (예외가 발생하지 않아야 한다)
        notificationService.markAsRead(memberId, notificationId);
    }

    @Test
    @DisplayName("읽음 처리 실패 - 존재하지 않거나 소유하지 않은 알림이면 예외가 발생한다")
    void markAsRead_fail_notFound() {
        // given
        Long memberId = 1L;
        Long notificationId = 999L;

        when(notificationRepository.markAsRead(notificationId, memberId))
            .thenReturn(0);
        when(notificationRepository.existsActiveByIdAndMemberId(notificationId, memberId))
            .thenReturn(false);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> notificationService.markAsRead(memberId, notificationId)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 처리 성공 - 로그인 회원 소유의 알림을 삭제한다")
    void deleteNotification_success() {
        // given
        Long memberId = 1L;
        Long notificationId = 100L;

        when(notificationRepository.softDelete(notificationId, memberId))
            .thenReturn(1);

        // when & then (예외가 발생하지 않아야 한다)
        notificationService.deleteNotification(memberId, notificationId);
    }

    @Test
    @DisplayName("삭제 처리 실패 - 다른 회원 소유의 알림이면 예외가 발생한다")
    void deleteNotification_fail_notOwned() {
        // given
        Long memberId = 1L;
        Long notificationId = 100L;

        when(notificationRepository.softDelete(notificationId, memberId))
            .thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> notificationService.deleteNotification(memberId, notificationId)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}