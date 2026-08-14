package com.wallet.member.domain;

// 탈퇴 사유를 정해진 값 중 하나로 받기 위한 enum
// 프론트에서 자유 텍스트로만 사유를 받으면 나중에 "탈퇴 사유 통계"를 낼 때
// 문자열을 일일이 분류해야 하므로, 항목을 미리 정해두고 상세 설명은
// reasonDetail(자유 텍스트)로 별도로 받는다.
//
// DB의 member_withdrawal.reason_type(VARCHAR(50), NULL 허용)과 이름을 맞춘다.
// 컬럼이 NULL을 허용하는 이유는 관리자에 의한 강제 탈퇴 등 사유 선택이
// 없는 경로도 나중에 생길 수 있어서이며, 회원 스스로 탈퇴하는 이번 기능에서는
// MemberWithdrawRequest에서 필수값으로 받는다.
public enum WithdrawalReasonType {
    LOW_USAGE,          // 서비스를 자주 사용하지 않음
    INCONVENIENT_UX,    // 사용이 불편함
    PRIVACY_CONCERN,    // 개인정보 제공이 부담스러움
    FOUND_ALTERNATIVE,  // 다른 서비스로 대체함
    OTHER               // 기타 (reasonDetail에 상세 사유를 남기도록 유도)
}
