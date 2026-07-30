package com.wallet.auth.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public record TermAgreementRequest(
    @NotNull(message = "약관 버전 ID는 필수입니다.")
    @Positive(message = "약관 버전 ID는 양수여야 합니다.")
    Long termsVersionId,

    @NotNull(message = "약관 동의 여부는 필수입니다.")
    Boolean agreed
) {
}