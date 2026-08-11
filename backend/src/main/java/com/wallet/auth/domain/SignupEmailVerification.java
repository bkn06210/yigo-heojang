package com.wallet.auth.domain;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class SignupEmailVerification {
    private Long signupEmailVerificationId;
    private String email;
    private String verificationCodeHash;
    private String verificationStatus;
    private Integer failedAttemptCount;
    private LocalDateTime verificationCodeExpiresAt;
    private String signupTokenHash;
    private LocalDateTime signupTokenExpiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SignupEmailVerification createPending(
        String email,
        String verificationCodeHash,
        LocalDateTime verificationCodeExpiresAt
    ) {
        SignupEmailVerification verification = new SignupEmailVerification();
        verification.email = email;
        verification.verificationCodeHash = verificationCodeHash;
        verification.verificationStatus = VerificationStatus.PENDING;
        verification.failedAttemptCount = 0;
        verification.verificationCodeExpiresAt = verificationCodeExpiresAt;
        return verification;
    }
}