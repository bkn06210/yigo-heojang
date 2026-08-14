package com.wallet.member.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.wallet.member.domain.WithdrawalReasonType;

// DELETE /api/members/me 의 request body.
// 탈퇴는 되돌릴 수 없는 처리라, 로그인 때와 마찬가지로 비밀번호를 다시 입력받아
// 본인 확인을 한 번 더 한다.
public record MemberWithdrawRequest(
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password,

    @NotNull(message = "탈퇴 사유는 필수입니다.")
    WithdrawalReasonType reasonType,

    @Size(max = 500, message = "탈퇴 사유 상세는 500자 이하로 입력해주세요.")
    String reasonDetail,

    // 사용자가 탈퇴 화면에서 동의한 WITHDRAWAL_NOTICE 약관의 term_version_id.
    // 프론트가 보낸 값을 그대로 믿지 않고, 서비스 계층에서 "이게 지금 시점에
    // 실제로 유효한 탈퇴 고지 약관 버전이 맞는지"를
    // TermAgreementMapper(스코프=WITHDRAWAL)로 다시 검증한다.
    @NotNull(message = "탈퇴 안내 약관 동의는 필수입니다.")
    Long termVersionId
) {
}
