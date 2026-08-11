package com.wallet.auth.dto;

public record PasswordResetCodeVerifyResponse(
    String passwordResetToken,
    long expiresIn
) {
}