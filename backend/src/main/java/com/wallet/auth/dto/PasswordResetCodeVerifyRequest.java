package com.wallet.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public record PasswordResetCodeVerifyRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    String email,

    @NotBlank(message = "인증 코드는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    String verificationCode
) {
}