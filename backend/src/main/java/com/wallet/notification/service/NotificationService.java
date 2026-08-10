package com.wallet.notification.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.notification.domain.NotificationListItemResult;
import com.wallet.notification.dto.NotificationListItemResponse;
import com.wallet.notification.dto.NotificationListResponse;
import com.wallet.notification.repository.NotificationRepository;

@RequiredArgsConstructor
@Service
public class NotificationService {

    // 한 번에 너무 많은 행을 요청하지 못하도록 막는 상한. 예를 들어 size=100000 같은
    // 요청이 들어오면 DB에 부담을 주므로, 여기서 미리 잘라낸다.
    private static final int MAX_PAGE_SIZE = 10;

    private final NotificationRepository notificationRepository;

    /**
     * @Transactional(readOnly = true) : 이 메서드 안의 DB 작업은 조회만 한다는 표시다.
     *                                   데이터를 바꾸지 않는다는 걸 미리 알려주면, DB 드라이버와 트랜잭션 매니저가
     *                                   불필요한 잠금이나 변경 감지 작업을 생략할 수 있어 조회 성능에 도움이 된다.
     */
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long memberId, int page, int size) {
        validatePageRequest(page, size);

        int offset = page * size;
        // 다음 페이지 존재 여부를 판단하기 위해 요청한 size보다 1건 더 조회한다.
        List<NotificationListItemResult> results =
            notificationRepository.findActiveByMember(memberId, size + 1, offset);

        boolean hasNext = results.size() > size;
        List<NotificationListItemResult> pageItems = hasNext
            ? results.subList(0, size)
            : results;

        List<NotificationListItemResponse> items = pageItems.stream()
            .map(NotificationListItemResponse::from)
            .toList();

        return NotificationListResponse.from(items, hasNext);
    }

    /**
     * 알림을 읽음 처리한다.
     * <p>
     * markAsRead가 0건을 갱신했다면 두 가지 경우 중 하나다.
     * 1) 그 알림이 애초에 없거나(존재하지 않음/다른 회원 소유/이미 삭제됨) → 에러
     * 2) 이미 읽은 상태라 다시 읽음 처리할 게 없음 → 에러 없이 성공으로 취급(멱등)
     * existsActiveByIdAndMemberId로 이 둘을 구분한다.
     */
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, memberId);
        if (updated == 1) {
            return;
        }

        boolean exists = notificationRepository.existsActiveByIdAndMemberId(notificationId, memberId);
        if (!exists) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        // exists == true인데 updated == 0이면 이미 읽음 상태였다는 뜻이므로 그대로 종료한다.
    }

    /**
     * 알림을 삭제 처리한다.
     * UPDATE의 WHERE 절에 memberId를 포함시켜서, 이 한 번의 쿼리로
     * "그 알림이 존재하는지"와 "로그인 회원의 소유가 맞는지"를 동시에 검증한다.
     * (다른 회원의 알림 ID를 넣으면 조건에 안 걸려 영향받은 행이 0건이 되고,
     * 그 상태를 아래에서 NOTIFICATION_NOT_FOUND로 응답한다.)
     */
    @Transactional
    public void deleteNotification(Long memberId, Long notificationId) {
        int updated = notificationRepository.softDelete(notificationId, memberId);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.QUERY_PARAMETER_INVALID);
        }
    }
}