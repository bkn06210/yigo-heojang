package com.wallet.member.dto;

public record SimplePasswordEmailVerificationVerifyResponse(
    String simplePasswordChangeToken,
    long expiresIn
) {
}
