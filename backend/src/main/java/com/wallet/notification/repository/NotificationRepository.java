package com.wallet.notification.repository;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationSetting;
import com.wallet.notification.mapper.NotificationMapper;

/**
 * Mapper(순수 SQL 실행)와 상위 계층(서비스/배치) 사이에 놓이는 계층.
 * "SQL 결과에 약간의 규칙을 더해서" 돌려주는 역할을 한다 — 예를 들어
 * 설정 행이 없을 때 기본값을 채우는 것은 SQL로 표현하기보다 여기서 처리하는 게 명확하다.
 *
 * @Repository : 이 클래스를 스프링이 관리하는 빈으로 등록한다.
 * 일반 @Component와 거의 같지만, DB 예외를 스프링의 공통 예외 체계로
 * 변환해주는 부가 기능이 있다(지금 당장 이 클래스가 그 기능을 쓰진 않지만,
 * 관례적으로 @Repository를 사용한다).
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepository {
    private final NotificationMapper notificationMapper;

    /**
     * 규칙 문서 1.4절: 설정 행이 없는 회원은 두 알림 모두 활성화된 것으로 본다.
     * 호출하는 쪽(3~4번 커밋의 후보 계산 로직)은 "행이 있었는지 없었는지"를
     * 신경 쓸 필요 없이 이 메서드가 돌려주는 값만 보고 판단하면 된다.
     */
    public NotificationSetting findEffectiveSetting(Long memberId) {
        NotificationSetting setting = notificationMapper.selectSetting(memberId);
        return setting != null ? setting : NotificationSetting.defaultEnabled(memberId);
    }

    /**
     * 후보 알림들을 한 번에 저장하고, 실제로 새로 저장된 건수를 반환한다.
     * 빈 리스트로 벌크 insert SQL을 만들면 "VALUES" 뒤에 아무 값도 없는 잘못된
     * 쿼리가 되므로, SQL을 타기 전에 여기서 미리 막는다.
     */
    public int saveAll(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return 0;
        }
        return notificationMapper.insertAll(notifications);
    }

    public int countUnread(Long memberId) {
        return notificationMapper.countUnread(memberId);
    }
}