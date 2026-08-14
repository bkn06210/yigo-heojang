package com.wallet.member.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public record SimplePasswordUpdateRequest(
    @NotBlank(message = "간편비밀번호 변경 토큰은 필수입니다.")
    String simplePasswordChangeToken,

    /*
     * 숫자로 받으면 012345의 앞자리 0이 사라질 수 있으므로 문자열로 받고,
     * 정규식으로 숫자 6자리만 허용한다.
     */
    @NotBlank(message = "간편비밀번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "간편비밀번호는 6자리 숫자여야 합니다.")
    String simplePassword,

    @NotBlank(message = "간편비밀번호 확인은 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "간편비밀번호 확인은 6자리 숫자여야 합니다.")
    String simplePasswordConfirm
) {
}
