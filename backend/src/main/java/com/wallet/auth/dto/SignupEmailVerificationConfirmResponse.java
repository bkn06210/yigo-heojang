package com.wallet.auth.dto;

public record SignupEmailVerificationConfirmResponse(
    String signupVerificationToken,
    long expiresInSeconds
) {
}