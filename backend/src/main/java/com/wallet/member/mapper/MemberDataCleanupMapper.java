package com.wallet.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 회원 탈퇴 시 신용정보가 아닌 나머지 데이터(알림, 개인화 설정, 인증 부가정보)를 즉시 물리 삭제하는 매퍼
@Mapper
public interface MemberDataCleanupMapper {

    // notification이 point_history·user_card를 FK로 참조하고 있어
    // (fk_notification_point_history, fk_notification_user_card),
    // CreditInfoWithdrawalMapper의 point_history·user_card 삭제보다
    // 반드시 먼저 실행해야 한다. 이 순서는 서비스 계층이 관리한다.
    int deleteNotificationByMemberId(@Param("memberId") Long memberId);

    // member만 참조하는 독립적인 테이블이라 순서 제약이 없다.
    int deleteNotificationSettingByMemberId(@Param("memberId") Long memberId);

    int deleteMemberPreferredCategoryByMemberId(@Param("memberId") Long memberId);

    int deleteMemberPreferredMerchantByMemberId(@Param("memberId") Long memberId);

    // member_personalization_brand가 member_personalization_category를 참조한다
    // (ON DELETE CASCADE 있음). category만 지워도 DB가 brand를 자동으로 같이
    // 지워주지만, 이 스키마 전체에서 CASCADE를 쓰는 곳이 여기 한 곳뿐이라
    // 다른 삭제들과 마찬가지로 명시적으로 먼저 지운다.
    int deleteMemberPersonalizationBrandByMemberId(@Param("memberId") Long memberId);

    int deleteMemberPersonalizationCategoryByMemberId(@Param("memberId") Long memberId);

    int deletePasswordResetVerificationByMemberId(@Param("memberId") Long memberId);

    // 간편비밀번호 변경용 이메일 인증 이력. member를 FK로 참조하며, 탈퇴하면
    // 간편비밀번호 자체가 의미를 잃으므로 인증 이력도 함께 지운다.
    int deleteSimplePasswordVerificationByMemberId(@Param("memberId") Long memberId);
}
