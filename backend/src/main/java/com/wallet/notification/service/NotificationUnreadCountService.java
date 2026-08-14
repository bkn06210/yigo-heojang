package com.wallet.notification.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.notification.repository.NotificationRepository;

/**
 * 안 읽은 알림 수 조회.
 * <p>
 * 이 값은 캐시하지 않고 매번 MySQL에서 센다. 의도적인 선택이다.
 * <p>
 * 캐시(Redis)를 앞에 두면 "쓰기(읽음 처리·삭제)가 캐시를 갱신한 직후, 그보다 먼저
 * 시작됐던 조회가 뒤늦게 도착해 옛날 값으로 덮어쓰는" 경쟁이 생긴다. TTL이 긴 만큼
 * 한 번 덮이면 꽤 오래 틀린 수가 보인다. 지연 삭제를 한 번 더 예약하는 식으로 창을
 * 좁힐 수는 있어도 완전히 닫히지는 않는다.
 * <p>
 * 반면 이 COUNT는 idx_notification_member_read (member_id, read_at) 인덱스를 그대로
 * 타는 단순 조회이고, 한 회원의 알림 건수는 캐시를 둘 만큼 크지 않다. 캐시를 없애면
 * 그 경쟁 자체가 존재하지 않게 되고, Redis 연결·비밀번호·장애 대비 같은 운영 부담도
 * 함께 사라진다. 트래픽이 커져 이 조회가 실제로 병목이 되면 그때 캐시를 도입하되,
 * 그때는 무효화 순서를 명시적으로 설계해야 한다.
 */
@RequiredArgsConstructor
@Service
public class NotificationUnreadCountService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public int getUnreadCount(Long memberId) {
        return notificationRepository.countUnread(memberId);
    }
}
