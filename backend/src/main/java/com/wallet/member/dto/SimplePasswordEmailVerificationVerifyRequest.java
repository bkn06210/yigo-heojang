package com.wallet.member.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public record SimplePasswordEmailVerificationVerifyRequest(
    @NotBlank(message = "인증 코드는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    String verificationCode
) {
}
