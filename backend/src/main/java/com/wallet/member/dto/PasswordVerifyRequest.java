package com.wallet.member.dto;

import javax.validation.constraints.NotBlank;

public record PasswordVerifyRequest(
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password
) {
}
