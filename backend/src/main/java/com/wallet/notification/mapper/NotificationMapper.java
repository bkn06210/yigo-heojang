package com.wallet.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationSetting;

public interface NotificationMapper {
    /**
     * 회원의 알림 설정 행을 조회한다. 행이 없으면 null을 반환한다.
     * "행이 없을 때 기본값으로 채우는" 처리는 여기서 하지 않고 Repository에서 한다.
     * 매퍼는 SQL 결과를 그대로 반환하는 책임만 진다.
     */
    NotificationSetting selectSetting(@Param("memberId") Long memberId);

    /**
     * 알림 후보 목록을 한 번에 저장한다.
     * deduplication_key가 겹치는 행은 SQL 단에서 조용히 건너뛰고(=INSERT IGNORE),
     * 반환값은 "실제로 새로 들어간 행 수"다. 이 값을 그대로 호출한 쪽에 돌려주면
     * 이후 Redis 안 읽은 수 캐시를 INCRBY할 때 그대로 쓸 수 있다.
     */
    int insertAll(@Param("notifications") List<Notification> notifications);

    /**
     * 규칙 문서 6.2절의 MySQL 기준 COUNT 쿼리.
     * 지금은 이 메서드가 곧 "안 읽은 수 조회"이지만, 6번 커밋에서 Redis 캐시가
     * 앞단에 씌워지면 이 메서드는 캐시 miss일 때만 호출되는 fallback이 된다.
     */
    int countUnread(@Param("memberId") Long memberId);
}