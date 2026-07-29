package com.wallet.auth.domain;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class SignupEmailVerification {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";

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
        verification.verificationStatus = STATUS_PENDING;
        verification.failedAttemptCount = 0;
        verification.verificationCodeExpiresAt = verificationCodeExpiresAt;
        return verification;
    }
}