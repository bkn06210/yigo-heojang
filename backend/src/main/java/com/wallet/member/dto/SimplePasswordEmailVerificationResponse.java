package com.wallet.member.dto;

public record SimplePasswordEmailVerificationResponse(
    String email,
    long expiresIn
) {
}
