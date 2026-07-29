package com.wallet.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public record SignupEmailVerificationRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    String email
) {
}