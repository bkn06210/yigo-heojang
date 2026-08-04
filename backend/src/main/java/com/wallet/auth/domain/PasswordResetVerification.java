package com.wallet.auth.domain;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class PasswordResetVerification {
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
        verification.verificationStatus = VerificationStatus.PENDING;
        verification.failedAttemptCount = 0;
        verification.verificationCodeExpiresAt = verificationCodeExpiresAt;
        return verification;
    }
}