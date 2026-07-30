package com.wallet.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.SignupEmailVerification;
import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.dto.SignupEmailVerificationConfirmRequest;
import com.wallet.auth.dto.SignupEmailVerificationConfirmResponse;
import com.wallet.auth.dto.SignupEmailVerificationRequest;
import com.wallet.auth.dto.SignupEmailVerificationResponse;
import com.wallet.auth.mapper.SignupEmailVerificationMapper;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.mapper.MemberMapper;

/**
 * 회원가입 이메일 인증을 담당하는 서비스.
 * <p>
 * 흐름:
 * 1. 인증 코드 요청
 * 2. 인증 코드 검증
 * 3. 검증 성공 시 signupVerificationToken 발급
 */
@RequiredArgsConstructor
@Service
public class SignupEmailVerificationService {
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final long SIGNUP_TOKEN_EXPIRE_MINUTES = 10;
    private static final long REISSUE_WAIT_SECONDS = 60;
    private static final int MAX_FAILED_ATTEMPT_COUNT = 5;

    private final SignupEmailVerificationMapper signupEmailVerificationMapper;
    private final MemberMapper memberMapper;
    private final EmailSender emailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final TokenHashUtil tokenHashUtil;

    // 회원가입 이메일 인증 코드를 발송
    @Transactional
    public SignupEmailVerificationResponse sendVerificationCode(
        SignupEmailVerificationRequest request
    ) {
        String email = normalizeEmail(request.email());

        validateEmailNotDuplicated(email);

        SignupEmailVerification existingVerification =
            signupEmailVerificationMapper.findByEmailForUpdate(email);

        validateReissueAllowed(existingVerification);

        String verificationCode = verificationCodeGenerator.generateSixDigitCode();
        String verificationCodeHash = tokenHashUtil.sha256(verificationCode);
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusMinutes(VERIFICATION_CODE_EXPIRE_MINUTES);

        if (existingVerification == null) {
            SignupEmailVerification verification = SignupEmailVerification.createPending(
                email,
                verificationCodeHash,
                expiresAt
            );

            signupEmailVerificationMapper.insert(verification);
        } else {
            signupEmailVerificationMapper.updateForReissue(
                email,
                verificationCodeHash,
                expiresAt
            );
        }

        try {
            emailSender.sendSignupVerificationCode(
                email,
                verificationCode,
                VERIFICATION_CODE_EXPIRE_MINUTES
            );
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }

        return new SignupEmailVerificationResponse(
            email,
            VERIFICATION_CODE_EXPIRE_MINUTES * 60
        );
    }


    // 사용자가 입력한 인증 코드를 검증, 성공 시 최종 회원가입 요청에 사용할 signupVerificationToken을 발급
    @Transactional
    public SignupEmailVerificationConfirmResponse verifyCode(
        SignupEmailVerificationConfirmRequest request
    ) {
        String email = normalizeEmail(request.email());

        SignupEmailVerification verification =
            signupEmailVerificationMapper.findByEmailForUpdate(email);

        validateVerificationExists(verification);
        validateVerificationPending(verification);
        validateAttemptLimitNotExceeded(verification);
        validateVerificationCodeNotExpired(verification);

        String requestedCodeHash = tokenHashUtil.sha256(request.verificationCode());

        if (!requestedCodeHash.equals(verification.getVerificationCodeHash())) {
            signupEmailVerificationMapper.increaseFailedAttemptCount(
                verification.getSignupEmailVerificationId()
            );
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_CODE_INVALID);
        }

        String signupVerificationToken = verificationTokenGenerator.generate();
        String signupTokenHash = tokenHashUtil.sha256(signupVerificationToken);
        LocalDateTime signupTokenExpiresAt = LocalDateTime.now()
            .plusMinutes(SIGNUP_TOKEN_EXPIRE_MINUTES);

        int updatedCount = signupEmailVerificationMapper.verify(
            verification.getSignupEmailVerificationId(),
            signupTokenHash,
            signupTokenExpiresAt
        );

        if (updatedCount != 1) {
            throw new BusinessException(ErrorCode.SIGNUP_VERIFICATION_TOKEN_FAILED);
        }

        return new SignupEmailVerificationConfirmResponse(
            signupVerificationToken,
            SIGNUP_TOKEN_EXPIRE_MINUTES * 60
        );
    }

    // 최종 회원가입 요청에서 signupVerificationToken을 검증
    @Transactional
    public Long validateSignupVerificationToken(
        String requestEmail,
        String signupVerificationToken
    ) {
        String normalizedEmail = normalizeEmail(requestEmail);
        String signupTokenHash = tokenHashUtil.sha256(signupVerificationToken);

        SignupEmailVerification verification =
            signupEmailVerificationMapper.findBySignupTokenHashForUpdate(signupTokenHash);

        validateSignupTokenExists(verification);
        validateSignupTokenStatus(verification);
        validateSignupTokenNotExpired(verification);
        validateSignupEmailMatches(normalizedEmail, verification);

        return verification.getSignupEmailVerificationId();
    }

    // 최종 회원가입 성공 후 이메일 인증 정보를 사용 완료 처리
    @Transactional
    public void markAsUsed(Long signupEmailVerificationId) {
        int updatedCount = signupEmailVerificationMapper.markAsUsed(signupEmailVerificationId);

        if (updatedCount != 1) {
            throw new BusinessException(ErrorCode.SIGNUP_VERIFICATION_TOKEN_INVALID);
        }
    }

    private void validateEmailNotDuplicated(String email) {
        if (memberMapper.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateReissueAllowed(SignupEmailVerification verification) {
        if (verification == null || verification.getUpdatedAt() == null) {
            return;
        }

        LocalDateTime reissueAllowedAt = verification.getUpdatedAt()
            .plusSeconds(REISSUE_WAIT_SECONDS);

        if (reissueAllowedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_REQUEST_TOO_FREQUENT);
        }
    }

    private void validateVerificationExists(SignupEmailVerification verification) {
        if (verification == null) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_NOT_FOUND);
        }
    }

    private void validateVerificationPending(SignupEmailVerification verification) {
        if (!VerificationStatus.PENDING.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_CODE_INVALID);
        }
    }

    private void validateAttemptLimitNotExceeded(SignupEmailVerification verification) {
        if (verification.getFailedAttemptCount() >= MAX_FAILED_ATTEMPT_COUNT) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    private void validateVerificationCodeNotExpired(SignupEmailVerification verification) {
        if (!verification.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now())) {
            signupEmailVerificationMapper.expireVerificationCode(
                verification.getSignupEmailVerificationId()
            );
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_CODE_EXPIRED);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateSignupTokenExists(SignupEmailVerification verification) {
        if (verification == null) {
            throw new BusinessException(ErrorCode.SIGNUP_VERIFICATION_TOKEN_INVALID);
        }
    }

    private void validateSignupTokenStatus(SignupEmailVerification verification) {
        if (VerificationStatus.USED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_ALREADY_USED);
        }

        if (!VerificationStatus.VERIFIED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_VERIFICATION_TOKEN_INVALID);
        }
    }

    private void validateSignupTokenNotExpired(SignupEmailVerification verification) {
        if (verification.getSignupTokenExpiresAt() == null
            || !verification.getSignupTokenExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SIGNUP_VERIFICATION_TOKEN_EXPIRED);
        }
    }

    private void validateSignupEmailMatches(
        String requestEmail,
        SignupEmailVerification verification
    ) {
        if (!requestEmail.equals(verification.getEmail())) {
            throw new BusinessException(ErrorCode.SIGNUP_VERIFICATION_EMAIL_MISMATCH);
        }
    }
}