package com.wallet.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 회원 탈퇴 시 신용/거래 정보(카드·결제·포인트·추천 도메인)를 즉시 물리 삭제하는 전용 매퍼
@Mapper
public interface CreditInfoWithdrawalMapper {
    // 아래 11개는 반드시 이 순서(선언 순서가 곧 호출 순서)로 실행해야 한다.
    // FK 참조 관계상 자식 테이블을 부모 테이블보다 먼저 지워야 하기 때문이다.
    //
    // 주의: 이 11개를 실행하기 전에 MemberDataCleanupMapper의 notification 삭제가
    // 반드시 먼저 끝나 있어야 한다. notification이 이 그룹의 point_history·user_card를
    // FK로 참조하고 있어서다. 이 제약은 매퍼 두 개에 걸쳐 있어 어느 한쪽 인터페이스만
    // 봐서는 알 수 없으므로, 실제 호출 순서는 서비스 계층에서 명시적으로 관리한다.

    // 1. payment_qr: payment를 참조하므로 payment보다 먼저 지운다.
    int deletePaymentQrByMemberId(@Param("memberId") Long memberId);

    // 2. payment: expense를 참조하므로 expense보다 먼저 지운다.
    int deletePaymentByMemberId(@Param("memberId") Long memberId);

    // 3. point_history: expense와 point_wallet을 참조하므로 둘 다보다 먼저 지운다.
    //    (전제: notification이 이미 지워져 있어야 한다 — 위 주의사항 참고)
    int deletePointHistoryByMemberId(@Param("memberId") Long memberId);

    // 4. expense: 1~3이 먼저 지워져야 지울 수 있다.
    int deleteExpenseByMemberId(@Param("memberId") Long memberId);

    // 5. point_wallet: point_history가 먼저 지워져야 지울 수 있다.
    int deletePointWalletByMemberId(@Param("memberId") Long memberId);

    // 6~8. member_id 컬럼이 없고 user_card_id로만 연결되어 있어,
    // "이 회원 소유의 보유카드 ID들"을 서브쿼리로 찾아 지운다.
    // user_card 삭제(9번)보다 반드시 먼저 실행해야 한다.
    int deleteUserCardBenefitSelectionByMemberId(@Param("memberId") Long memberId);

    int deleteUserCardMonthlyStateByMemberId(@Param("memberId") Long memberId);

    int deleteUserBenefitUsageByMemberId(@Param("memberId") Long memberId);

    // 9. user_card: 4번(expense)과 6~8번이 먼저 지워져야 지울 수 있다.
    //    (전제: notification이 이미 지워져 있어야 한다 — 위 주의사항 참고)
    int deleteUserCardByMemberId(@Param("memberId") Long memberId);

    // 10~11. member만 참조하는 독립적인 테이블이다.
    int deleteMembershipRegisterByMemberId(@Param("memberId") Long memberId);

    int deleteRecommendInputByMemberId(@Param("memberId") Long memberId);
}
