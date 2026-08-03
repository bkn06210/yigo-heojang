package com.wallet.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank(message = "비밀번호 재설정 토큰은 필수입니다.")
    String passwordResetToken,

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해 주세요.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]+$",
        message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 하며, 다른 문자는 사용할 수 없습니다."
    )
    String newPassword
) {
}