package com.wallet.notification.repository;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.wallet.notification.batch.model.MemberDedupKeyLookup;
import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationListItemResult;
import com.wallet.notification.domain.NotificationSetting;
import com.wallet.notification.mapper.NotificationMapper;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final NotificationMapper notificationMapper;

    /**
     * 회원에게 실제로 적용되는 알림 설정을 돌려준다.
     * <p>
     * 설정을 한 번도 바꾼 적 없는 회원은 행이 없다. 그건 오류가 아니라 "아직 기본값"이라는 뜻이므로,
     * 호출하는 쪽마다 null 처리를 반복하지 않도록 여기서 기본값으로 채워 돌려준다.
     */
    public NotificationSetting findEffectiveSetting(Long memberId) {
        NotificationSetting setting = notificationMapper.findSettingByMemberId(memberId);

        if (setting == null) {
            return NotificationSetting.defaultEnabled(memberId);
        }

        return setting;
    }

    /** 알림 설정값 저장. */
    public void saveSetting(NotificationSetting setting) {
        notificationMapper.upsertSetting(setting);
    }

    /** 삭제되지 않은 알림을 최신순으로 조회한다. */
    public List<NotificationListItemResult> findActiveByMember(Long memberId, int limit, int offset) {
        return notificationMapper.selectActiveByMemberId(memberId, limit, offset);
    }

    /** 안 읽고 삭제되지 않은 알림 수. */
    public int countUnread(Long memberId) {
        return notificationMapper.countUnread(memberId);
    }

    /** 읽음 처리. 실제로 상태가 바뀐 행 수를 돌려준다(이미 읽었으면 0). */
    public int markAsRead(Long notificationId, Long memberId) {
        return notificationMapper.markAsRead(notificationId, memberId);
    }

    /** 삭제되지 않은 내 알림이 존재하는지. */
    public boolean existsActiveByIdAndMemberId(Long notificationId, Long memberId) {
        return notificationMapper.existsActiveByIdAndMemberId(notificationId, memberId);
    }

    /** 소프트 삭제. 지운 행 수를 돌려준다(없거나 남의 알림이면 0). */
    public int softDelete(Long notificationId, Long memberId) {
        return notificationMapper.softDeleteByIdAndMemberId(notificationId, memberId);
    }

    /**
     * 알림을 한 번에 저장하고 실제로 삽입된 건수를 돌려준다.
     * 이미 같은 (회원, 중복방지키)로 저장된 건은 건너뛴다.
     * <p>
     * 빈 목록으로 호출하면 유효하지 않은 SQL(VALUES 뒤가 빈 INSERT)이 만들어지므로 미리 막는다.
     */
    public int saveAllIgnoringDuplicates(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return 0;
        }

        return notificationMapper.insertAll(notifications);
    }

    /**
     * 넘긴 (회원, 중복방지키) 조합 중 이미 알림이 나간 것만 돌려준다.
     * 빈 입력이면 조회하지 않는다 — JOIN 대상 파생 테이블이 비면 SQL이 깨진다.
     */
    public List<MemberDedupKeyLookup> findExistingMemberDedupKeys(List<MemberDedupKeyLookup> lookups) {
        if (lookups == null || lookups.isEmpty()) {
            return List.of();
        }

        return notificationMapper.findExistingMemberDedupKeys(lookups);
    }
}
