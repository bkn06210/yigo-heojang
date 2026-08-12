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

    /**
     * 회원의 삭제되지 않은 알림 목록을 최신순으로 조회한다.
     * limit을 "요청한 size + 1"로 넘기면, 결과가 size보다 많이 오는지 보고
     * 다음 페이지 존재 여부(hasNext)를 별도 COUNT 쿼리 없이 판단할 수 있다.
     */
    List<NotificationListItemResult> selectActiveByMemberId(
        @Param("memberId") Long memberId,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 아직 읽지 않은 알림을 읽음 처리한다.
     * WHERE 절에 memberId와 read_at IS NULL을 함께 걸어서,
     * "내 알림이 아니거나(소유권 위반)" "이미 읽은 알림이거나(중복 처리)" 인 경우
     * 둘 다 영향받은 행이 0건이 되게 한다. 두 경우를 구분하는 건 Repository/Service 쪽 책임이다.
     */
    int markAsRead(
        @Param("notificationId") Long notificationId,
        @Param("memberId") Long memberId)
    ;

    /**
     * notificationId가 해당 회원 소유의 삭제되지 않은 알림으로 실제 존재하는지 확인한다.
     * markAsRead가 0건을 갱신했을 때, "존재하지 않아서 0건"인지
     * "이미 읽어서 0건"인지 구분하기 위한 보조 조회다.
     */
    boolean existsActiveByIdAndMemberId(
        @Param("notificationId") Long notificationId,
        @Param("memberId") Long memberId
    );

    /** 알림을 삭제 처리(soft delete)한다. */
    int softDeleteByIdAndMemberId(
        @Param("notificationId") Long notificationId,
        @Param("memberId") Long memberId
    );

    /**
     * "member_id:deduplication_key" 형태로 합친 후보 목록 중, 실제로 notification 테이블에
     * 존재하는 것만 골라 돌려준다. NEAR 알림을 만들기 전 같은 한도의 EXHAUSTED가 이미
     * 발송됐는지 한 번에 확인하는 용도(3.6절).
     */
    List<String> findExistingMemberDedupKeys(@Param("lookups") List<MemberDedupKeyLookup> lookups);
}