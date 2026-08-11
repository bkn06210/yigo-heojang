package com.wallet.notification.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.common.ApiResponse;
import com.wallet.notification.dto.NotificationListResponse;
import com.wallet.notification.dto.NotificationUnreadCountResponse;
import com.wallet.notification.service.NotificationService;
import com.wallet.notification.service.NotificationUnreadCountService;

/**
 * @RestController : @Controller + @ResponseBody가 합쳐진 것이다.
 *                    메서드 반환값을 뷰(JSP 등)로 렌더링하지 않고, 그대로 JSON으로
 *                    응답 본문에 써준다. 이 프로젝트는 JSP 화면이 없는 API 서버이므로
 *                    모든 컨트롤러가 @RestController를 쓴다.
 * @RequestMapping : 이 컨트롤러의 모든 메서드가 공유하는 URL 앞부분("/api/notifications")을 지정한다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationUnreadCountService notificationUnreadCountService;

    /**
     * 로그인 회원의 알림 목록을 조회한다.
     * <p>
     * memberId는 요청 파라미터로 받지 않는다. 클라이언트가 다른 회원의 ID를
     * 보내면 그 회원의 알림을 볼 수 있게 되므로, JwtAuthenticationFilter가
     * 토큰 검증 후 request attribute에 넣어준 값만 신뢰한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        NotificationListResponse response =
            notificationService.getNotifications(memberId, page, size);

        return ResponseEntity.ok(
            ApiResponse.success("알림 목록 조회에 성공했습니다.", response));
    }

    /**
     * 알림을 읽음 처리한다.
     * <p>
     * 처리 결과로 돌려줄 데이터가 없으므로 ResponseEntity.noContent()로
     * 응답 본문 없이 HTTP 204 No Content만 반환한다.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(memberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** 알림을 삭제 처리한다. */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(memberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** 안 읽은 알림 수를 조회한다. Redis 캐시를 우선 확인하고, 없으면 MySQL로 계산한다. */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId
    ) {
        int unreadCount = notificationUnreadCountService.getUnreadCount(memberId);
        return ResponseEntity.ok(
            ApiResponse.success("안 읽은 알림 수 조회에 성공했습니다.", new NotificationUnreadCountResponse(unreadCount)));
    }
}