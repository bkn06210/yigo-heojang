package com.wallet.auth.dto;

import java.time.LocalDateTime;

public record TermResponse(
    Long termsId,
    String termsCode,
    String termsName,
    Boolean required,
    String termsStatus,
    Long termsVersionId,
    String version,
    String content,
    LocalDateTime effectiveStartedAt
) {
}