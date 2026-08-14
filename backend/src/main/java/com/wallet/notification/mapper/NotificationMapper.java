package com.wallet.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.notification.batch.model.MemberDedupKeyLookup;
import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationListItemResult;
import com.wallet.notification.domain.NotificationSetting;

@Mapper
public interface NotificationMapper {

    /**
     * 회원의 알림 설정 행을 조회한다. 아직 설정을 바꾼 적 없는 회원은 행이 없어 null이 온다.
     * 기본값 처리는 조회한 쪽(NotificationRepository)이 맡는다.
     */
    NotificationSetting findSettingByMemberId(@Param("memberId") Long memberId);

    /**
     * 알림 설정을 저장한다. member_id에 이미 행이 있으면 갱신하고, 없으면 새로 만든다
     * (UNIQUE KEY uk_notification_setting_member 덕분에 이 판단을 MySQL이 원자적으로 해준다 —
     * "조회해서 있으면 UPDATE, 없으면 INSERT"처럼 애플리케이션에서 직접 분기하면, 그 사이에
     * 다른 요청이 끼어들어 같은 회원의 행이 중복 생성될 수 있는 경쟁 상태가 생긴다).
     */
    void upsertSetting(@Param("setting") NotificationSetting setting);

    /**
     * 삭제되지 않은 알림을 최신순으로 조회한다.
     * hasNext 판정을 위해 호출하는 쪽이 limit에 1을 더해 넘긴다.
     */
    List<NotificationListItemResult> selectActiveByMemberId(
        @Param("memberId") Long memberId,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /** 안 읽고 삭제되지 않은 알림 수. */
    int countUnread(@Param("memberId") Long memberId);

    /** 안 읽은 알림을 읽음으로 바꾼다. 이미 읽었거나 남의 알림이면 0을 반환한다. */
    int markAsRead(
        @Param("notificationId") Long notificationId,
        @Param("memberId") Long memberId
    );

    /** 삭제되지 않은 내 알림이 존재하는지. 읽음 처리 0건의 원인을 구분할 때 쓴다. */
    boolean existsActiveByIdAndMemberId(
        @Param("notificationId") Long notificationId,
        @Param("memberId") Long memberId
    );

    /** 알림을 소프트 삭제한다. 남의 알림이거나 이미 삭제됐으면 0을 반환한다. */
    int softDeleteByIdAndMemberId(
        @Param("notificationId") Long notificationId,
        @Param("memberId") Long memberId
    );

    /**
     * 알림 여러 건을 한 번에 저장한다. 이미 같은 (회원, 중복방지키) 조합이 있으면 그 행만 건너뛴다.
     * 실제로 삽입된 건수를 반환한다.
     */
    int insertAll(@Param("notifications") List<Notification> notifications);

    /**
     * 넘긴 (회원, 중복방지키) 조합 중 이미 알림이 발송된 것만 골라 돌려준다.
     * 같은 한도에 대해 EXHAUSTED가 이미 나갔으면 NEAR를 다시 보내지 않기 위한 조회다.
     */
    List<MemberDedupKeyLookup> findExistingMemberDedupKeys(
        @Param("lookups") List<MemberDedupKeyLookup> lookups
    );
}
