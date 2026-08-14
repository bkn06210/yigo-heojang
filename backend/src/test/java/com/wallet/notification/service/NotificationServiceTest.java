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

        // size=10으로 요청했으니 Repository에는 11건을 요청한다.
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

        // size + 1 = 3건을 반환해 "다음 페이지가 있다"는 상황을 재현한다.
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
        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> notificationService.getNotifications(1L, 0, 51)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QUERY_PARAMETER_INVALID);
        verify(notificationRepository, never()).findActiveByMember(1L, 52, 0);
    }

    @Test
    @DisplayName("읽음 처리 성공 - 안 읽은 알림이면 갱신된다")
    void markAsRead_success() {
        // given
        when(notificationRepository.markAsRead(10L, 1L)).thenReturn(1);

        // when
        notificationService.markAsRead(1L, 10L);

        // then
        // 갱신이 1건이면 존재 여부를 다시 확인할 이유가 없다.
        verify(notificationRepository, never()).existsActiveByIdAndMemberId(10L, 1L);
    }

    @Test
    @DisplayName("읽음 처리 멱등 - 이미 읽은 알림이면 예외 없이 끝난다")
    void markAsRead_idempotent_whenAlreadyRead() {
        // given
        when(notificationRepository.markAsRead(10L, 1L)).thenReturn(0);
        when(notificationRepository.existsActiveByIdAndMemberId(10L, 1L)).thenReturn(true);

        // when & then
        notificationService.markAsRead(1L, 10L);
    }

    @Test
    @DisplayName("읽음 처리 실패 - 없는 알림이면 NOTIFICATION_NOT_FOUND")
    void markAsRead_fail_whenNotFound() {
        // given
        when(notificationRepository.markAsRead(10L, 1L)).thenReturn(0);
        when(notificationRepository.existsActiveByIdAndMemberId(10L, 1L)).thenReturn(false);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> notificationService.markAsRead(1L, 10L)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 실패 - 다른 회원의 알림이면 NOTIFICATION_NOT_FOUND")
    void deleteNotification_fail_whenNotOwned() {
        // given
        // WHERE 절에 memberId가 들어 있어 남의 알림은 0건 갱신으로 돌아온다.
        when(notificationRepository.softDelete(10L, 1L)).thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> notificationService.deleteNotification(1L, 10L)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 성공 - 내 알림이면 소프트 삭제된다")
    void deleteNotification_success() {
        // given
        when(notificationRepository.softDelete(10L, 1L)).thenReturn(1);

        // when
        notificationService.deleteNotification(1L, 10L);

        // then
        verify(notificationRepository).softDelete(10L, 1L);
    }
}
