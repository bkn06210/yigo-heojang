package com.wallet.member.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record MemberUpdateRequest(
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 50, message = "닉네임은 50자 이하로 입력해주세요.")
    String nickname
) {
}