package com.wallet.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.PasswordResetVerification;
import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.dto.PasswordResetCodeRequest;
import com.wallet.auth.dto.PasswordResetCodeVerifyRequest;
import com.wallet.auth.dto.PasswordResetCodeVerifyResponse;
import com.wallet.auth.dto.PasswordResetRequest;
import com.wallet.auth.mapper.PasswordResetVerificationMapper;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.common.util.Sha256Hasher;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;

@RequiredArgsConstructor
@Service
public class PasswordResetService {
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final long PASSWORD_RESET_TOKEN_EXPIRE_MINUTES = 10;
    private static final long REISSUE_WAIT_SECONDS = 60;
    private static final int MAX_FAILED_ATTEMPT_COUNT = 5;

    private final PasswordResetVerificationMapper passwordResetVerificationMapper;
    private final MemberMapper memberMapper;
    private final EmailSender emailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationTokenGenerator passwordResetTokenGenerator;
    private final Sha256Hasher sha256Hasher;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void sendResetCode(PasswordResetCodeRequest request) {
        String email = normalizeEmail(request.email());

        Member member = memberMapper.findByEmail(email);

        if (member == null || !isActiveMember(member)) {
            return;
        }

        PasswordResetVerification existingVerification =
            passwordResetVerificationMapper.findLatestByMemberIdForUpdate(
                member.getMemberId()
            );

        validateReissueAllowed(existingVerification);

        String verificationCode = verificationCodeGenerator.generateSixDigitCode();
        String verificationCodeHash = sha256Hasher.sha256(verificationCode);
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusMinutes(VERIFICATION_CODE_EXPIRE_MINUTES);

        passwordResetVerificationMapper.expireActiveByMemberId(member.getMemberId());

        PasswordResetVerification verification = PasswordResetVerification.createPending(
            member.getMemberId(),
            verificationCodeHash,
            expiresAt
        );

        passwordResetVerificationMapper.insert(verification);

        try {
            emailSender.sendPasswordResetVerificationCode(
                email,
                verificationCode,
                VERIFICATION_CODE_EXPIRE_MINUTES
            );
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PasswordResetCodeVerifyResponse verifyResetCode(
        PasswordResetCodeVerifyRequest request
    ) {
        String email = normalizeEmail(request.email());

        Member member = memberMapper.findByEmail(email);
        if (member == null || !isActiveMember(member)) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        PasswordResetVerification verification =
            passwordResetVerificationMapper.findLatestByMemberIdForUpdate(
                member.getMemberId()
            );

        validateVerificationExists(verification);
        validateVerificationStatus(verification);
        validateAttemptLimit(verification);
        validateCodeNotExpired(verification);

        String requestCodeHash = sha256Hasher.sha256(request.verificationCode());
        if (!verification.getVerificationCodeHash().equals(requestCodeHash)) {
            handleCodeMismatch(verification);
        }

        String passwordResetToken = passwordResetTokenGenerator.generate();
        String resetTokenHash = sha256Hasher.sha256(passwordResetToken);
        LocalDateTime resetTokenExpiresAt = LocalDateTime.now()
            .plusMinutes(PASSWORD_RESET_TOKEN_EXPIRE_MINUTES);

        int updatedCount = passwordResetVerificationMapper.verify(
            verification.getPasswordResetVerificationId(),
            resetTokenHash,
            resetTokenExpiresAt
        );

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        return new PasswordResetCodeVerifyResponse(
            passwordResetToken,
            PASSWORD_RESET_TOKEN_EXPIRE_MINUTES * 60
        );
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String resetTokenHash = sha256Hasher.sha256(request.passwordResetToken());

        PasswordResetVerification verification =
            passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash);

        validateResetTokenExists(verification);
        validateResetTokenStatus(verification);
        validateResetTokenNotExpired(verification);

        Member member = memberMapper.findById(verification.getMemberId());

        if (member == null || !isActiveMember(member)) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());

        int passwordUpdatedCount = memberMapper.updatePassword(
            member.getMemberId(),
            encodedPassword
        );

        if (passwordUpdatedCount == 0) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        int usedCount = passwordResetVerificationMapper.markAsUsed(
            verification.getPasswordResetVerificationId()
        );

        if (usedCount == 0) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        refreshTokenService.revokeAllByPasswordReset(member.getMemberId());
    }

    private void validateVerificationExists(PasswordResetVerification verification) {
        if (verification == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }
    }

    private void validateVerificationStatus(PasswordResetVerification verification) {
        if (VerificationStatus.EXPIRED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }

        if (VerificationStatus.VERIFIED.equals(verification.getVerificationStatus())
            || VerificationStatus.USED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_ALREADY_USED);
        }

        if (!VerificationStatus.PENDING.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }
    }

    private void validateAttemptLimit(PasswordResetVerification verification) {
        if (verification.getFailedAttemptCount() >= MAX_FAILED_ATTEMPT_COUNT) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    private void validateCodeNotExpired(PasswordResetVerification verification) {
        if (!verification.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now())) {
            passwordResetVerificationMapper.expireVerificationCode(
                verification.getPasswordResetVerificationId()
            );
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }
    }

    private void handleCodeMismatch(PasswordResetVerification verification) {
        passwordResetVerificationMapper.increaseFailedAttemptCount(
            verification.getPasswordResetVerificationId()
        );

        if (verification.getFailedAttemptCount() + 1 >= MAX_FAILED_ATTEMPT_COUNT) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED);
        }

        throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
    }

    private void validateReissueAllowed(PasswordResetVerification verification) {
        if (verification == null || verification.getUpdatedAt() == null) {
            return;
        }

        LocalDateTime reissueAllowedAt = verification.getUpdatedAt()
            .plusSeconds(REISSUE_WAIT_SECONDS);

        if (reissueAllowedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_REISSUE_COOLDOWN);
        }
    }

    private boolean isActiveMember(Member member) {
        return "ACTIVE".equals(member.getMemberStatus())
            && member.getWithdrawnAt() == null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateResetTokenExists(PasswordResetVerification verification) {
        if (verification == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
    }

    private void validateResetTokenStatus(PasswordResetVerification verification) {
        if (VerificationStatus.USED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);
        }

        if (!VerificationStatus.VERIFIED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
    }

    private void validateResetTokenNotExpired(PasswordResetVerification verification) {
        if (verification.getResetTokenExpiresAt() == null
            || !verification.getResetTokenExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }
    }
}