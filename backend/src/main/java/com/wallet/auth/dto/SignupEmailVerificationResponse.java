package com.wallet.auth.dto;

public record SignupEmailVerificationResponse(
    String email,
    long expiresInSeconds
) {
}