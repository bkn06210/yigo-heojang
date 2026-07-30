package com.wallet.auth.domain;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class PasswordResetVerification {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private Long passwordResetVerificationId;
    private Long memberId;
    private String verificationCodeHash;
    private String verificationStatus;
    private Integer failedAttemptCount;
    private LocalDateTime verificationCodeExpiresAt;
    private String resetTokenHash;
    private LocalDateTime resetTokenExpiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PasswordResetVerification createPending(
        Long memberId,
        String verificationCodeHash,
        LocalDateTime verificationCodeExpiresAt
    ) {
        PasswordResetVerification verification = new PasswordResetVerification();
        verification.memberId = memberId;
        verification.verificationCodeHash = verificationCodeHash;
        verification.verificationStatus = STATUS_PENDING;
        verification.failedAttemptCount = 0;
        verification.verificationCodeExpiresAt = verificationCodeExpiresAt;
        return verification;
    }
}