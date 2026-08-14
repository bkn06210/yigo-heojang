package com.wallet.member.domain;

import java.time.LocalDateTime;

import lombok.Getter;

// member_withdrawal 테이블 한 행에 대응하는 도메인 객체다.
// "왜 탈퇴했는지"라는 통계성 정보만 담고 있어서, 이메일·성명 같은 실제 개인정보는
// MemberWithdrawalArchive의 역할이다. 그래서 이 정보는 3년 같은
// 보관 기한 없이 계속 남아 있어도 된다.
@Getter
public class MemberWithdrawal {
    private Long memberWithdrawalId;
    private Long memberId;
    private WithdrawalReasonType reasonType;
    private String reasonDetail;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;

    // withdrawnAt을 파라미터로 받는 이유: 탈퇴 트랜잭션 하나에서 member.withdrawn_at,
    // 이 테이블의 withdrawn_at, member_withdrawal_archive의 withdrawn_at까지
    // 여러 곳에 같은 시각을 남겨야 하는데, 각자 LocalDateTime.now()를 부르면
    // 호출 시점 차이만큼 값이 미세하게 달라질 수 있다.
    public static MemberWithdrawal create(
        Long memberId,
        WithdrawalReasonType reasonType,
        String reasonDetail,
        LocalDateTime withdrawnAt
    ) {
        MemberWithdrawal memberWithdrawal = new MemberWithdrawal();
        memberWithdrawal.memberId = memberId;
        memberWithdrawal.reasonType = reasonType;
        memberWithdrawal.reasonDetail = reasonDetail;
        memberWithdrawal.withdrawnAt = withdrawnAt;
        return memberWithdrawal;
    }
}